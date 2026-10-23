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
        // Arrange — leading '¡' and trailing '+' become separator runs that must NOT
        // survive as dangling dashes (BUG-048): the slug has to be trimmed at both ends.
        var game = TestData.Game(name: "¡Hola! Mundo+");

        // Act
        var id = game.GameId;

        // Assert
        id.ShouldBe("hola-mundo");
    }

    [Theory]
    [InlineData("+++", "")]
    [InlineData("-Trailing-", "trailing")]
    [InlineData("Inner  Spaces", "inner-spaces")]
    public void GameId_TrimsDanglingDashesAndCollapsesSeparators(string name, string expected)
    {
        // Arrange
        var game = TestData.Game(name: name);

        // Act
        var id = game.GameId;

        // Assert
        id.ShouldBe(expected);
    }

    [Fact]
    public void GameId_WhenNombreIsNull_ReturnsEmptyStringWithoutThrowing()
    {
        // Arrange — System.Text.Json can assign null to the non-nullable Nombre (BUG-053),
        // and GameId is consumed in bindings/VMs outside any try/catch. The computed slug must
        // degrade to an empty string instead of NRE'ing on Nombre.ToLowerInvariant().
        var game = new GameInfo { Nombre = null! };

        // Act
        var id = game.GameId;

        // Assert
        id.ShouldBe(string.Empty);
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
    public void DownloadSpeedText_WhenSpeedIsExactlyOneMb_ReturnsMbPerSecond()
    {
        // Arrange — boundary regression (BUG-048): exactly 1 MB/s must render as MB/s,
        // not "1024.0 KB/s" (the old exclusive '> 1_048_576' fell through to the KB branch).
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 1_048_576 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe($"1{sep}0 MB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsExactlyOneKb_ReturnsKbPerSecond()
    {
        // Arrange — boundary regression (BUG-048): exactly 1024 B/s must render as "1.0 KB/s",
        // not "0 KB/s" (the old exclusive '> 1024' fell through to the default branch).
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 1024 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe($"1{sep}0 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsBelowOneKb_ReturnsFractionalKbPerSecond()
    {
        // Arrange — sub-kilobyte speeds (BUG-048): 512 B/s must render as "0.5 KB/s",
        // not the misleading flat "0 KB/s".
        var sep = CultureInfo.CurrentCulture.NumberFormat.NumberDecimalSeparator;
        var game = new GameInfo { DownloadSpeedBytesPerSec = 512 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe($"0{sep}5 KB/s");
    }

    [Fact]
    public void DownloadSpeedText_WhenSpeedIsNegative_ReturnsZeroKbPerSecond()
    {
        // Arrange — defensive: a spurious negative speed must not produce a negative string.
        var game = new GameInfo { DownloadSpeedBytesPerSec = -1 };

        // Act
        var text = game.DownloadSpeedText;

        // Assert
        text.ShouldBe("0 KB/s");
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
