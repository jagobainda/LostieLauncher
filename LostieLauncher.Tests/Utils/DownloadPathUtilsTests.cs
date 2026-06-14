using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadPathUtilsTests
{
    // -------------------- ComputeToken --------------------

    [Fact]
    public void ComputeToken_IsStableForTheSameVersionAndKey()
    {
        // Arrange & Act
        var first = DownloadPathUtils.ComputeToken("1.0.0", "ABCD-EFGH-IJKL-MNOP-QRST");
        var second = DownloadPathUtils.ComputeToken("1.0.0", "ABCD-EFGH-IJKL-MNOP-QRST");

        // Assert — a resumed download must derive the exact same .part as the paused one.
        first.ShouldBe(second);
    }

    [Fact]
    public void ComputeToken_DiffersBetweenSpecialVersionAndStandard_SameVersion()
    {
        // Arrange — this is the BUG-003 variant: a special version (with key) and the standard one
        // share GameId and version, but must NOT share the .part file.
        var standard = DownloadPathUtils.ComputeToken("1.0.0", key: null);
        var special = DownloadPathUtils.ComputeToken("1.0.0", key: "ABCD-EFGH-IJKL-MNOP-QRST");

        // Assert
        standard.ShouldNotBe(special);
    }

    [Fact]
    public void ComputeToken_DiffersBetweenVersions()
    {
        // Arrange & Act
        var v1 = DownloadPathUtils.ComputeToken("1.0.0", key: null);
        var v2 = DownloadPathUtils.ComputeToken("2.0.0", key: null);

        // Assert
        v1.ShouldNotBe(v2);
    }

    [Fact]
    public void ComputeToken_ProducesFileSystemSafeLowercaseHex()
    {
        // Arrange & Act
        var token = DownloadPathUtils.ComputeToken("1.0.0-beta+meta", key: null);

        // Assert — 8 bytes -> 16 hex chars, no path-hostile characters.
        token.Length.ShouldBe(16);
        token.ShouldAllBe(c => Uri.IsHexDigit(c) && !char.IsUpper(c));
    }

    // -------------------- GetZipFileName --------------------

    [Fact]
    public void GetZipFileName_EmbedsGameIdAndDiscriminatingToken()
    {
        // Arrange
        var args = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip");

        // Act
        var name = DownloadPathUtils.GetZipFileName(args);

        // Assert
        name.ShouldStartWith("cool-game.");
        name.ShouldEndWith(".zip");
    }

    [Fact]
    public void GetZipFileName_SpecialVersionAndStandardOfSameGame_DoNotCollide()
    {
        // Arrange — same game and version, one standard and one keyed special version.
        var standard = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip");
        var special = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip", "ABCD-EFGH-IJKL-MNOP-QRST");

        // Act
        var standardName = DownloadPathUtils.GetZipFileName(standard);
        var specialName = DownloadPathUtils.GetZipFileName(special);

        // Assert — different file names => different .part/.part.meta => no byte mixing.
        standardName.ShouldNotBe(specialName);
    }
}
