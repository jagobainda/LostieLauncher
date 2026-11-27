using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadPathUtilsTests
{
    [Fact]
    public void ComputeToken_IsStableForTheSameVersionAndKey()
    {
        var first = DownloadPathUtils.ComputeToken("1.0.0", "ABCD-EFGH-IJKL-MNOP-QRST");
        var second = DownloadPathUtils.ComputeToken("1.0.0", "ABCD-EFGH-IJKL-MNOP-QRST");

        first.ShouldBe(second);
    }

    [Fact]
    public void ComputeToken_DiffersBetweenSpecialVersionAndStandard_SameVersion()
    {
        var standard = DownloadPathUtils.ComputeToken("1.0.0", key: null);
        var special = DownloadPathUtils.ComputeToken("1.0.0", key: "ABCD-EFGH-IJKL-MNOP-QRST");

        standard.ShouldNotBe(special);
    }

    [Fact]
    public void ComputeToken_DiffersBetweenVersions()
    {
        var v1 = DownloadPathUtils.ComputeToken("1.0.0", key: null);
        var v2 = DownloadPathUtils.ComputeToken("2.0.0", key: null);

        v1.ShouldNotBe(v2);
    }

    [Fact]
    public void ComputeToken_ProducesFileSystemSafeLowercaseHex()
    {
        var token = DownloadPathUtils.ComputeToken("1.0.0-beta+meta", key: null);

        token.Length.ShouldBe(16);
        token.ShouldAllBe(c => Uri.IsHexDigit(c) && !char.IsUpper(c));
    }

    [Fact]
    public void GetZipFileName_EmbedsGameIdAndDiscriminatingToken()
    {
        var args = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip");

        var name = DownloadPathUtils.GetZipFileName(args);

        name.ShouldStartWith("cool-game.");
        name.ShouldEndWith(".zip");
    }

    [Fact]
    public void GetZipFileName_SpecialVersionAndStandardOfSameGame_DoNotCollide()
    {
        var standard = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip");
        var special = new GameDownloadArgs("cool-game", "1.0.0", "/games/cool.zip", "ABCD-EFGH-IJKL-MNOP-QRST");

        var standardName = DownloadPathUtils.GetZipFileName(standard);
        var specialName = DownloadPathUtils.GetZipFileName(special);

        standardName.ShouldNotBe(specialName);
    }

    [Fact]
    public void GetPartFilePath_AppendsPartExtension()
    {
        var partPath = DownloadPathUtils.GetPartFilePath(@"C:\games\.downloads\cool-game.abcd1234.zip");

        partPath.ShouldBe(@"C:\games\.downloads\cool-game.abcd1234.zip.part");
    }

    [Fact]
    public void GetMetaFilePath_AppendsMetaExtensionToPartPath()
    {
        var metaPath = DownloadPathUtils.GetMetaFilePath(@"C:\games\.downloads\cool-game.abcd1234.zip.part");

        metaPath.ShouldBe(@"C:\games\.downloads\cool-game.abcd1234.zip.part.meta");
    }

    [Fact]
    public void PartAndMetaDerivation_MatchesAcrossWriterAndCleaner()
    {
        var finalPath = @"C:\games\.downloads\cool-game.abcd1234.zip";

        var partPath = DownloadPathUtils.GetPartFilePath(finalPath);
        var metaPath = DownloadPathUtils.GetMetaFilePath(partPath);

        partPath.ShouldBe(finalPath + ".part");
        metaPath.ShouldBe(finalPath + ".part.meta");
    }
}
