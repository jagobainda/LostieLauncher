using LostieLauncher.Models;

namespace LostieLauncher.Services;

public interface IDownloadLocationService
{
    public DirectoryProbeResult Probe(string directory);

    public bool IsOneDriveSynced(string directory);
}

public sealed class DownloadLocationService : IDownloadLocationService
{
    public DirectoryProbeResult Probe(string directory) => DownloadDirectoryProbe.Run(directory);

    public bool IsOneDriveSynced(string directory) => OneDrivePathPolicy.IsSynced(directory, OneDrivePathPolicy.GetEnvironmentSyncRoots());
}
