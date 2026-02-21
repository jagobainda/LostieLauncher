using System.IO;
using System.Windows;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Content;
using EricLostieLauncher.Models;
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
    private string _downloadDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "EricLostie", "Games");

    public static AppLanguage[] LanguageOptions { get; } = Enum.GetValues<AppLanguage>();
    public static AppTheme[] ThemeOptions { get; } = Enum.GetValues<AppTheme>();

    private ResourceDictionary? _activeThemeDict;

    public SettingsViewModel()
    {
        Instance = this;
        _activeThemeDict = Application.Current.Resources.MergedDictionaries
            .FirstOrDefault(d => d.Source != null &&
                (d.Source.OriginalString.Contains("/Themes/") ||
                 d.Source.OriginalString.Contains("Themes/")));

        if (_activeThemeDict == null) ApplyTheme(_theme);
    }

    partial void OnLanguageChanged(AppLanguage value)
    {
        Strings = value switch
        {
            AppLanguage.Eng => new Eng(),
            _ => new Esp()
        };
    }

    partial void OnThemeChanged(AppTheme value) => ApplyTheme(value);

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
        if (!string.IsNullOrEmpty(DownloadDirectory))
            dialog.InitialDirectory = DownloadDirectory;

        if (dialog.ShowDialog() == true)
            DownloadDirectory = dialog.FolderName;
    }
}
