using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class SettingsServiceTests
{
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
}
