using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.Utils;
using System.Text.Json;

namespace LostieLauncher.Tests.Services;

public class SettingsServiceTests
{
    private static readonly TimeSpan NoAutoFlush = TimeSpan.FromSeconds(30);

    private static SettingsService CreateService(TempDirectoryFixture temp, TimeSpan? delay = null) =>
        new(temp.Path, temp.Combine("legacy.json"), delay ?? NoAutoFlush);

    private static string SettingsPath(TempDirectoryFixture temp) => temp.Combine("launcher_settings.json");

    private static readonly string UserProfile = DownloadDefaults.DownloadDirectory;

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

    [Fact]
    public void SanitizeSettings_OutOfRangeTheme_ResetsToVolcarona()
    {
        var settings = new AppSettings { Theme = (AppTheme)99 };

        var sanitized = SettingsService.SanitizeSettings(settings);

        sanitized.Theme.ShouldBe(AppTheme.Volcarona);
    }

    [Fact]
    public void SanitizeSettings_OutOfRangeLanguage_ResetsToEsp()
    {
        var settings = new AppSettings { Language = (AppLanguage)99 };

        var sanitized = SettingsService.SanitizeSettings(settings);

        sanitized.Language.ShouldBe(AppLanguage.Esp);
    }

    [Fact]
    public void SanitizeSettings_NegativeEnumValues_ResetToDefaults()
    {
        var settings = new AppSettings { Theme = (AppTheme)(-1), Language = (AppLanguage)(-5) };

        var sanitized = SettingsService.SanitizeSettings(settings);

        sanitized.Theme.ShouldBe(AppTheme.Volcarona);
        sanitized.Language.ShouldBe(AppLanguage.Esp);
    }

    [Fact]
    public void SanitizeSettings_ValidValues_AreLeftUntouched()
    {
        var settings = new AppSettings
        {
            Theme = AppTheme.Empoleon,
            Language = AppLanguage.Eng,
            StartMinimized = true,
            AutoUpdate = true,
            DownloadDirectory = @"C:\games",
            HasSeenWelcome = true
        };

        var sanitized = SettingsService.SanitizeSettings(settings);

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
        foreach (var theme in Enum.GetValues<AppTheme>())
        {
            var sanitized = SettingsService.SanitizeSettings(new AppSettings { Theme = theme });
            sanitized.Theme.ShouldBe(theme);
        }
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("relative")]
    [InlineData(@"relative\path")]
    [InlineData("relative/path")]
    public void NormalizeDownloadDirectory_EmptyOrRelative_FallsBackToTheUserProfile(string? input)
    {
        var normalized = SettingsService.NormalizeDownloadDirectory(input);

        normalized.ShouldBe(UserProfile);
    }

    [Fact]
    public void NormalizeDownloadDirectory_FullyQualifiedPath_IsPreserved()
    {
        var normalized = SettingsService.NormalizeDownloadDirectory(@"C:\Games\Lostie");

        normalized.ShouldBe(@"C:\Games\Lostie");
    }

    [Fact]
    public void SanitizeSettings_EmptyDownloadDirectory_IsNormalizedToTheUserProfile()
    {
        var settings = new AppSettings { DownloadDirectory = "" };

        var sanitized = SettingsService.SanitizeSettings(settings);

        sanitized.DownloadDirectory.ShouldBe(UserProfile);
    }

    [Fact]
    public void Load_SecondCall_ReturnsCachedInstanceWithoutRereadingDisk()
    {
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), JsonSerializer.Serialize(
            new AppSettings { Language = AppLanguage.Eng, DownloadDirectory = temp.Path }));
        using var service = CreateService(temp);

        var first = service.Load();
        File.Delete(SettingsPath(temp));
        var second = service.Load();

        first.Language.ShouldBe(AppLanguage.Eng);
        second.ShouldBeSameAs(first);
    }

    [Fact]
    public void Load_EmptyDownloadDirectoryOnDisk_FallsBackToTheUserProfile()
    {
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), "{\"DownloadDirectory\": \"\"}");
        using var service = CreateService(temp);

        var settings = service.Load();

        settings.DownloadDirectory.ShouldBe(UserProfile);
    }

    [Fact]
    public void GetGamesRootDirectory_WithEmptyDownloadDirectory_ReturnsAbsolutePath()
    {
        using var temp = new TempDirectoryFixture("settings");
        File.WriteAllText(SettingsPath(temp), "{\"DownloadDirectory\": \"\"}");
        using var service = CreateService(temp);

        var root = service.GetGamesRootDirectory();

        Path.IsPathFullyQualified(root).ShouldBeTrue();
        root.ShouldBe(Path.Combine(UserProfile, "LostieLauncher"));
    }

    [Fact]
    public void Load_WithNoSettingsFile_DefaultsToADirectoryThatIsNotOneDriveSynced()
    {
        using var temp = new TempDirectoryFixture("settings");
        using var service = CreateService(temp);

        var root = service.GetGamesRootDirectory();

        root.ShouldStartWith(UserProfile);
        OneDrivePathPolicy.IsSynced(root, OneDrivePathPolicy.GetEnvironmentSyncRoots()).ShouldBeFalse();
    }

    [Fact]
    public void Load_WithNoSettingsFile_DefaultsOutsideTheLauncherOwnInstallDirectory()
    {
        using var temp = new TempDirectoryFixture("settings");
        using var service = CreateService(temp);
        var installDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "LostieLauncher");

        var root = service.GetGamesRootDirectory();

        root.ShouldNotBe(installDirectory);
        root.StartsWith(installDirectory, StringComparison.OrdinalIgnoreCase).ShouldBeFalse();
    }

    [Fact]
    public void Load_WithAStoredDownloadDirectory_KeepsIt()
    {
        using var temp = new TempDirectoryFixture("settings");
        var stored = temp.Combine("OneDrive", "Documentos");
        File.WriteAllText(SettingsPath(temp), JsonSerializer.Serialize(new AppSettings { DownloadDirectory = stored }));
        using var service = CreateService(temp);

        var settings = service.Load();

        settings.DownloadDirectory.ShouldBe(stored);
    }

    [Fact]
    public void Save_UpdatesCacheImmediately_BeforeTheDiskFlush()
    {
        using var temp = new TempDirectoryFixture("settings");
        using var service = CreateService(temp);

        service.Save(new AppSettings { Language = AppLanguage.Fra, DownloadDirectory = temp.Path });

        service.Load().Language.ShouldBe(AppLanguage.Fra);
        File.Exists(SettingsPath(temp)).ShouldBeFalse();
    }

    [Fact]
    public void Dispose_FlushesPendingSaveToDisk()
    {
        using var temp = new TempDirectoryFixture("settings");

        using (var service = CreateService(temp))
        {
            service.Save(new AppSettings { Theme = AppTheme.Empoleon, DownloadDirectory = temp.Path });
            File.Exists(SettingsPath(temp)).ShouldBeFalse();
        }

        File.Exists(SettingsPath(temp)).ShouldBeTrue();
        var reloaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(SettingsPath(temp)))!;
        reloaded.Theme.ShouldBe(AppTheme.Empoleon);
    }

    [Fact]
    public void Save_CalledRapidly_CoalescesIntoASingleDiskWrite()
    {
        using var temp = new TempDirectoryFixture("settings");
        var service = new CountingSettingsService(temp.Path, temp.Combine("legacy.json"), NoAutoFlush);

        service.Save(new AppSettings { Language = AppLanguage.Eng, DownloadDirectory = temp.Path });
        service.Save(new AppSettings { Language = AppLanguage.Cat, DownloadDirectory = temp.Path });
        service.Save(new AppSettings { Language = AppLanguage.Fra, DownloadDirectory = temp.Path });
        service.WriteCount.ShouldBe(0);
        service.Dispose();

        service.WriteCount.ShouldBe(1);
        var reloaded = JsonSerializer.Deserialize<AppSettings>(File.ReadAllText(SettingsPath(temp)))!;
        reloaded.Language.ShouldBe(AppLanguage.Fra);
    }
}
