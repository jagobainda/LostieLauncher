using LostieLauncher.ViewModels;
using LostieLauncher.Views.Dialogs;

namespace LostieLauncher.Services;

/// <summary>
/// Seam over the WPF dialogs shown during an update check, so the orchestration in
/// <see cref="UpdateService"/> can be unit tested without instantiating Windows or
/// pumping a real Dispatcher.
/// </summary>
public interface IUpdateNotifier
{
    public void NotifyUpToDate();
    public void NotifyCheckFailed();
    public void NotifyDownloadInProgress();

    /// <summary>Asks the user whether to apply the update now. Returns <c>true</c> to apply.</summary>
    public bool PromptApply(string version);
}

public sealed class WpfUpdateNotifier : IUpdateNotifier
{
    public void NotifyUpToDate() => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        CustomMessageBox.Show(strings.UpToDateTitle, strings.UpToDateMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
    });

    public void NotifyCheckFailed() => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        CustomMessageBox.Show(strings.UpdateCheckFailedTitle, strings.UpdateCheckFailedMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
    });

    public void NotifyDownloadInProgress() => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        CustomMessageBox.Show(strings.UpdateCheckBusyTitle, strings.UpdateCheckBusyMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
    });

    public bool PromptApply(string version) => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        var result = CustomMessageBox.Show(strings.UpdateAvailableTitle, string.Format(strings.UpdateAvailableMessage, version), CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Update);
        return result == true;
    });

    private static void ShowOnUi(Action action)
    {
        var app = Application.Current;
        if (app is null) action();
        else app.Dispatcher.Invoke(action);
    }

    private static T ShowOnUi<T>(Func<T> func)
    {
        var app = Application.Current;
        return app is null ? func() : app.Dispatcher.Invoke(func);
    }
}
