using LostieLauncher.Utils;
using System.Text;

namespace LostieLauncher.Tests.Utils;

public sealed class LogsMaintenanceTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("logs");

    public void Dispose() => _temp.Dispose();

    [Theory]
    [InlineData("2026-06", 0, "2026-06.log")]
    [InlineData("2026-06", 1, "2026-06.1.log")]
    [InlineData("2026-06", 12, "2026-06.12.log")]
    [InlineData("2026-06", -1, "2026-06.log")]
    public void BuildLogFileName_BuildsBaseOrNumberedName(string month, int index, string expected)
    {
        var name = Logs.BuildLogFileName(month, index);

        name.ShouldBe(expected);
    }

    [Theory]
    [InlineData("2026-06.log", true, 0)]
    [InlineData("2026-06.1.log", true, 1)]
    [InlineData("2026-06.10.log", true, 10)]
    public void TryParseLogIndex_WithMatchingMonth_ReturnsIndex(string fileName, bool expected, int expectedIndex)
    {
        var ok = Logs.TryParseLogIndex(fileName, "2026-06", out var index);

        ok.ShouldBe(expected);
        index.ShouldBe(expectedIndex);
    }

    [Theory]
    [InlineData("2026-05.log")]
    [InlineData("2026-06.0.log")]
    [InlineData("2026-06.x.log")]
    [InlineData("2026-06.log.bak")]
    [InlineData("random.log")]
    public void TryParseLogIndex_WithNonMatchingName_ReturnsFalse(string fileName)
    {
        var ok = Logs.TryParseLogIndex(fileName, "2026-06", out _);

        ok.ShouldBeFalse();
    }

    [Fact]
    public void SelectExpiredLogFiles_ReturnsOnlyMonthsOlderThanRetention()
    {
        var now = new DateTimeOffset(2026, 6, 15, 10, 0, 0, TimeSpan.Zero);
        string[] files =
        [
            "2025-11.log",
            "2025-01.3.log",
            "2025-12.log",
            "2026-06.log",
            "2026-06.2.log",
            "notes.log",
        ];

        var expired = Logs.SelectExpiredLogFiles(files, now, retentionMonths: 6);

        expired.ShouldBe(["2025-11.log", "2025-01.3.log"], ignoreOrder: true);
    }

    [Fact]
    public void ProbeMonth_WithNoFiles_ReturnsZeroIndexAndSize()
    {
        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        index.ShouldBe(0);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ProbeMonth_WithBaseFileUnderCap_ContinuesOnSameFile()
    {
        WriteBytes("2026-06.log", 50);

        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        index.ShouldBe(0);
        size.ShouldBe(50L);
    }

    [Fact]
    public void ProbeMonth_WithHighestFileAtCap_RollsToNextIndex()
    {
        WriteBytes("2026-06.log", 1024);

        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        index.ShouldBe(1);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ProbeMonth_WithRolledFilesAndHighestUnderCap_ContinuesOnHighest()
    {
        WriteBytes("2026-06.log", 1024);
        WriteBytes("2026-06.1.log", 200);
        WriteBytes("2026-05.log", 5000);

        var (index, size) = Logs.ProbeMonth(_temp.Path, "2026-06", maxBytes: 1024);

        index.ShouldBe(1);
        size.ShouldBe(200L);
    }

    [Fact]
    public void ResolveActiveFile_FirstWrite_ProbesDirectoryForBaseFile()
    {
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: null, activeMonth: null, activeIndex: 0, activeSize: 0);

        path.ShouldBe(_temp.Combine("2026-06.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(0);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_MonthChange_ReprobesForTheNewMonth()
    {
        WriteBytes("2026-06.log", 200);

        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-05.3.log"), activeMonth: "2026-05", activeIndex: 3, activeSize: 9999);

        path.ShouldBe(_temp.Combine("2026-06.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(0);
        size.ShouldBe(200L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthAtCap_RollsToNextIndexWithoutTouchingDisk()
    {
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.log"), activeMonth: "2026-06", activeIndex: 0, activeSize: 1024);

        path.ShouldBe(_temp.Combine("2026-06.1.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(1);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthAtCapTwice_KeepsClimbingIndices()
    {
        var (path, _, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.1.log"), activeMonth: "2026-06", activeIndex: 1, activeSize: 2048);

        path.ShouldBe(_temp.Combine("2026-06.2.log"));
        index.ShouldBe(2);
        size.ShouldBe(0L);
    }

    [Fact]
    public void ResolveActiveFile_SameMonthUnderCap_KeepsCacheUnchanged()
    {
        var (path, month, index, size) = Logs.ResolveActiveFile(
            _temp.Path, "2026-06", maxBytes: 1024,
            activePath: _temp.Combine("2026-06.1.log"), activeMonth: "2026-06", activeIndex: 1, activeSize: 500);

        path.ShouldBe(_temp.Combine("2026-06.1.log"));
        month.ShouldBe("2026-06");
        index.ShouldBe(1);
        size.ShouldBe(500L);
    }

    [Fact]
    public void PurgeExpiredLogs_DeletesOnlyExpiredMonthsAndReturnsCount()
    {
        var now = new DateTimeOffset(2026, 6, 15, 10, 0, 0, TimeSpan.Zero);
        WriteBytes("2025-10.log", 10);
        WriteBytes("2025-11.2.log", 10);
        WriteBytes("2025-12.log", 10);
        WriteBytes("2026-06.log", 10);
        WriteBytes("keep-me.log", 10);

        var removed = Logs.PurgeExpiredLogs(_temp.Path, now, retentionMonths: 6);

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
        var removed = Logs.PurgeExpiredLogs(_temp.Combine("does-not-exist"), DateTimeOffset.Now, retentionMonths: 6);

        removed.ShouldBe(0);
    }

    private void WriteBytes(string fileName, int count) =>
        File.WriteAllBytes(_temp.Combine(fileName), Encoding.ASCII.GetBytes(new string('x', count)));
}
