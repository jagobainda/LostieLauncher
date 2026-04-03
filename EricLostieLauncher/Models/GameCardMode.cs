namespace EricLostieLauncher.Models;

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
