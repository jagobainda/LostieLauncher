using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

/// <summary>
/// The adapter is deliberately thin — the decisions live in <c>DownloadDirectoryProbe</c> and
/// <c>OneDrivePathPolicy</c>, which have their own tests. These cover the wiring only.
/// </summary>
public class DownloadLocationServiceTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("location");

    public void Dispose() => _temp.Dispose();

    private static DownloadLocationService CreateSut() => new();

    [Fact]
    public void Probe_OnAWritableDirectory_ReportsUsableAndLeavesNothingBehind()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        var result = sut.Probe(_temp.Path);

        // Assert
        result.Outcome.ShouldBe(DirectoryProbeOutcome.Usable);
        Directory.EnumerateFileSystemEntries(_temp.Path).ShouldBeEmpty();
    }

    [Fact]
    public void IsOneDriveSynced_ForAFolderNamedOneDrive_ReturnsTrue()
    {
        // Arrange — the folder-name fallback holds whatever the machine's environment says,
        // so this assertion does not depend on the agent having OneDrive installed.
        var sut = CreateSut();

        // Act
        var synced = sut.IsOneDriveSynced(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher");

        // Assert
        synced.ShouldBeTrue();
    }
}
