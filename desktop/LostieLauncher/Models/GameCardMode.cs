namespace LostieLauncher.Models;

public enum GameCardMode
{
    Library,
    Games
}

public enum GameDownloadStatus
{
    Available,
    Downloading,
    Paused,
    Downloaded,
    UpdateAvailable,
    Extracting,
    VerifyingIntegrity
}

public enum GameCardTakeover
{
    InstallStarted,
    InstallFinished,
    Uninstalling
}
