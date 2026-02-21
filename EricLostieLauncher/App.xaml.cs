using EricLostieLauncher.Core;
using EricLostieLauncher.ViewModels;
using EricLostieLauncher.Views;
using EricLostieLauncher.Views.Dialogs;
using Microsoft.Extensions.DependencyInjection;
using System.Windows;
using Velopack;

namespace EricLostieLauncher;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    private readonly string feedUrl = "https://cdn.jagoba.dev/downloads/skinholder-desktop-latest";

    protected override void OnStartup(StartupEventArgs e)
    {
        VelopackApp.Build().Run();

        Services = DependencyInjection.Configure();

        _ = Task.Run(CheckForUpdatesAsync);

        var loginWindow = Services.GetRequiredService<MainWindow>();
        loginWindow.Show();

        base.OnStartup(e);
    }

    private async Task CheckForUpdatesAsync()
    {
        try
        {
            var mgr = new UpdateManager(feedUrl);

            var updateInfo = await mgr.CheckForUpdatesAsync();

            if (updateInfo == null) return;

            await mgr.DownloadUpdatesAsync(updateInfo);

            Dispatcher.Invoke(() =>
            {
                var strings = SettingsViewModel.Instance.Strings;
                var result = CustomMessageBox.Show(
                    strings.UpdateAvailableTitle,
                    string.Format(strings.UpdateAvailableMessage, updateInfo.TargetFullRelease.Version),
                    CustomMessageBoxButton.YesNo,
                    CustomMessageBoxIcon.Update
                );

                if (result == true) mgr.ApplyUpdatesAndRestart(updateInfo.TargetFullRelease);
            });
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error checking for updates: {ex.Message}");
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        base.OnExit(e);
    }
}
