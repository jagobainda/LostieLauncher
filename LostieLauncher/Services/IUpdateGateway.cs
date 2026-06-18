using Velopack;

namespace LostieLauncher.Services;

/// <summary>
/// Seam over Velopack's <see cref="UpdateManager"/> so the update orchestration in
/// <see cref="UpdateService"/> can be unit tested without touching the network or the
/// not-installed Velopack runtime (which throws when the app isn't packaged).
/// </summary>
public interface IUpdateGateway
{
    /// <summary>Returns the pending update, or <c>null</c> when already up to date.</summary>
    Task<IUpdatePackage?> CheckForUpdatesAsync();
}

/// <summary>A pending update that can be downloaded and applied.</summary>
public interface IUpdatePackage
{
    string Version { get; }
    Task DownloadAsync();
    void ApplyAndRestart();
}

public sealed class VelopackUpdateGateway : IUpdateGateway
{
    private readonly string _feedUrl;

    public VelopackUpdateGateway(Models.UpdateOptions options)
    {
        ArgumentNullException.ThrowIfNull(options);
        _feedUrl = options.FeedUrl;
    }

    public async Task<IUpdatePackage?> CheckForUpdatesAsync()
    {
        var mgr = new UpdateManager(_feedUrl);
        var updateInfo = await mgr.CheckForUpdatesAsync().ConfigureAwait(false);
        return updateInfo is null ? null : new VelopackUpdatePackage(mgr, updateInfo);
    }

    private sealed class VelopackUpdatePackage(UpdateManager mgr, UpdateInfo updateInfo) : IUpdatePackage
    {
        public string Version => updateInfo.TargetFullRelease.Version.ToString();

        public Task DownloadAsync() => mgr.DownloadUpdatesAsync(updateInfo);

        public void ApplyAndRestart() => mgr.ApplyUpdatesAndRestart(updateInfo.TargetFullRelease);
    }
}
