using Velopack;

namespace LostieLauncher.Services;

public interface IUpdateGateway
{
    public Task<IUpdatePackage?> CheckForUpdatesAsync();
}

public interface IUpdatePackage
{
    public string Version { get; }
    public Task DownloadAsync();
    public void ApplyAndRestart();
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
