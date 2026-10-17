using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadArtifactsTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("download-artifacts");

    public void Dispose() => _temp.Dispose();

    [Fact]
    public void Delete_RemovesTheArchiveThePartialTransferAndTheResumeMetadata()
    {
        // Arrange — the full set a download can leave behind after a terminal failure.
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        var meta = part + ".meta";
        File.WriteAllText(zip, "archive");
        File.WriteAllText(part, "partial");
        File.WriteAllText(meta, "{}");

        // Act
        var removed = DownloadArtifacts.Delete(zip);

        // Assert
        removed.ShouldBe(3);
        File.Exists(zip).ShouldBeFalse();
        File.Exists(part).ShouldBeFalse();
        File.Exists(meta).ShouldBeFalse();
    }

    [Fact]
    public void Delete_WhenOnlyThePartialTransferExists_RemovesItAndReportsWhatItRemoved()
    {
        // Arrange — the realistic case: the transfer completed but finalizing it never did.
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        File.WriteAllText(part, "partial");

        // Act
        var removed = DownloadArtifacts.Delete(zip);

        // Assert
        removed.ShouldBe(1);
        File.Exists(part).ShouldBeFalse();
    }

    [Fact]
    public void Delete_WhenNothingIsThere_IsANoOp()
    {
        // Arrange & Act — cleanup runs on paths that may already be clean.
        var removed = DownloadArtifacts.Delete(_temp.Combine("missing.zip"));

        // Assert
        removed.ShouldBe(0);
    }

    [Fact]
    public void Delete_WhenAFileIsHeldOpen_KeepsGoingWithTheRest()
    {
        // Arrange — cleanup is best-effort: one locked file must not strand the others on disk.
        var zip = _temp.Combine("demo-game.abc123.zip");
        var part = zip + ".part";
        File.WriteAllText(zip, "archive");
        File.WriteAllText(part, "partial");
        using var holder = new FileStream(part, FileMode.Open, FileAccess.Read, FileShare.None);

        // Act
        var removed = DownloadArtifacts.Delete(zip);

        // Assert
        removed.ShouldBe(1);
        File.Exists(zip).ShouldBeFalse();
        File.Exists(part).ShouldBeTrue();
    }
}
