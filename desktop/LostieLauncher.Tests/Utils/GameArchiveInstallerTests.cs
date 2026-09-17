using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class GameArchiveInstallerTests
{
    // ---- Extraction never runs over a surviving .tmp (wave 2) -------------------------------

    [Fact]
    public async Task ExtractAsync_WhenTheLeftoverTempDirectoryCannotBeCleared_AbortsAndLeavesTheInstallUntouched()
    {
        // Arrange — an aborted extraction left a .tmp behind and a lock (an antivirus still scanning
        // the freshly written files, realistically) keeps it there. Directory.CreateDirectory is a
        // no-op on it, so extraction used to proceed into the survivor and promote a mixture of two
        // versions into the live install, reporting success the whole way.
        using var root = new TempDirectoryFixture("extract-leftover");
        var extractDir = root.Combine("Demo");
        var tempDir = extractDir + ".tmp";
        Directory.CreateDirectory(extractDir);
        File.WriteAllText(Path.Combine(extractDir, "game.exe"), "v1.0");
        Directory.CreateDirectory(tempDir);
        var strandedPath = Path.Combine(tempDir, "stranded.bin");
        File.WriteAllText(strandedPath, "half-extracted v2.0");
        using var lockedFile = new FileStream(strandedPath, FileMode.Open, FileAccess.Read, FileShare.None);

        var zipPath = root.Combine("demo.zip");
        File.WriteAllBytes(zipPath, ValidZipArchiveWithSingleEntry());

        // Act
        await Should.ThrowAsync<IOException>(() => GameArchiveInstaller.ExtractAsync(zipPath, extractDir));

        // Assert — the failure surfaces (the caller turns it into the update-failure dialog) and the
        // live installation still holds exactly the version it had before.
        File.ReadAllText(Path.Combine(extractDir, "game.exe")).ShouldBe("v1.0");
        Directory.EnumerateFiles(extractDir).Count().ShouldBe(1);
        File.Exists(strandedPath).ShouldBeTrue();
    }

    [Fact]
    public async Task ExtractAsync_WhenTheLeftoverTempDirectoryCanBeCleared_InstallsTheNewVersion()
    {
        // Arrange — same leftover, nothing holding it: the abort must be about the failed cleanup,
        // not about the .tmp merely being there.
        using var root = new TempDirectoryFixture("extract-leftover-clean");
        var extractDir = root.Combine("Demo");
        Directory.CreateDirectory(extractDir);
        File.WriteAllText(Path.Combine(extractDir, "game.exe"), "v1.0");
        Directory.CreateDirectory(extractDir + ".tmp");
        File.WriteAllText(Path.Combine(extractDir + ".tmp", "stranded.bin"), "half-extracted v2.0");

        var zipPath = root.Combine("demo.zip");
        File.WriteAllBytes(zipPath, ValidZipArchiveWithSingleEntry());

        // Act
        await GameArchiveInstaller.ExtractAsync(zipPath, extractDir);

        // Assert — only what the archive carries is installed; the stranded file did not survive.
        File.ReadAllText(Path.Combine(extractDir, "game.exe")).ShouldBe("v2.0");
        File.Exists(Path.Combine(extractDir, "stranded.bin")).ShouldBeFalse();
        Directory.Exists(extractDir + ".tmp").ShouldBeFalse();
    }

    /// <summary>A minimal, stored zip holding a single <c>game.exe</c> with "v2.0".</summary>
    private static byte[] ValidZipArchiveWithSingleEntry()
    {
        using var buffer = new MemoryStream();
        using (var archive = new System.IO.Compression.ZipArchive(buffer, System.IO.Compression.ZipArchiveMode.Create, leaveOpen: true))
        using (var entry = new StreamWriter(archive.CreateEntry("game.exe").Open()))
            entry.Write("v2.0");

        return buffer.ToArray();
    }

    // -------------------- AtomicSwapDirectories --------------------

    [Fact]
    public void AtomicSwapDirectories_FreshInstall_MovesSourceToTargetAndCleansUp()
    {
        // Arrange — no existing target directory (fresh install)
        using var root = new TempDirectoryFixture("atomicswap-fresh");
        var source = root.Combine("source");
        var backup = root.Combine("backup");
        var target = root.Combine("target");

        Directory.CreateDirectory(source);
        File.WriteAllText(Path.Combine(source, "game.exe"), "v1.0");

        // Act
        GameArchiveInstaller.AtomicSwapDirectories(source, backup, target);

        // Assert — source is gone, target exists with the file, backup does not exist
        Directory.Exists(source).ShouldBeFalse();
        Directory.Exists(target).ShouldBeTrue();
        File.Exists(Path.Combine(target, "game.exe")).ShouldBeTrue();
        File.ReadAllText(Path.Combine(target, "game.exe")).ShouldBe("v1.0");
        Directory.Exists(backup).ShouldBeFalse();
    }

    [Fact]
    public void AtomicSwapDirectories_Update_SwapsTargetToBackupSourceToTargetAndDeletesBackup()
    {
        // Arrange — existing target directory (update scenario)
        using var root = new TempDirectoryFixture("atomicswap-update");
        var source = root.Combine("source");
        var backup = root.Combine("backup");
        var target = root.Combine("target");

        Directory.CreateDirectory(source);
        File.WriteAllText(Path.Combine(source, "game.exe"), "v2.0");
        File.WriteAllText(Path.Combine(source, "new.dll"), "new");

        Directory.CreateDirectory(target);
        File.WriteAllText(Path.Combine(target, "game.exe"), "v1.0");
        File.WriteAllText(Path.Combine(target, "old.dll"), "orphan");

        // Act
        GameArchiveInstaller.AtomicSwapDirectories(source, backup, target);

        // Assert — source gone, target has only v2.0 files (BUG-029: no orphan old.dll), backup gone
        Directory.Exists(source).ShouldBeFalse();
        Directory.Exists(target).ShouldBeTrue();
        File.Exists(Path.Combine(target, "game.exe")).ShouldBeTrue();
        File.ReadAllText(Path.Combine(target, "game.exe")).ShouldBe("v2.0");
        File.Exists(Path.Combine(target, "new.dll")).ShouldBeTrue();
        File.Exists(Path.Combine(target, "old.dll")).ShouldBeFalse();
        Directory.Exists(backup).ShouldBeFalse();
    }

    [Fact]
    public void AtomicSwapDirectories_LeftoverBackup_IsCleanedBeforeSwap()
    {
        // Arrange — a leftover .old directory from a previous crashed swap
        using var root = new TempDirectoryFixture("atomicswap-leftover");
        var source = root.Combine("source");
        var backup = root.Combine("backup");
        var target = root.Combine("target");

        Directory.CreateDirectory(source);
        File.WriteAllText(Path.Combine(source, "game.exe"), "fresh");

        Directory.CreateDirectory(target);
        File.WriteAllText(Path.Combine(target, "game.exe"), "old");

        Directory.CreateDirectory(backup);
        File.WriteAllText(Path.Combine(backup, "stale.txt"), "leftover-from-crash");

        // Act
        GameArchiveInstaller.AtomicSwapDirectories(source, backup, target);

        // Assert — stale backup was cleaned, swap succeeded normally
        Directory.Exists(source).ShouldBeFalse();
        Directory.Exists(target).ShouldBeTrue();
        File.ReadAllText(Path.Combine(target, "game.exe")).ShouldBe("fresh");
        Directory.Exists(backup).ShouldBeFalse();
    }

    [Fact]
    public void AtomicSwapDirectories_WhenThePreviousVersionHasAReadOnlyDirectory_StillDeletesTheBackup()
    {
        using var root = new TempDirectoryFixture("atomicswap-readonly");
        var source = root.Combine("source");
        var backup = root.Combine("backup");
        var target = root.Combine("target");

        Directory.CreateDirectory(source);
        File.WriteAllText(Path.Combine(source, "game.exe"), "v2.0");

        var blocked = Path.Combine(target, "Animations", "Beat_Up_hit_2");
        Directory.CreateDirectory(blocked);
        File.WriteAllText(Path.Combine(target, "game.exe"), "v1.0");
        File.WriteAllText(Path.Combine(blocked, "frame.png"), "pixels");
        File.SetAttributes(blocked, File.GetAttributes(blocked) | FileAttributes.ReadOnly);

        GameArchiveInstaller.AtomicSwapDirectories(source, backup, target);

        Directory.Exists(backup).ShouldBeFalse();
        File.ReadAllText(Path.Combine(target, "game.exe")).ShouldBe("v2.0");
    }
}
