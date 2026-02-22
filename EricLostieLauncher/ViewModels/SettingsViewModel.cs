using System.IO;
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
    private AppLanguage _language = AppLanguage.Esp;

    [ObservableProperty]
    private IStrings _strings = new Esp();

    [ObservableProperty]
    private AppTheme _theme = AppTheme.Volcarona;

    [ObservableProperty]
    private bool _startWithWindows;

    [ObservableProperty]
    private bool _startMinimized;

    [ObservableProperty]
    private bool _autoUpdate = true;

    [ObservableProperty]
    private string _downloadDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "EricLostie", "Games");

    public static AppLanguage[] LanguageOptions { get; } = Enum.GetValues<AppLanguage>();
    public static AppTheme[] ThemeOptions { get; } = Enum.GetValues<AppTheme>();

    private ResourceDictionary? _activeThemeDict;

    private readonly ISettingsService _settingsService;
    private readonly IWindowsStartupService _windowsStartupService;
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

        if (_activeThemeDict == null) ApplyTheme(_theme);

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
        _isLoading = false;
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
            DownloadDirectory = DownloadDirectory
        });
    }

    partial void OnLanguageChanged(AppLanguage value)
    {
        Strings = value switch
        {
            AppLanguage.Eng => new Eng(),
            _ => new Esp()
        };
        SaveSettings();
    }

    partial void OnThemeChanged(AppTheme value)
    {
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
        SaveSettings();
    }
    partial void OnStartMinimizedChanged(bool value) => SaveSettings();
    partial void OnAutoUpdateChanged(bool value) => SaveSettings();
    partial void OnDownloadDirectoryChanged(string value) => SaveSettings();

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

    [RelayCommand]
    private void BrowseDownloadDirectory()
    {
        var dialog = new OpenFolderDialog();

        if (!string.IsNullOrEmpty(DownloadDirectory)) dialog.InitialDirectory = DownloadDirectory;

        if (dialog.ShowDialog() == true) DownloadDirectory = dialog.FolderName;
    }

    [RelayCommand]
    private void CheckForUpdates()
    {
        var result = CustomMessageBox.Show(Strings.CheckForUpdatesTitle, Strings.CheckForUpdatesMessage, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Update);

        if (result == true) ProcessUtils.RestartApplication();
    }
}
