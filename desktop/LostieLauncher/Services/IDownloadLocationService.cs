using LostieLauncher.Models;

namespace LostieLauncher.Services;

/// <summary>
/// Seam over the disk and environment checks that decide whether a folder can host
/// the game library: the write+rename pre-flight and OneDrive detection. Kept behind
/// an interface so the ViewModels that act on the answers can be unit tested without
/// touching the filesystem or the machine's environment variables.
/// </summary>
public interface IDownloadLocationService
{
    /// <summary>Runs the write+rename pre-flight, creating <paramref name="directory"/> if needed.</summary>
    public DirectoryProbeResult Probe(string directory);

    public bool IsOneDriveSynced(string directory);
}

public sealed class DownloadLocationService : IDownloadLocationService
{
    public DirectoryProbeResult Probe(string directory) => DownloadDirectoryProbe.Run(directory);

    public bool IsOneDriveSynced(string directory) => OneDrivePathPolicy.IsSynced(directory, OneDrivePathPolicy.GetEnvironmentSyncRoots());
}
