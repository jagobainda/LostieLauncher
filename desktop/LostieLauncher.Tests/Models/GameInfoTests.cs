using LostieLauncher.Models;
using System.Globalization;

namespace LostieLauncher.Tests.Models;

public class GameInfoTests
{
    [Fact]
    public void PesoFormateado_WhenSizeIsAtLeastOneGB_ReturnsValueInGigabytes()
    {
        var game = TestData.Game(pesoGB: 1.5);

        var formatted = game.PesoFormateado;

        formatted.ShouldBe("1.5 GB");
    }

    [Fact]
    public void PesoFormateado_WhenSizeBelowOneGB_ReturnsValueInMegabytes()
    {
        var game = TestData.Game(pesoGB: 0.25);

        var formatted = game.PesoFormateado;

        formatted.ShouldBe("256 MB");
    }

    [Fact]
    public void PesoFormateado_UsesInvariantCulture_NotThousandsOrCommaSeparator()
    {
        var game = TestData.Game(pesoGB: 1.0);

        var formatted = game.PesoFormateado;

        formatted.ShouldBe("1 GB");
    }

    [Fact]
    public void GameId_WhenNameContainsUppercaseAndSpaces_ReturnsLowerKebabSlug()
    {
        var game = TestData.Game(name: "Eric Lostie 2");

        var id = game.GameId;

        id.ShouldBe("eric-lostie-2");
    }

    [Fact]
    public void GameId_WhenNameContainsAccentsAndSymbols_StripsToAlphaNumWithDashes()
    {
        var game = TestData.Game(name: "¡Hola! Mundo+");

        var id = game.GameId;

        id.ShouldBe("hola-mundo");
    }

    [Theory]
    [InlineData("+++", "")]
    [InlineData("-Trailing-", "trailing")]
    [InlineData("Inner  Spaces", "inner-spaces")]
    public void GameId_TrimsDanglingDashesAndCollapsesSeparators(string name, string expected)
    {
        var game = TestData.Game(name: name);

        var id = game.GameId;

        id.ShouldBe(expected);
    }

    [Fact]
    public void GameId_WhenNombreIsNull_ReturnsEmptyStringWithoutThrowing()
    {
        var game = new GameInfo { Nombre = null! };

        var id = game.GameId;

        id.ShouldBe(string.Empty);
    }

    [Fact]
    public void LogoUrl_WhenLogoIsEmpty_ReturnsNull()
    {
        var game = new GameInfo { Logo = string.Empty };

        var url = game.LogoUrl;

        url.ShouldBeNull();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsRelativePath_PrependsCdnHost()
    {
        var game = new GameInfo { Logo = "/logos/x.png" };

        var url = game.LogoUrl;

        url.ShouldBe("https://ericlostie-launcher.jagoba.dev/logos/x.png");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsZero_ReturnsZeroKbPerSecond()
    {
        var game = new GameInfo { DownloadSpeedBytesPerSec = 0 };

        var text = game.DownloadSpeedText;

        text.ShouldBe("0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsAboveOneKb_ReturnsKbPerSecond()
    {
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 2048 };

        var text = game.DownloadSpeedText;

        text.ShouldBe($"2{sep}0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsAboveOneMb_ReturnsMbPerSecond()
    {
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 5 * 1_048_576 };

        var text = game.DownloadSpeedText;

        text.ShouldBe($"5{sep}0 MB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsExactlyOneMb_ReturnsMbPerSecond()
    {
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 1_048_576 };

        var text = game.DownloadSpeedText;

        text.ShouldBe($"1{sep}0 MB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsExactlyOneKb_ReturnsKbPerSecond()
    {
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 1024 };

        var text = game.DownloadSpeedText;

        text.ShouldBe($"1{sep}0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsBelowOneKb_ReturnsFractionalKbPerSecond()
    {
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 512 };

        var text = game.DownloadSpeedText;

        text.ShouldBe($"0{sep}5 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsNegative_ReturnsZeroKbPerSecond()
    {
        var game = new GameInfo { DownloadSpeedBytesPerSec = -1 };

        var text = game.DownloadSpeedText;

        text.ShouldBe("0 KB/s");
    }

    [Fact]
    public void DownloadSpeedBytesPerSec_WhenChanged_RaisesNotificationForDownloadSpeedText()
    {
        var game = new GameInfo();
        using var recorder = new PropertyChangedRecorder(game);

        game.DownloadSpeedBytesPerSec = 4096;

        recorder.WasRaised(nameof(GameInfo.DownloadSpeedText)).ShouldBeTrue();
    }

    [Fact]
    public void PlaytimeMinutes_WhenChanged_RaisesNotificationForPlaytimeText()
    {
        var game = new GameInfo();
        using var recorder = new PropertyChangedRecorder(game);

        game.PlaytimeMinutes = 75;

        recorder.WasRaised(nameof(GameInfo.PlaytimeText)).ShouldBeTrue();
    }

    [Theory]
    [InlineData(0, "")]
    [InlineData(-5, "")]
    [InlineData(45, "45 min")]
    [InlineData(60, "1 h")]
    [InlineData(125, "2 h 5 min")]
    public void PlaytimeText_FormatsMinutesUsingHourMinuteRules(int minutes, string expected)
    {
        var game = new GameInfo { PlaytimeMinutes = minutes };

        var text = game.PlaytimeText;

        text.ShouldBe(expected);
    }
}
