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
    public static SettingsViewModel Instance { get; private set; } = null!;

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
    public partial string DownloadDirectory { get; set; } = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);

    public static AppLanguage[] LanguageOptions { get; } = Enum.GetValues<AppLanguage>();
    public static AppTheme[] ThemeOptions { get; } = Enum.GetValues<AppTheme>();

    private ResourceDictionary? _activeThemeDict;

    private readonly ISettingsService _settingsService;
    private readonly IWindowsStartupService _windowsStartupService;
    private readonly GlobalViewModel _globalViewModel;
    private readonly IUpdateService _updateService;
    private bool _hasSeenWelcome;
    private bool _isLoading;

    public SettingsViewModel(ISettingsService settingsService, IWindowsStartupService windowsStartupService, GlobalViewModel globalViewModel, IUpdateService updateService)
    {
        Instance = this;
        _settingsService = settingsService;
        _windowsStartupService = windowsStartupService;
        _globalViewModel = globalViewModel;
        _updateService = updateService;

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

        if (dialog.ShowDialog() == true)
        {
            Logs.InfoLogManager($"Download directory changed to: {dialog.FolderName}.");
            DownloadDirectory = dialog.FolderName;
        }
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
