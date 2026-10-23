using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class GameCardTakeoverPolicyTests
{
    [Theory]
    [InlineData(GameDownloadStatus.Available)]
    [InlineData(GameDownloadStatus.UpdateAvailable)]
    [InlineData(GameDownloadStatus.Downloaded)]
    public void ForStatusChange_WhenADownloadStarts_ReturnsInstallStarted(GameDownloadStatus previous)
    {
        // Act
        var takeover = GameCardTakeoverPolicy.ForStatusChange(previous, GameDownloadStatus.Downloading);

        // Assert
        takeover.ShouldBe(GameCardTakeover.InstallStarted);
    }

    [Theory]
    [InlineData(GameDownloadStatus.Downloading)]
    [InlineData(GameDownloadStatus.Paused)]
    [InlineData(GameDownloadStatus.VerifyingIntegrity)]
    [InlineData(GameDownloadStatus.Extracting)]
    public void ForStatusChange_WhenAnInstallInProgressCompletes_ReturnsInstallFinished(GameDownloadStatus previous)
    {
        // Act
        var takeover = GameCardTakeoverPolicy.ForStatusChange(previous, GameDownloadStatus.Downloaded);

        // Assert
        takeover.ShouldBe(GameCardTakeover.InstallFinished);
    }

    [Theory]
    [InlineData(GameDownloadStatus.Paused, GameDownloadStatus.Downloading)]
    [InlineData(GameDownloadStatus.Downloading, GameDownloadStatus.Paused)]
    [InlineData(GameDownloadStatus.Downloading, GameDownloadStatus.VerifyingIntegrity)]
    [InlineData(GameDownloadStatus.Extracting, GameDownloadStatus.Available)]
    [InlineData(GameDownloadStatus.Extracting, GameDownloadStatus.UpdateAvailable)]
    [InlineData(GameDownloadStatus.Available, GameDownloadStatus.Downloaded)]
    public void ForStatusChange_ForOtherTransitions_ReturnsNull(GameDownloadStatus previous, GameDownloadStatus current)
    {
        // Act
        var takeover = GameCardTakeoverPolicy.ForStatusChange(previous, current);

        // Assert
        takeover.ShouldBeNull();
    }
}
