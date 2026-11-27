using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadArtifactsTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("download-artifacts");

    public void Dispose() => _temp.Dispose();

    [Fact]
    public void Delete_RemovesTheArchiveThePartialTransferAndTheResumeMetadata()
    {
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        var meta = part + ".meta";
        File.WriteAllText(zip, "archive");
        File.WriteAllText(part, "partial");
        File.WriteAllText(meta, "{}");

        var removed = DownloadArtifacts.Delete(zip);

        removed.ShouldBe(3);
        File.Exists(zip).ShouldBeFalse();
        File.Exists(part).ShouldBeFalse();
        File.Exists(meta).ShouldBeFalse();
    }

    [Fact]
    public void Delete_WhenOnlyThePartialTransferExists_RemovesItAndReportsWhatItRemoved()
    {
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        File.WriteAllText(part, "partial");

        var removed = DownloadArtifacts.Delete(zip);

        removed.ShouldBe(1);
        File.Exists(part).ShouldBeFalse();
    }

    [Fact]
    public void Delete_WhenNothingIsThere_IsANoOp()
    {
        var removed = DownloadArtifacts.Delete(_temp.Combine("missing.zip"));

        removed.ShouldBe(0);
    }

    [Fact]
    public void Delete_WhenAFileIsHeldOpen_KeepsGoingWithTheRest()
    {
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        File.WriteAllText(zip, "archive");
        File.WriteAllText(part, "partial");
        using var holder = new FileStream(part, FileMode.Open, FileAccess.Read, FileShare.None);

        var removed = DownloadArtifacts.Delete(zip);

        removed.ShouldBe(1);
        File.Exists(zip).ShouldBeFalse();
        File.Exists(part).ShouldBeTrue();
    }
}
