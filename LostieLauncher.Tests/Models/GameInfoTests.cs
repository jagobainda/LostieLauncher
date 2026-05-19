using LostieLauncher.Models;
using System.Globalization;

namespace LostieLauncher.Tests.Models;

public class GameInfoTests
{
    [Fact]
    public void PesoFormateado_WhenSizeIsAtLeastOneGB_ReturnsValueInGigabytes()
    {
        // Arrange
        var game = TestData.Game(pesoGB: 1.5);

        // Act
        var formatted = game.PesoFormateado;

        // Assert
        formatted.ShouldBe("1.5 GB");
    }

    [Fact]
    public void PesoFormateado_WhenSizeBelowOneGB_ReturnsValueInMegabytes()
    {
        // Arrange
        var game = TestData.Game(pesoGB: 0.25);

        // Act
        var formatted = game.PesoFormateado;

        // Assert
        formatted.ShouldBe("256 MB");
    }

    [Fact]
    public void PesoFormateado_UsesInvariantCulture_NotThousandsOrCommaSeparator()
    {
        // Arrange — culture-sensitive scenario: 1.0 GB must always render with a dot.
        var game = TestData.Game(pesoGB: 1.0);

        // Act
        var formatted = game.PesoFormateado;

        // Assert
        formatted.ShouldBe("1 GB");
    }

    [Fact]
    public void GameId_WhenNameContainsUppercaseAndSpaces_ReturnsLowerKebabSlug()
    {
        // Arrange
        var game = TestData.Game(name: "Eric Lostie 2");

        // Act
        var id = game.GameId;

        // Assert
        id.ShouldBe("eric-lostie-2");
    }

    [Fact]
    public void GameId_WhenNameContainsAccentsAndSymbols_StripsToAlphaNumWithDashes()
    {
        // Arrange
        var game = TestData.Game(name: "¡Hola! Mundo+");

        // Act
        var id = game.GameId;

        // Assert
        id.ShouldBe("-hola-mundo-");
    }

    [Fact]
    public void LogoUrl_WhenLogoIsEmpty_ReturnsNull()
    {
        // Arrange
        var game = new GameInfo { Logo = string.Empty };

        // Act
        var url = game.LogoUrl;

        // Assert
        url.ShouldBeNull();
    }

    [Fact]
    public void LogoUrl_WhenLogoIsRelativePath_PrependsCdnHost()
    {
        // Arrange
        var game = new GameInfo { Logo = "/logos/x.png" };

        // Act
        var url = game.LogoUrl;

        // Assert
        url.ShouldBe("https://ericlostie-launcher.jagoba.dev/logos/x.png");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsZero_ReturnsZeroKbPerSecond()
    {
        // Arrange
        var game = new GameInfo { DownloadSpeedBytesPerSec = 0 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe("0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsAboveOneKb_ReturnsKbPerSecond()
    {
        // Arrange — production code uses current culture for the decimal separator,
        // so we compute the expected separator dynamically to keep the test deterministic
        // regardless of the host locale.
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 2048 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe($"2{sep}0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsAboveOneMb_ReturnsMbPerSecond()
    {
        // Arrange
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 5 * 1_048_576 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe($"5{sep}0 MB/s");
    }

    [Fact]
    public void DownloadSpeedBytesPerSec_WhenChanged_RaisesNotificationForDownloadSpeedText()
    {
        // Arrange
        var game = new GameInfo();
        using var recorder = new PropertyChangedRecorder(game);

        // Act
        game.DownloadSpeedBytesPerSec = 4096;

        // Assert
        recorder.WasRaised(nameof(GameInfo.DownloadSpeedText)).ShouldBeTrue();
    }

    [Fact]
    public void PlaytimeMinutes_WhenChanged_RaisesNotificationForPlaytimeText()
    {
        // Arrange
        var game = new GameInfo();
        using var recorder = new PropertyChangedRecorder(game);

        // Act
        game.PlaytimeMinutes = 75;

        // Assert
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
        // Arrange
        var game = new GameInfo { PlaytimeMinutes = minutes };

        // Act
        var text = game.PlaytimeText;

        // Assert
        text.ShouldBe(expected);
    }
}
