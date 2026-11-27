using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadDirectoryProbeTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("probe");

    public void Dispose() => _temp.Dispose();

    private static void NoopCreate(string _) { }
    private static void NoopWrite(string _) { }
    private static void NoopRename(string _, string __) { }
    private static void NoopDelete(string _) { }

    [Fact]
    public void Run_OnAWritableDirectory_ReportsUsable()
    {
        var result = DownloadDirectoryProbe.Run(_temp.Path);

        result.IsUsable.ShouldBeTrue();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.Usable);
        result.Directory.ShouldBe(_temp.Path);
        result.Error.ShouldBeNull();
    }

    [Fact]
    public void Run_OnAMissingDirectory_CreatesItAndReportsUsable()
    {
        var target = _temp.Combine("nested", "downloads");

        var result = DownloadDirectoryProbe.Run(target);

        result.IsUsable.ShouldBeTrue();
        Directory.Exists(target).ShouldBeTrue();
    }

    [Fact]
    public void Run_WhenSuccessful_LeavesNoFileBehind()
    {
        DownloadDirectoryProbe.Run(_temp.Path);

        Directory.EnumerateFileSystemEntries(_temp.Path).ShouldBeEmpty();
    }

    [Fact]
    public void Run_WhenTheDirectoryPathIsAFile_ReportsCannotCreate()
    {
        var path = _temp.Combine("not-a-directory");
        File.WriteAllText(path, "x");

        var result = DownloadDirectoryProbe.Run(path);

        result.IsUsable.ShouldBeFalse();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotCreate);
        result.Error.ShouldNotBeNull();
    }

    [Fact]
    public void Run_WhenWritingThrows_ReportsCannotWrite()
    {
        static void FailingWrite(string _) => throw new UnauthorizedAccessException("no write");

        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, FailingWrite, NoopRename, NoopDelete);

        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotWrite);
        result.Error.ShouldNotBeNull();
        result.Error.ShouldContain("UnauthorizedAccessException");
    }

    [Fact]
    public void Run_WhenRenamingThrows_ReportsCannotRename()
    {
        static void FailingRename(string _, string __) => throw new UnauthorizedAccessException("Access to the path is denied.");

        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, FailingRename, NoopDelete);

        result.IsUsable.ShouldBeFalse();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotRename);
    }

    [Fact]
    public void Run_WhenRenamingThrows_StillRemovesTheTemporaryFile()
    {
        var deleted = new List<string>();
        static void FailingRename(string _, string __) => throw new IOException("boom");

        DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, FailingRename, deleted.Add);

        deleted.Count.ShouldBe(2);
    }

    [Fact]
    public void Run_WhenDeletingThrows_StillReportsUsable()
    {
        static void FailingDelete(string _) => throw new IOException("locked");

        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, NoopRename, FailingDelete);

        result.IsUsable.ShouldBeTrue();
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Run_WithABlankDirectory_ReportsCannotCreateWithoutTouchingTheDisk(string directory)
    {
        var touched = false;

        var result = DownloadDirectoryProbe.Run(directory, _ => touched = true, NoopWrite, NoopRename, NoopDelete);

        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotCreate);
        touched.ShouldBeFalse();
    }
}
