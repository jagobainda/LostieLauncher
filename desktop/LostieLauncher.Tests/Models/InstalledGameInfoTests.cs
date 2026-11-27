using LostieLauncher.Models;

namespace LostieLauncher.Tests.Models;

public class InstalledGameInfoTests
{
    [Fact]
    public void IsSpecialVersion_WhenTipoIsNull_ReturnsFalse()
    {
        var info = new InstalledGameInfo { Tipo = null };

        var isSpecial = info.IsSpecialVersion;

        isSpecial.ShouldBeFalse();
    }

    [Fact]
    public void IsSpecialVersion_WhenTipoIsEmpty_ReturnsFalse()
    {
        var info = new InstalledGameInfo { Tipo = string.Empty };

        var isSpecial = info.IsSpecialVersion;

        isSpecial.ShouldBeFalse();
    }

    [Fact]
    public void IsSpecialVersion_WhenTipoHasValue_ReturnsTrue()
    {
        var info = new InstalledGameInfo { Tipo = "beta" };

        var isSpecial = info.IsSpecialVersion;

        isSpecial.ShouldBeTrue();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsEmpty_ReturnsNull()
    {
        var info = new InstalledGameInfo { Logo = string.Empty };

        var url = info.LogoUrl;

        url.ShouldBeNull();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsRelativePath_PrependsCdnHost()
    {
        var info = new InstalledGameInfo { Logo = "/logos/y.png" };

        var url = info.LogoUrl;

        url.ShouldBe("https://ericlostie-launcher.jagoba.dev/logos/y.png");
    }

    [Theory]
    [InlineData(0, "")]
    [InlineData(30, "30 min")]
    [InlineData(60, "1 h")]
    [InlineData(150, "2 h 30 min")]
    public void PlaytimeText_FormatsAccordingToHourMinuteRules(int minutes, string expected)
    {
        var info = new InstalledGameInfo { PlaytimeMinutes = minutes };

        var text = info.PlaytimeText;

        text.ShouldBe(expected);
    }

    [Fact]
    public void PlaytimeMinutes_WhenChanged_RaisesPropertyChangedForPlaytimeText()
    {
        var info = new InstalledGameInfo();
        using var recorder = new PropertyChangedRecorder(info);

        info.PlaytimeMinutes = 90;

        recorder.WasRaised(nameof(InstalledGameInfo.PlaytimeText)).ShouldBeTrue();
    }
}
