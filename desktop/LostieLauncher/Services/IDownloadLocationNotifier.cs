using LostieLauncher.Content;
using LostieLauncher.Models;
using LostieLauncher.ViewModels;
using LostieLauncher.Views.Dialogs;

namespace LostieLauncher.Services;

public interface IDownloadLocationNotifier
{
    public void NotifyDirectoryNotUsable(DirectoryProbeResult result);

    public bool ConfirmOneDriveDirectory(string directory);
}

public sealed class WpfDownloadLocationNotifier : IDownloadLocationNotifier
{
    public void NotifyDirectoryNotUsable(DirectoryProbeResult result) => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        var message = string.Format(strings.DownloadDirNotUsableMessage, result.Directory, DescribeStep(result.Outcome, strings));
        CustomMessageBox.Show(strings.DownloadDirNotUsableTitle, message, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
    });

    public bool ConfirmOneDriveDirectory(string directory) => ShowOnUi(() =>
    {
        var strings = SettingsViewModel.Instance.Strings;
        var message = string.Format(strings.OneDriveWarningMessage, directory);
        return CustomMessageBox.Show(strings.OneDriveWarningTitle, message, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information) == true;
    });

    private static string DescribeStep(DirectoryProbeOutcome outcome, IStrings strings) => outcome switch
    {
        DirectoryProbeOutcome.CannotCreate => strings.DownloadDirStepCreate,
        DirectoryProbeOutcome.CannotWrite => strings.DownloadDirStepWrite,
        _ => strings.DownloadDirStepRename,
    };

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
