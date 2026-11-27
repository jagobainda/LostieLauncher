using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class DownloadLocationServiceTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("location");

    public void Dispose() => _temp.Dispose();

    private static DownloadLocationService CreateSut() => new();

    [Fact]
    public void Probe_OnAWritableDirectory_ReportsUsableAndLeavesNothingBehind()
    {
        var sut = CreateSut();

        var result = sut.Probe(_temp.Path);

        result.Outcome.ShouldBe(DirectoryProbeOutcome.Usable);
        Directory.EnumerateFileSystemEntries(_temp.Path).ShouldBeEmpty();
    }

    [Fact]
    public void IsOneDriveSynced_ForAFolderNamedOneDrive_ReturnsTrue()
    {
        var sut = CreateSut();

        var synced = sut.IsOneDriveSynced(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher");

        synced.ShouldBeTrue();
    }
}
