using LostieLauncher.Models;
using LostieLauncher.Services;
using System.Text.Json;

namespace LostieLauncher.Tests.Services;

public class SettingsServiceTests
{
    // A long debounce so the timer never fires mid-test: every flush is driven explicitly
    // via Dispose(), keeping disk-write assertions deterministic.
    private static readonly TimeSpan NoAutoFlush = TimeSpan.FromSeconds(30);

    private static SettingsService CreateService(TempDirectoryFixture temp, TimeSpan? delay = null) =>
        new(temp.Path, temp.Combine("legacy.json"), delay ?? NoAutoFlush);

    private static string SettingsPath(TempDirectoryFixture temp) => temp.Combine("launcher_settings.json");

    private static readonly string MyDocuments = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);

    /// <summary>Counts how many times the settings actually hit disk, to prove debounce coalescing.</summary>
    private sealed class CountingSettingsService(string directory, string legacy, TimeSpan delay)
        : SettingsService(directory, legacy, delay)
    {
        public int WriteCount { get; private set; }

        protected override void WriteToDisk(AppSettings settings)
        {
            WriteCount++;
            base.WriteToDisk(settings);
        }
    }

    // -------------------- SanitizeSettings (BUG-032) --------------------

    [Fact]
    public void SanitizeSettings_OutOfRangeTheme_ResetsToVolcarona()
    {
        // Arrange — a hand-edited "Theme": 99 deserializes to (AppTheme)99 without range-checking.
        var settings = new AppSettings { Theme = (AppTheme)99 };

        // Act
        var sanitized = SettingsService.SanitizeSettings(settings);

        // Assert — the bogus value must not survive to ApplyTheme (Themes/99.xaml does not exist).
        sanitized.Theme.ShouldBe(AppTheme.Volcarona);
    }

    [Fact]
    public void SanitizeSettings_OutOfRangeLanguage_ResetsToEsp()
    {
        // Arrange — same vector for the language enum.
        var settings = new AppSettings { Language = (AppLanguage)99 };

        // Act
        var sanitized = SettingsService.SanitizeSettings(settings);

        // Assert
        sanitized.Language.ShouldBe(AppLanguage.Esp);
    }

    [Fact]
    public void SanitizeSettings_NegativeEnumValues_ResetToDefaults()
    {
        // Arrange — negative underlying values are equally undefined.
        var settings = new AppSettings { Theme = (AppTheme)(-1), Language = (AppLanguage)(-5) };

        // Act
        var sanitized = SettingsService.SanitizeSettings(settings);

        // Assert
        sanitized.Theme.ShouldBe(AppTheme.Volcarona);
        sanitized.Language.ShouldBe(AppLanguage.Esp);
    }

    [Fact]
    public void SanitizeSettings_ValidValues_AreLeftUntouched()
    {
        // Arrange — a perfectly valid configuration must pass through unchanged.
        var settings = new AppSettings
        {
            Theme = AppTheme.Empoleon,
            Language = AppLanguage.Eng,
            StartMinimized = true,
            AutoUpdate = true,
            DownloadDirectory = @"C:\games",
            HasSeenWelcome = true
        };

        // Act
        var sanitized = SettingsService.SanitizeSettings(settings);

        // Assert — enums preserved and no collateral mutation of the other fields.
        sanitized.Theme.ShouldBe(AppTheme.Empoleon);
        sanitized.Language.ShouldBe(AppLanguage.Eng);
        sanitized.StartMinimized.ShouldBeTrue();
        sanitized.AutoUpdate.ShouldBeTrue();
        sanitized.DownloadDirectory.ShouldBe(@"C:\games");
        sanitized.HasSeenWelcome.ShouldBeTrue();
    }

    [Fact]
    public void SanitizeSettings_AllDefinedThemeValues_AreConsideredValid()
    {
        // Arrange & Act & Assert — every declared theme must be treated as defined (guards against
        // a future theme being added but rejected by the validation).
        foreach (var theme in Enum.GetValues<AppTheme>())
        {
            var sanitized = SettingsService.SanitizeSettings(new AppSettings { Theme = theme });
            sanitized.Theme.ShouldBe(theme);
        }
    }

    // -------------------- NormalizeDownloadDirectory (BUG-037) --------------------

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("relative")]
    [InlineData(@"relative\path")]
    [InlineData("relative/path")]
    public void NormalizeDownloadDirectory_EmptyOrRelative_FallsBackToMyDocuments(string? input)
    {
        // Arrange & Act — a hand-edited "" or any non-rooted path would make Path.Combine resolve
        // against the current working directory; it must be rejected.
        var normalized = SettingsService.NormalizeDownloadDirectory(input);

        // Assert
        normalized.ShouldBe(MyDocuments);
    }

    [Fact]
    public void NormalizeDownloadDirectory_FullyQualifiedPath_IsPreserved()
    {
        // Arrange & Act — a valid absolute path survives canonicalization unchanged.
        var normalized = SettingsService.NormalizeDownloadDirectory(@"C:\Games\Lostie");

        // Assert
        normalized.ShouldBe(@"C:\Games\Lostie");
    }

    [Fact]
    public void SanitizeSettings_EmptyDownloadDirectory_IsNormalizedToMyDocuments()
    {
        // Arrange — the corrupt-JSON vector: "DownloadDirectory": "".
        var settings = new AppSettings { DownloadDirectory = "" };

        // Act
        var sanitized = SettingsService.SanitizeSettings(settings);

        // Assert
        sanitized.DownloadDirectory.ShouldBe(MyDocuments);
    }

    // -------------------- In-memory cache (BUG-037) --------------------

    [Fact]
    public void Load_SecondCall_ReturnsCachedInstanceWithoutRereadingDisk()
    {
        // Arrange — a settings file on disk with a distinctive value.
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), JsonSerializer.Serialize(
            new AppSettings { Language = AppLanguage.Eng, DownloadDirectory = temp.Path }));
        using var service = CreateService(temp);

        // Act — load once, then delete the backing file: a cache miss would now yield defaults.
        var first = service.Load();
        File.Delete(SettingsPath(temp));
        var second = service.Load();

        // Assert — the second call served the same cached instance, never touching the (now gone) file.
        first.Language.ShouldBe(AppLanguage.Eng);
        second.ShouldBeSameAs(first);
    }

    [Fact]
    public void Load_EmptyDownloadDirectoryOnDisk_FallsBackToMyDocuments()
    {
        // Arrange — corrupt JSON with an empty download directory.
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), "{\"DownloadDirectory\": \"\"}");
        using var service = CreateService(temp);

        // Act
        var settings = service.Load();

        // Assert
        settings.DownloadDirectory.ShouldBe(MyDocuments);
    }

    [Fact]
    public void GetGamesRootDirectory_WithEmptyDownloadDirectory_ReturnsAbsolutePath()
    {
        // Arrange — the exact BUG-037 vector: "" would make Path.Combine("", "LostieLauncher")
        // produce a path relative to the working directory.
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), "{\"DownloadDirectory\": \"\"}");
        using var service = CreateService(temp);

        // Act
        var root = service.GetGamesRootDirectory();

        // Assert — always rooted, never relative to the CWD.
        Path.IsPathFullyQualified(root).ShouldBeTrue();
        root.ShouldBe(Path.Combine(MyDocuments, "LostieLauncher"));
    }

    // -------------------- Debounced save (BUG-039) --------------------

    [Fact]
    public void Save_UpdatesCacheImmediately_BeforeTheDiskFlush()
    {
        // Arrange — no file on disk yet; the debounce is long so nothing flushes during the test.
        using var temp = new TempDirectoryFixture("settings");
        using var service = CreateService(temp);

        // Act — save, then read back through the cache without waiting for the timer.
        service.Save(new AppSettings { Language = AppLanguage.Fra, DownloadDirectory = temp.Path });

        // Assert — the cache reflects the save at once (so GetGamesRootDirectory/EnsureGamesRoot stay coherent),
        // while disk I/O is still pending.
        service.Load().Language.ShouldBe(AppLanguage.Fra);
        File.Exists(SettingsPath(temp)).ShouldBeFalse();
    }

    [Fact]
    public void Dispose_FlushesPendingSaveToDisk()
    {
        // Arrange
        using var temp = new TempDirectoryFixture("settings");

        // Act — a debounced save that has not yet flushed must be persisted on dispose (app exit).
        using (var service = CreateService(temp))
        {
            service.Save(new AppSettings { Theme = AppTheme.Empoleon, DownloadDirectory = temp.Path });
            File.Exists(SettingsPath(temp)).ShouldBeFalse();
        }

        // Assert
        File.Exists(SettingsPath(temp)).ShouldBeTrue();
        var reloaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(SettingsPath(temp)))!;
        reloaded.Theme.ShouldBe(AppTheme.Empoleon);
    }

    [Fact]
    public void Save_CalledRapidly_CoalescesIntoASingleDiskWrite()
    {
        // Arrange
        using var temp = new TempDirectoryFixture("settings");
        var service = new CountingSettingsService(temp.Path, temp.Combine("legacy.json"), NoAutoFlush);

        // Act — three rapid changes, then a single flush via dispose.
        service.Save(new AppSettings { Language = AppLanguage.Eng, DownloadDirectory = temp.Path });
        service.Save(new AppSettings { Language = AppLanguage.Cat, DownloadDirectory = temp.Path });
        service.Save(new AppSettings { Language = AppLanguage.Fra, DownloadDirectory = temp.Path });
        service.WriteCount.ShouldBe(0); // debounced: nothing on disk yet
        service.Dispose();

        // Assert — five hypothetical writes collapsed into one, and last-write-wins.
        service.WriteCount.ShouldBe(1);
        var reloaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(SettingsPath(temp)))!;
        reloaded.Language.ShouldBe(AppLanguage.Fra);
    }
}
