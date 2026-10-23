using LostieLauncher.Models;

namespace LostieLauncher.Utils;

public static class GameCardTakeoverPolicy
{
    public static readonly TimeSpan TakeoverFade = TimeSpan.FromMilliseconds(250);
    public static readonly TimeSpan EjectLoop = TimeSpan.FromMilliseconds(900);

    public static GameCardTakeover? ForStatusChange(GameDownloadStatus previous, GameDownloadStatus current) => previous switch
    {
        GameDownloadStatus.Downloading or GameDownloadStatus.Paused or GameDownloadStatus.VerifyingIntegrity or GameDownloadStatus.Extracting
            => current == GameDownloadStatus.Downloaded ? GameCardTakeover.InstallFinished : null,
        _ => current == GameDownloadStatus.Downloading ? GameCardTakeover.InstallStarted : null
    };
}
