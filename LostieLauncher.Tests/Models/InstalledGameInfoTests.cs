using LostieLauncher.Models;

namespace LostieLauncher.Tests.Models;

public class InstalledGameInfoTests
{
    [Fact]
    public void IsSpecialVersion_WhenTipoIsNull_ReturnsFalse()
    {
        // Arrange
        var info = new InstalledGameInfo { Tipo = null };

        // Act
        var isSpecial = info.IsSpecialVersion;

        // Assert
        isSpecial.ShouldBeFalse();
    }

    [Fact]
    public void IsSpecialVersion_WhenTipoIsEmpty_ReturnsFalse()
    {
        // Arrange
        var info = new InstalledGameInfo { Tipo = string.Empty };

        // Act
        var isSpecial = info.IsSpecialVersion;

        // Assert
        isSpecial.ShouldBeFalse();
    }

    [Fact]
    public void IsSpecialVersion_WhenTipoHasValue_ReturnsTrue()
    {
        // Arrange
        var info = new InstalledGameInfo { Tipo = "beta" };

        // Act
        var isSpecial = info.IsSpecialVersion;

        // Assert
        isSpecial.ShouldBeTrue();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsEmpty_ReturnsNull()
    {
        // Arrange
        var info = new InstalledGameInfo { Logo = string.Empty };

        // Act
        var url = info.LogoUrl;

        // Assert
        url.ShouldBeNull();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsRelativePath_PrependsCdnHost()
    {
        // Arrange
        var info = new InstalledGameInfo { Logo = "/logos/y.png" };

        // Act
        var url = info.LogoUrl;

        // Assert
        url.ShouldBe("https://ericlostie-launcher.jagoba.dev/logos/y.png");
    }

    [Theory]
    [InlineData(0, "")]
    [InlineData(30, "30 min")]
    [InlineData(60, "1 h")]
    [InlineData(150, "2 h 30 min")]
    public void PlaytimeText_FormatsAccordingToHourMinuteRules(int minutes, string expected)
    {
        // Arrange
        var info = new InstalledGameInfo { PlaytimeMinutes = minutes };

        // Act
        var text = info.PlaytimeText;

        // Assert
        text.ShouldBe(expected);
    }

    [Fact]
    public void PlaytimeMinutes_WhenChanged_RaisesPropertyChangedForPlaytimeText()
    {
        // Arrange
        var info = new InstalledGameInfo();
        using var recorder = new PropertyChangedRecorder(info);

        // Act
        info.PlaytimeMinutes = 90;

        // Assert
        recorder.WasRaised(nameof(InstalledGameInfo.PlaytimeText)).ShouldBeTrue();
    }
}
