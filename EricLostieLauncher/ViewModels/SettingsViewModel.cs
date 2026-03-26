using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Content;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;
using EricLostieLauncher.Utils;
using EricLostieLauncher.Views.Dialogs;
using Microsoft.Win32;

namespace EricLostieLauncher.ViewModels;

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
    public partial bool AutoUpdate { get; set; } = true;

    [ObservableProperty]
    public partial string DownloadDirectory { get; set; } = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);

    public static AppLanguage[] LanguageOptions { get; } = Enum.GetValues<AppLanguage>();
    public static AppTheme[] ThemeOptions { get; } = Enum.GetValues<AppTheme>();

    private ResourceDictionary? _activeThemeDict;

    private readonly ISettingsService _settingsService;
    private readonly IWindowsStartupService _windowsStartupService;
    private bool _hasSeenWelcome;
    private bool _isLoading;

    public SettingsViewModel(ISettingsService settingsService, IWindowsStartupService windowsStartupService)
    {
        Instance = this;
        _settingsService = settingsService;
        _windowsStartupService = windowsStartupService;

        _activeThemeDict = Application.Current.Resources.MergedDictionaries
            .FirstOrDefault(d => d.Source != null &&
                (d.Source.OriginalString.Contains("/Themes/") ||
                 d.Source.OriginalString.Contains("Themes/")));

        if (_activeThemeDict == null) ApplyTheme(Theme);

        LoadSettings();
    }

    private void LoadSettings()
    {
        _isLoading = true;
        var settings = _settingsService.Load();
        Language = settings.Language;
        Theme = settings.Theme;
        StartWithWindows = _windowsStartupService.IsEnabled();
        StartMinimized = settings.StartMinimized;
        AutoUpdate = settings.AutoUpdate;
        DownloadDirectory = settings.DownloadDirectory;
        _hasSeenWelcome = settings.HasSeenWelcome;
        _isLoading = false;

        _settingsService.EnsureGamesRootDirectoryExists();
        Logs.DebugLogManager("Settings loaded.");
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
            if (value) _windowsStartupService.Enable();
            else _windowsStartupService.Disable();
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
        var dicts = Application.Current.Resources.MergedDictionaries;

        if (_activeThemeDict != null) dicts.Remove(_activeThemeDict);

        _activeThemeDict = new ResourceDictionary
        {
            Source = new Uri($"pack://application:,,,/Themes/{theme}.xaml")
        };

        dicts.Add(_activeThemeDict);
    }

    public bool HasSeenWelcome => _hasSeenWelcome;
    public static string CurrentVersion => "v" + typeof(SettingsViewModel).Assembly.GetName().Version?.ToString() ?? "Unknown";

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
    private void CheckForUpdates()
    {
        Logs.InfoLogManager("Manual update check initiated.");
        var result = CustomMessageBox.Show(Strings.CheckForUpdatesTitle, Strings.CheckForUpdatesMessage, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Update);

        if (result == true) ProcessUtils.RestartApplication();
    }
}
