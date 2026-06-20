using LostieLauncher.Tests.Helpers;
using LostieLauncher.Utils;
using System.IO;
using System.Text;

namespace LostieLauncher.Tests.Utils;

/// <summary>
/// Coverage for the log rotation/retention logic introduced for BUG-045. The stateful, synchronous
/// write path of <see cref="Logs"/> targets <c>%LOCALAPPDATA%</c> directly (no path abstraction), but
/// the rotation/purge decisions are extracted into internal helpers that take an explicit directory and
/// clock, so they are exercised end-to-end against a temp directory.
/// </summary>
public sealed class LogsMaintenanceTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("logs");

    public void Dispose() => _temp.Dispose();

    // ---- BuildLogFileName ----

    [Theory]
    [InlineData("2026-06", 0, "2026-06.log")]
    [InlineData("2026-06", 1, "2026-06.1.log")]
    [InlineData("2026-06", 12, "2026-06.12.log")]
    [InlineData("2026-06", -1, "2026-06.log")]
    public void BuildLogFileName_BuildsBaseOrNumberedName(string month, int index, string expected)
    {
        // Act
        var name = Logs.BuildLogFileName(month, index);

        // Assert
        name.ShouldBe(expected);
    }

    // ---- TryParseLogIndex ----

    [Theory]
    [InlineData("2026-06.log", true, 0)]
    [InlineData("2026-06.1.log", true, 1)]
    [InlineData("2026-06.10.log", true, 10)]
    public void TryParseLogIndex_WithMatchingMonth_ReturnsIndex(string fileName, bool expected, int expectedIndex)
    {
        // Act
        var ok = Logs.TryParseLogIndex(fileName, "2026-06", out var index);

        // Assert
        ok.ShouldBe(expected);
        index.ShouldBe(expectedIndex);
    }

    [Theory]
    [InlineData("2026-05.log")]      // different month
    [InlineData("2026-06.0.log")]    // explicit .0 is not a valid rolled index (> 0 required)
    [InlineData("2026-06.x.log")]    // non-numeric middle
    [InlineData("2026-06.log.bak")]  // wrong extension
    [InlineData("random.log")]       // no month prefix
    public void TryParseLogIndex_WithNonMatchingName_ReturnsFalse(string fileName)
    {
        // Act
        var ok = Logs.TryParseLogIndex(fileName, "2026-06", out _);

        // Assert
        ok.ShouldBeFalse();
    }

    // ---- SelectExpiredLogFiles ----

    [Fact]
    public void SelectExpiredLogFiles_ReturnsOnlyMonthsOlderThanRetention()
    {
        // Arrange — now = 2026-06, retention 6 → cutoff 2025-12-01 (strictly older is expired).
        var now = new DateTimeOffset(2026, 6, 15, 10, 0, 0, TimeSpan.Zero);
        string[] files =
        [
            "2025-11.log",    // expired (before cutoff)
            "2025-01.3.log",  // expired (rolled, before cutoff)
            "2025-12.log",    // kept (exactly at cutoff)
            "2026-06.log",    // kept (current month)
            "2026-06.2.log",  // kept (current month, rolled)
            "notes.log",      // ignored (no month prefix)
        ];

        // Act
        var expired = Logs.SelectExpiredLogFiles(files, now, retentionMonths: 6);

        // Assert
        expired.ShouldBe(["2025-11.log", "2025-01.3.log"], ignoreOrder: true);
    }

    // ---- ProbeMonth (directory-driven) ----

    [Fact]
    public void ProbeMonth_WithNoFiles_ReturnsZeroIndexAndSize()
    {
        // Act
        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        // Assert
        index.ShouldBe(0);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ProbeMonth_WithBaseFileUnderCap_ContinuesOnSameFile()
    {
        // Arrange
        WriteBytes("2026-06.log", 50);

        // Act
        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        // Assert
        index.ShouldBe(0);
        size.ShouldBe(50L);
    }

    [Fact]
    public void ProbeMonth_WithHighestFileAtCap_RollsToNextIndex()
    {
        // Arrange — base file is already full.
        WriteBytes("2026-06.log", 1024);

        // Act
        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        // Assert
        index.ShouldBe(1);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ProbeMonth_WithRolledFilesAndHighestUnderCap_ContinuesOnHighest()
    {
        // Arrange — base full, .1 partially written; only files of the current month count.
        WriteBytes("2026-06.log", 1024);
        WriteBytes("2026-06.1.log", 200);
        WriteBytes("2026-05.log", 5000); // different month, must be ignored

        // Act
        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        // Assert
        index.ShouldBe(1);
        size.ShouldBe(200L);
    }

    // ---- ResolveActiveFile (pure state transition, incl. the in-process rotation jump) ----

    [Fact]
    public void ResolveActiveFile_FirstWrite_ProbesDirectoryForBaseFile()
    {
        // Arrange — empty directory, no cached state yet.
        // Act
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: null, activeMonth: null, activeIndex: 0, activeSize: 0);

        // Assert
        path.ShouldBe(_temp.Combine("2026-06.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(0);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_MonthChange_ReprobesForTheNewMonth()
    {
        // Arrange — cache points at a full file from the previous month; new month has an existing base file.
        WriteBytes("2026-06.log", 200);

        // Act
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-05.3.log"), activeMonth: "2026-05", activeIndex: 3, activeSize: 9999);

        // Assert — re-probed for June, not carried over from May's index 3.
        path.ShouldBe(_temp.Combine("2026-06.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(0);
        size.ShouldBe(200L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthAtCap_RollsToNextIndexWithoutTouchingDisk()
    {
        // Arrange — same month, accumulated size has reached the cap. Directory is irrelevant (no probe).
        // Act — this is the in-process rotation branch (HALLAZGO-001).
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.log"), activeMonth: "2026-06", activeIndex: 0, activeSize: 1024);

        // Assert — jumped to index 1, size reset; no file needs to exist for this decision.
        path.ShouldBe(_temp.Combine("2026-06.1.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(1);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthAtCapTwice_KeepsClimbingIndices()
    {
        // Arrange — already on a rolled file (.1) that has filled up again.
        // Act
        var (path, _, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.1.log"), activeMonth: "2026-06", activeIndex: 1, activeSize: 2048);

        // Assert
        path.ShouldBe(_temp.Combine("2026-06.2.log"));
        index.ShouldBe(2);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthUnderCap_KeepsCacheUnchanged()
    {
        // Arrange — same month, still under the cap: nothing rolls, cache is returned verbatim.
        // Act
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.1.log"), activeMonth: "2026-06", activeIndex: 1, activeSize: 500);

        // Assert
        path.ShouldBe(_temp.Combine("2026-06.1.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(1);
        size.ShouldBe(500L);
    }

    // ---- PurgeExpiredLogs (directory-driven) ----

    [Fact]
    public void PurgeExpiredLogs_DeletesOnlyExpiredMonthsAndReturnsCount()
    {
        // Arrange
        var now = new DateTimeOffset(2026, 6, 15, 10, 0, 0, TimeSpan.Zero);
        WriteBytes("2025-10.log", 10);   // expired
        WriteBytes("2025-11.2.log", 10); // expired (rolled)
        WriteBytes("2025-12.log", 10);   // kept (boundary)
        WriteBytes("2026-06.log", 10);   // kept (current)
        WriteBytes("keep-me.log", 10);   // kept (unrecognized name)

        // Act
        var removed = Logs.PurgeExpiredLogs(_temp.Path, now, retentionMonths: 6);

        // Assert
        removed.ShouldBe(2);
        File.Exists(_temp.Combine("2025-10.log")).ShouldBeFalse();
        File.Exists(_temp.Combine("2025-11.2.log")).ShouldBeFalse();
        File.Exists(_temp.Combine("2025-12.log")).ShouldBeTrue();
        File.Exists(_temp.Combine("2026-06.log")).ShouldBeTrue();
        File.Exists(_temp.Combine("keep-me.log")).ShouldBeTrue();
    }

    [Fact]
    public void PurgeExpiredLogs_WithMissingDirectory_ReturnsZero()
    {
        // Act
        var removed = Logs.PurgeExpiredLogs(_temp.Combine("does-not-exist"), DateTimeOffset.Now, retentionMonths: 6);

        // Assert
        removed.ShouldBe(0);
    }

    private void WriteBytes(string fileName, int count) =>
        File.WriteAllBytes(_temp.Combine(fileName), Encoding.ASCII.GetBytes(new string('x', count)));
}
