using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadDirectoryProbeTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("probe");

    public void Dispose() => _temp.Dispose();

    // No-op operations, so a test only has to override the step it cares about.
    private static void NoopCreate(string _) { }
    private static void NoopWrite(string _) { }
    private static void NoopRename(string _, string __) { }
    private static void NoopDelete(string _) { }

    // -------------------- Real filesystem --------------------

    [Fact]
    public void Run_OnAWritableDirectory_ReportsUsable()
    {
        // Arrange & Act — the scratch directory grants write, rename and delete.
        var result = DownloadDirectoryProbe.Run(_temp.Path);

        // Assert
        result.IsUsable.ShouldBeTrue();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.Usable);
        result.Directory.ShouldBe(_temp.Path);
        result.Error.ShouldBeNull();
    }

    [Fact]
    public void Run_OnAMissingDirectory_CreatesItAndReportsUsable()
    {
        // Arrange — the games root does not exist yet the first time a user picks a folder.
        var target = _temp.Combine("nested", "downloads");

        // Act
        var result = DownloadDirectoryProbe.Run(target);

        // Assert
        result.IsUsable.ShouldBeTrue();
        Directory.Exists(target).ShouldBeTrue();
    }

    [Fact]
    public void Run_WhenSuccessful_LeavesNoFileBehind()
    {
        // Arrange & Act — the probe must not litter the user's library (acceptance criterion).
        DownloadDirectoryProbe.Run(_temp.Path);

        // Assert
        Directory.EnumerateFileSystemEntries(_temp.Path).ShouldBeEmpty();
    }

    [Fact]
    public void Run_WhenTheDirectoryPathIsAFile_ReportsCannotCreate()
    {
        // Arrange — a path already taken by a file can never be created as a directory.
        var path = _temp.Combine("not-a-directory");
        File.WriteAllText(path, "x");

        // Act
        var result = DownloadDirectoryProbe.Run(path);

        // Assert
        result.IsUsable.ShouldBeFalse();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotCreate);
        result.Error.ShouldNotBeNull();
    }

    // -------------------- Injected operations --------------------
    // The ACL that grants write but denies delete cannot be set up from a unit test without
    // elevating, so the three failure steps are driven through the internal seam instead.

    [Fact]
    public void Run_WhenWritingThrows_ReportsCannotWrite()
    {
        // Arrange
        static void FailingWrite(string _) => throw new UnauthorizedAccessException("no write");

        // Act
        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, FailingWrite, NoopRename, NoopDelete);

        // Assert
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotWrite);
        result.Error.ShouldNotBeNull();
        result.Error.ShouldContain("UnauthorizedAccessException");
    }

    [Fact]
    public void Run_WhenRenamingThrows_ReportsCannotRename()
    {
        // Arrange — the exact production vector: the folder accepts the write, then refuses the
        // rename that finalizes the download because DELETE is denied on it.
        static void FailingRename(string _, string __) => throw new UnauthorizedAccessException("Access to the path is denied.");

        // Act
        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, FailingRename, NoopDelete);

        // Assert — a write-only check would have called this folder usable.
        result.IsUsable.ShouldBeFalse();
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotRename);
    }

    [Fact]
    public void Run_WhenRenamingThrows_StillRemovesTheTemporaryFile()
    {
        // Arrange — cleanup must run on the failure path too, or a rejected folder collects
        // one orphan probe file per attempt.
        var deleted = new List<string>();
        static void FailingRename(string _, string __) => throw new IOException("boom");

        // Act
        DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, FailingRename, deleted.Add);

        // Assert — both candidate names are cleaned up.
        deleted.Count.ShouldBe(2);
    }

    [Fact]
    public void Run_WhenDeletingThrows_StillReportsUsable()
    {
        // Arrange — cleanup is best-effort: a folder that passed write and rename is usable
        // even if removing the probe file happened to fail.
        static void FailingDelete(string _) => throw new IOException("locked");

        // Act
        var result = DownloadDirectoryProbe.Run(_temp.Path, NoopCreate, NoopWrite, NoopRename, FailingDelete);

        // Assert
        result.IsUsable.ShouldBeTrue();
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void Run_WithABlankDirectory_ReportsCannotCreateWithoutTouchingTheDisk(string directory)
    {
        // Arrange — a blank path would otherwise resolve against the working directory.
        var touched = false;

        // Act
        var result = DownloadDirectoryProbe.Run(directory, _ => touched = true, NoopWrite, NoopRename, NoopDelete);

        // Assert
        result.Outcome.ShouldBe(DirectoryProbeOutcome.CannotCreate);
        touched.ShouldBeFalse();
    }
}
