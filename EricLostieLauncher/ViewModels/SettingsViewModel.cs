using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Content;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.ViewModels;

public partial class SettingsViewModel : ObservableObject
{
    public static SettingsViewModel Instance { get; private set; } = null!;

    [ObservableProperty]
    private AppLanguage _language = AppLanguage.Esp;

    [ObservableProperty]
    private IStrings _strings = new Esp();

    public SettingsViewModel()
    {
        Instance = this;
    }

    partial void OnLanguageChanged(AppLanguage value)
    {
        Strings = value switch
        {
            AppLanguage.Eng => new Eng(),
            _ => new Esp()
        };
    }
}
