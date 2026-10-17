namespace LostieLauncher.Services;

public interface IUpdateService
{
    public Task CheckForUpdatesAsync(bool notifyWhenUpToDate);

    public void NotifyDownloadInProgress();
}

public sealed class UpdateService : IUpdateService
{
    private readonly IUpdateGateway _gateway;
    private readonly IUpdateNotifier _notifier;

    public UpdateService(IUpdateGateway gateway, IUpdateNotifier notifier)
    {
        ArgumentNullException.ThrowIfNull(gateway);
        ArgumentNullException.ThrowIfNull(notifier);
        _gateway = gateway;
        _notifier = notifier;
    }

    public async Task CheckForUpdatesAsync(bool notifyWhenUpToDate)
    {
        try
        {
            Logs.InfoLogManager("Checking for updates...");

            var update = await _gateway.CheckForUpdatesAsync().ConfigureAwait(false);

            if (update is null)
            {
                Logs.InfoLogManager("No updates available.");
                if (notifyWhenUpToDate) _notifier.NotifyUpToDate();
                return;
            }

            Logs.InfoLogManager($"Update available: {update.Version}. Downloading...");

            await update.DownloadAsync().ConfigureAwait(false);

            PromptAndApply(update);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            if (notifyWhenUpToDate) _notifier.NotifyCheckFailed();
        }
    }

    public void NotifyDownloadInProgress() => _notifier.NotifyDownloadInProgress();

    private void PromptAndApply(IUpdatePackage update)
    {
        if (_notifier.PromptApply(update.Version))
        {
            Logs.InfoLogManager($"Applying update to {update.Version} and restarting.");
            update.ApplyAndRestart();
        }
        else
        {
            Logs.InfoLogManager($"Update to {update.Version} declined by user.");
        }
    }
}
