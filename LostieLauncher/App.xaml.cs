using LostieLauncher.Core;
using LostieLauncher.ViewModels;
using LostieLauncher.Views;
using LostieLauncher.Views.Dialogs;
using Microsoft.Extensions.DependencyInjection;
using System.Diagnostics;
using System.Windows;
using Velopack;

namespace LostieLauncher;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    private readonly string feedUrl = "https://ericlostie-launcher.jagoba.dev/public/installer/";

    private Mutex? _instanceMutex;
    private EventWaitHandle? _showWindowEvent;
    private CancellationTokenSource? _singleInstanceListenerCts;

    private const string MutexName = "LostieLauncherSingleInstance";
    private const string EventName = "LostieLauncherShowWindow";

    private const string FatalErrorTitle = "Lostie Launcher";
    private const string FatalErrorMessage = "A fatal error occurred and the launcher must close.\n\nThe details have been written to the log.";

    private bool _startupCompleted;

    private NotifyIcon? _notifyIcon;
    private ToolStripMenuItem? _trayOpenItem;
    private ToolStripMenuItem? _trayExitItem;

    protected override void OnStartup(StartupEventArgs e)
    {
        VelopackApp.Build().Run();

        _instanceMutex = new Mutex(true, MutexName, out var isNewInstance);
        if (!isNewInstance)
        {
            Logs.InfoLogManager("Another instance is already running. Signaling it and shutting down.");
            _instanceMutex.Dispose();
            _instanceMutex = null;
            if (EventWaitHandle.TryOpenExisting(EventName, out var showEvent))
            {
                showEvent.Set();
                showEvent.Dispose();
            }
            else
            {
                Logs.DebugLogManager("Could not open existing show-window event.");
            }
            Shutdown();
            return;
        }

        _showWindowEvent = new EventWaitHandle(false, EventResetMode.AutoReset, EventName);
        _singleInstanceListenerCts = new CancellationTokenSource();
        Logs.DebugLogManager("Single-instance listener started.");
        _ = Task.Run(() =>
        {
            var token = _singleInstanceListenerCts.Token;
            while (!token.IsCancellationRequested)
            {
                if (_showWindowEvent.WaitOne(1000)) Dispatcher.BeginInvoke(RestoreMainWindow);
            }
        });

        ShutdownMode = ShutdownMode.OnExplicitShutdown;

        DispatcherUnhandledException += OnDispatcherUnhandledException;
        AppDomain.CurrentDomain.UnhandledException += (_, ex) =>
        {
            if (ex.ExceptionObject is Exception e) Logs.ErrorLogManager(e);
        };
        TaskScheduler.UnobservedTaskException += (_, ex) =>
        {
            Logs.ErrorLogManager(ex.Exception);
            ex.SetObserved();
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

        if (!SettingsViewModel.Instance.HasSeenWelcome)
        {
            Logs.InfoLogManager("First launch detected — navigating to library and showing welcome dialog.");
            var mainViewModel = Services.GetRequiredService<MainViewModel>();
            mainViewModel.NavigateToLibraryCommand.Execute(null);
            SettingsViewModel.Instance.MarkWelcomeSeen();
            WelcomeDialog.Show(mainWindow);
            Logs.InfoLogManager("Welcome dialog shown.");
        }

        base.OnStartup(e);

        _startupCompleted = true;
    }

    private void OnDispatcherUnhandledException(object sender, System.Windows.Threading.DispatcherUnhandledExceptionEventArgs e)
    {
        Logs.ErrorLogManager(e.Exception);

        e.Handled = true;

        if (UnhandledExceptionPolicy.Decide(_startupCompleted) == UnhandledExceptionAction.Fatal) HandleFatalException();
    }

    private void HandleFatalException()
    {
        try
        {
            System.Windows.MessageBox.Show(FatalErrorMessage, FatalErrorTitle, MessageBoxButton.OK, MessageBoxImage.Error);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }

        Shutdown();
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
            Text = "Lostie Launcher",
            ContextMenuStrip = contextMenu,
            Visible = true
        };

        _notifyIcon.DoubleClick += (_, _) =>
        {
            Logs.DebugLogManager("Tray icon double-clicked.");
            RestoreMainWindow();
        };

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
        Logs.DebugLogManager("Tray menu text updated.");
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

    internal void ReleaseSingleInstanceLock()
    {
        Debug.Assert(CheckAccess(), "ReleaseSingleInstanceLock must run on the UI thread (mutex affinity).");

        if (_instanceMutex is null) return;

        try { _instanceMutex.ReleaseMutex(); } catch (Exception ex) { Logs.ErrorLogManager(ex); }
        _instanceMutex.Dispose();
        _instanceMutex = null;
        Logs.DebugLogManager("Single-instance lock released for restart.");
    }

    internal void ReacquireSingleInstanceLock()
    {
        Debug.Assert(CheckAccess(), "ReacquireSingleInstanceLock must run on the UI thread (mutex affinity).");

        _instanceMutex ??= new Mutex(true, MutexName, out _);
        Logs.DebugLogManager("Single-instance lock re-acquired after a failed restart.");
    }

    protected override void OnExit(ExitEventArgs e)
    {
        Logs.InfoLogManager("Application exiting.");
        _singleInstanceListenerCts?.Cancel();
        _singleInstanceListenerCts?.Dispose();
        _showWindowEvent?.Dispose();
        try { _instanceMutex?.ReleaseMutex(); } catch (Exception ex) { Logs.ErrorLogManager(ex); }
        _instanceMutex?.Dispose();
        try { _notifyIcon?.Dispose(); } catch (Exception ex) { Logs.ErrorLogManager(ex); }
        try { (Services as IDisposable)?.Dispose(); } catch (Exception ex) { Logs.ErrorLogManager(ex); }
        base.OnExit(e);
    }
}

