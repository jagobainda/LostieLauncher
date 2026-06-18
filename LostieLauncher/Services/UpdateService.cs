using LostieLauncher.ViewModels;
using LostieLauncher.Views.Dialogs;
using Velopack;

namespace LostieLauncher.Services;

public interface IUpdateService
{
    public Task CheckForUpdatesAsync(bool notifyWhenUpToDate);

    public void NotifyDownloadInProgress();
}

public sealed class UpdateService : IUpdateService
{
    private readonly string _feedUrl;

    public UpdateService(Models.UpdateOptions options)
    {
        ArgumentNullException.ThrowIfNull(options);
        _feedUrl = options.FeedUrl;
    }

    public async Task CheckForUpdatesAsync(bool notifyWhenUpToDate)
    {
        try
        {
            Logs.InfoLogManager("Checking for updates...");

            var mgr = new UpdateManager(_feedUrl);

            var updateInfo = await mgr.CheckForUpdatesAsync().ConfigureAwait(false);

            if (updateInfo == null)
            {
                Logs.InfoLogManager("No updates available.");
                if (notifyWhenUpToDate) ShowOnUi(ShowUpToDate);
                return;
            }

            Logs.InfoLogManager($"Update available: {updateInfo.TargetFullRelease.Version}. Downloading...");

            await mgr.DownloadUpdatesAsync(updateInfo).ConfigureAwait(false);

            ShowOnUi(() => PromptAndApply(mgr, updateInfo));
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    public void NotifyDownloadInProgress() => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        CustomMessageBox.Show(strings.UpdateCheckBusyTitle, strings.UpdateCheckBusyMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
    });

    private static void ShowUpToDate()
    {
        var strings = SettingsViewModel.Instance.Strings;
        CustomMessageBox.Show(strings.UpToDateTitle, strings.UpToDateMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
    }

    private static void PromptAndApply(UpdateManager mgr, UpdateInfo updateInfo)
    {
        var strings = SettingsViewModel.Instance.Strings;

        var result = CustomMessageBox.Show(strings.UpdateAvailableTitle, string.Format(strings.UpdateAvailableMessage, updateInfo.TargetFullRelease.Version), CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Update);

        if (result == true)
        {
            Logs.InfoLogManager($"Applying update to {updateInfo.TargetFullRelease.Version} and restarting.");
            mgr.ApplyUpdatesAndRestart(updateInfo.TargetFullRelease);
        }
        else
        {
            Logs.InfoLogManager($"Update to {updateInfo.TargetFullRelease.Version} declined by user.");
        }
    }

    private static void ShowOnUi(Action action)
    {
        var app = Application.Current;
        if (app is null)
        {
            action();
            return;
        }
        app.Dispatcher.Invoke(action);
    }
}
