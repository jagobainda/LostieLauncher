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

    private NotifyIcon? _notifyIcon;
    private ToolStripMenuItem? _trayOpenItem;
    private ToolStripMenuItem? _trayExitItem;

    protected override void OnStartup(StartupEventArgs e)
    {
        VelopackApp.Build().Run();

        ShutdownMode = ShutdownMode.OnExplicitShutdown;

        DispatcherUnhandledException += (_, ex) =>
        {
            Logs.ErrorLogManager(ex.Exception);
            ex.Handled = true;
        };
        AppDomain.CurrentDomain.UnhandledException += (_, ex) =>
        {
            if (ex.ExceptionObject is Exception e) Logs.ErrorLogManager(e);
        };

        Services = DependencyInjection.Configure();

        Logs.InfoLogManager("Application started.");

        _ = Task.Run(CheckForUpdatesAsync);

        var mainWindow = Services.GetRequiredService<MainWindow>();

        InitializeTrayIcon();

        mainWindow.Show();

        if (SettingsViewModel.Instance.StartMinimized)
        {
            Logs.DebugLogManager("Window hidden on startup (StartMinimized).");
            mainWindow.Hide();
        }

        base.OnStartup(e);
    }

    private void InitializeTrayIcon()
    {
        _trayOpenItem = new ToolStripMenuItem();
        _trayExitItem = new ToolStripMenuItem();

        UpdateTrayMenuText();

        _trayOpenItem.Click += (_, _) => RestoreMainWindow();
        _trayExitItem.Click += (_, _) => Shutdown();

        var contextMenu = new ContextMenuStrip();
        contextMenu.Items.Add(_trayOpenItem);
        contextMenu.Items.Add(new ToolStripSeparator());
        contextMenu.Items.Add(_trayExitItem);

        Icon? icon = null;
        try { icon = Icon.ExtractAssociatedIcon(Environment.ProcessPath!); } catch (Exception ex) { Logs.ErrorLogManager(ex); }

        _notifyIcon = new NotifyIcon
        {
            Icon = icon ?? SystemIcons.Application,
            Text = "EricLostie Launcher",
            ContextMenuStrip = contextMenu,
            Visible = true
        };

        _notifyIcon.DoubleClick += (_, _) => RestoreMainWindow();

        Logs.DebugLogManager("Tray icon initialized.");

        SettingsViewModel.Instance.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(SettingsViewModel.Strings)) UpdateTrayMenuText();
        };
    }

    private void UpdateTrayMenuText()
    {
        var strings = SettingsViewModel.Instance.Strings;
        _trayOpenItem?.Text = strings.TrayOpen;
        _trayExitItem?.Text = strings.TrayExit;
    }

    private static void RestoreMainWindow()
    {
        Logs.DebugLogManager("Main window restored from tray.");
        var mainWindow = Services.GetRequiredService<MainWindow>();
        mainWindow.Show();
        mainWindow.WindowState = WindowState.Normal;
        mainWindow.Activate();
    }

    private async Task CheckForUpdatesAsync()
    {
        try
        {
            Logs.InfoLogManager("Checking for updates...");

            var mgr = new UpdateManager(feedUrl);

            var updateInfo = await mgr.CheckForUpdatesAsync();

            if (updateInfo == null)
            {
                Logs.InfoLogManager("No updates available.");
                return;
            }

            Logs.InfoLogManager($"Update available: {updateInfo.TargetFullRelease.Version}. Downloading...");

            await mgr.DownloadUpdatesAsync(updateInfo);

            Dispatcher.Invoke(() =>
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
            });
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Logs.InfoLogManager("Application exiting.");
        _notifyIcon?.Dispose();
        base.OnExit(e);
    }
}

