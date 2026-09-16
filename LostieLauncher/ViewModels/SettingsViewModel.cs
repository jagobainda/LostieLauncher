using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using LostieLauncher.Content;
using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.Views.Dialogs;
using Microsoft.Win32;
using System.Windows;

namespace LostieLauncher.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    private static SettingsViewModel? _instance;

    public static SettingsViewModel Instance => ResolveInstance(_instance);

    internal static SettingsViewModel ResolveInstance(SettingsViewModel? instance) => instance
        ?? throw new InvalidOperationException(
            $"{nameof(SettingsViewModel)}.{nameof(Instance)} was accessed before the DI container "
            + "constructed the singleton. Resolve SettingsViewModel (e.g. via MainViewModel) before "
            + "any static access, including XAML {x:Static} bindings.");

    [ObservableProperty]
    public partial AppLanguage Language { get; set; } = AppLanguage.Esp;

    [ObservableProperty]
    public partial IStrings Strings { get; set; } = new Esp();

    [ObservableProperty]
    public partial AppTheme Theme { get; set; } = AppTheme.Volcarona;

    [ObservableProperty]
    public partial bool StartWithWindows { get; set; }

    [ObservableProperty]
    public partial bool StartMinimized { get; set; }

    [ObservableProperty]
    public partial bool AutoUpdate { get; set; } = false;

    [ObservableProperty]
    public partial string DownloadDirectory { get; set; } = DownloadDefaults.DownloadDirectory;

    /// <summary>The folder the games actually land in, shown so the user never has to guess.</summary>
    [ObservableProperty]
    public partial string GamesRootDirectory { get; set; } = string.Empty;

    /// <summary>Drives the Settings warning banner for a library that already sits under OneDrive.</summary>
    [ObservableProperty]
    public partial bool IsDownloadDirectoryOneDriveSynced { get; set; }

    public static AppLanguage[] LanguageOptions { get; } = Enum.GetValues<AppLanguage>();
    public static AppTheme[] ThemeOptions { get; } = Enum.GetValues<AppTheme>();

    private ResourceDictionary? _activeThemeDict;

    private readonly ISettingsService _settingsService;
    private readonly IWindowsStartupService _windowsStartupService;
    private readonly GlobalViewModel _globalViewModel;
    private readonly IUpdateService _updateService;
    private readonly IDownloadLocationService _downloadLocationService;
    private readonly IDownloadLocationNotifier _downloadLocationNotifier;
    private bool _hasSeenWelcome;
    private bool _isLoading;

    public SettingsViewModel(ISettingsService settingsService, IWindowsStartupService windowsStartupService, GlobalViewModel globalViewModel,
        IUpdateService updateService, IDownloadLocationService downloadLocationService, IDownloadLocationNotifier downloadLocationNotifier)
    {
        _instance = this;
        _settingsService = settingsService;
        _windowsStartupService = windowsStartupService;
        _globalViewModel = globalViewModel;
        _updateService = updateService;
        _downloadLocationService = downloadLocationService;
        _downloadLocationNotifier = downloadLocationNotifier;

        try
        {
            _activeThemeDict = Application.Current.Resources.MergedDictionaries
                .FirstOrDefault(d => d.Source != null &&
                    (d.Source.OriginalString.Contains("/Themes/") ||
                     d.Source.OriginalString.Contains("Themes/")));

            if (_activeThemeDict == null) ApplyTheme(Theme);

            LoadSettings();
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private void LoadSettings()
    {
        _isLoading = true;
        try
        {
            var settings = _settingsService.Load();
            Language = settings.Language;
            Theme = settings.Theme;
            StartWithWindows = _windowsStartupService.IsEnabled();
            StartMinimized = settings.StartMinimized;
            AutoUpdate = settings.AutoUpdate;
            DownloadDirectory = settings.DownloadDirectory;
            _hasSeenWelcome = settings.HasSeenWelcome;

            _settingsService.EnsureGamesRootDirectoryExists();
            RefreshGamesRootState();
            Logs.DebugLogManager("Settings loaded.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
        finally
        {
            _isLoading = false;
        }
    }

    private void SaveSettings()
    {
        if (_isLoading) return;

        _settingsService.Save(new AppSettings
        {
            Language = Language,
            Theme = Theme,
            StartWithWindows = StartWithWindows,
            StartMinimized = StartMinimized,
            AutoUpdate = AutoUpdate,
            DownloadDirectory = DownloadDirectory,
            HasSeenWelcome = _hasSeenWelcome
        });
        Logs.DebugLogManager("Settings saved.");
    }

    partial void OnLanguageChanged(AppLanguage value)
    {
        Logs.DebugLogManager($"Language changed to: {value}.");
        Strings = value switch
        {
            AppLanguage.Eng => new Eng(),
            AppLanguage.Cat => new Cat(),
            AppLanguage.Eus => new Eus(),
            AppLanguage.Gal => new Gal(),
            AppLanguage.Por => new Por(),
            AppLanguage.Val => new Val(),
            AppLanguage.Fra => new Fra(),
            _ => new Esp()
        };
        SaveSettings();
    }

    partial void OnThemeChanged(AppTheme value)
    {
        Logs.DebugLogManager($"Theme changed to: {value}.");
        ApplyTheme(value);
        SaveSettings();
    }

    partial void OnStartWithWindowsChanged(bool value)
    {
        if (!_isLoading)
        {
            var succeeded = value ? _windowsStartupService.Enable() : _windowsStartupService.Disable();
            if (!succeeded)
            {
                // The registry write failed (e.g. ProcessPath unavailable or the run key is not
                // writable): revert the toggle to the real state so the UI never shows "on" for a
                // startup entry that was never written (BUG-052).
                _isLoading = true;
                try
                {
                    StartWithWindows = _windowsStartupService.IsEnabled();
                }
                finally
                {
                    _isLoading = false;
                }
                return;
            }
        }
        Logs.InfoLogManager($"Start with Windows: {(value ? "enabled" : "disabled")}.");
        SaveSettings();
    }
    partial void OnStartMinimizedChanged(bool value)
    {
        Logs.InfoLogManager($"Start minimized: {(value ? "enabled" : "disabled")}.");
        SaveSettings();
    }
    partial void OnAutoUpdateChanged(bool value)
    {
        Logs.InfoLogManager($"Auto update: {(value ? "enabled" : "disabled")}.");
        SaveSettings();
    }
    partial void OnDownloadDirectoryChanged(string value)
    {
        Logs.InfoLogManager($"Download directory changed to: {value}.");
        SaveSettings();
        _settingsService.EnsureGamesRootDirectoryExists();
        RefreshGamesRootState();
    }

    /// <summary>
    /// Recomputes what the Settings screen says about the library location. The OneDrive
    /// check runs on every load too, so a user who installed while the default was still
    /// Documents is told their library sits in a synced folder instead of silently keeping
    /// it there.
    /// </summary>
    private void RefreshGamesRootState()
    {
        try
        {
            GamesRootDirectory = _settingsService.GetGamesRootDirectory();
            IsDownloadDirectoryOneDriveSynced = _downloadLocationService.IsOneDriveSynced(GamesRootDirectory);

            if (IsDownloadDirectoryOneDriveSynced)
                Logs.InfoLogManager($"The games root sits under a OneDrive-synced folder: {GamesRootDirectory}.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private void ApplyTheme(AppTheme theme)
    {
        if (TryApplyTheme(theme)) return;

        if (theme != AppTheme.Volcarona && TryApplyTheme(AppTheme.Volcarona))
            Logs.ErrorLogManager($"Theme '{theme}' could not be applied; reverted to default '{AppTheme.Volcarona}'.");
    }

    private bool TryApplyTheme(AppTheme theme)
    {
        try
        {
            var dicts = Application.Current.Resources.MergedDictionaries;

            var newThemeDict = new ResourceDictionary
            {
                Source = new Uri($"pack://application:,,,/Themes/{theme}.xaml")
            };

            if (_activeThemeDict != null) dicts.Remove(_activeThemeDict);

            dicts.Add(newThemeDict);
            _activeThemeDict = newThemeDict;
            Logs.DebugLogManager($"Theme applied: {theme}.");
            return true;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }

    public bool HasSeenWelcome => _hasSeenWelcome;
    public static string CurrentVersion => FormatVersion(typeof(SettingsViewModel).Assembly.GetName().Version);

    internal static string FormatVersion(Version? version)
    {
        if (version is null) return "Unknown";

        var availableFields = version.Revision >= 0 ? 4 : version.Build >= 0 ? 3 : 2;
        return "v" + version.ToString(Math.Min(3, availableFields));
    }

    public void MarkWelcomeSeen()
    {
        _hasSeenWelcome = true;
        SaveSettings();
    }

    [RelayCommand]
    private void BrowseDownloadDirectory()
    {
        var result = CustomMessageBox.Show(Strings.ChangeDownloadDirTitle, Strings.ChangeDownloadDirMessage, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information);
        if (result != true) return;

        var dialog = new OpenFolderDialog();

        if (!string.IsNullOrEmpty(DownloadDirectory)) dialog.InitialDirectory = DownloadDirectory;

        if (dialog.ShowDialog() != true) return;

        if (!AcceptDownloadDirectory(dialog.FolderName)) return;

        Logs.InfoLogManager($"Download directory changed to: {dialog.FolderName}.");
        DownloadDirectory = dialog.FolderName;
    }

    /// <summary>
    /// Gates a freshly picked folder: a folder that grants write but not delete would accept
    /// gigabytes and then fail the rename that finalizes every download, and a OneDrive-synced
    /// folder is a bad host the user should at least be warned about.
    /// </summary>
    private bool AcceptDownloadDirectory(string directory)
    {
        var probe = _downloadLocationService.Probe(directory);
        if (!probe.IsUsable)
        {
            Logs.ErrorLogManager($"Rejected download directory '{directory}': probe failed at {probe.Outcome} ({probe.Error}).");
            _downloadLocationNotifier.NotifyDirectoryNotUsable(probe);
            return false;
        }

        if (!_downloadLocationService.IsOneDriveSynced(directory)) return true;

        Logs.InfoLogManager($"Picked download directory '{directory}' is OneDrive-synced; asking the user to confirm.");
        if (_downloadLocationNotifier.ConfirmOneDriveDirectory(directory)) return true;

        Logs.InfoLogManager("The user declined the OneDrive-synced download directory.");
        return false;
    }

    [RelayCommand]
    private async Task CheckForUpdatesAsync()
    {
        Logs.InfoLogManager("Manual update check initiated.");

        if (_globalViewModel.IsDownloading)
        {
            Logs.InfoLogManager("Manual update check blocked: a download is in progress.");
            _updateService.NotifyDownloadInProgress();
            return;
        }

        await _updateService.CheckForUpdatesAsync(notifyWhenUpToDate: true);
    }
}
