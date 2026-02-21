using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace EricLostieLauncher.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly GamesViewModel _gamesViewModel;
    private readonly LibraryViewModel _libraryViewModel;
    private readonly SettingsViewModel _settingsViewModel;

    [ObservableProperty]
    private string _currentTitle = string.Empty;

    [ObservableProperty]
    private ObservableObject _currentViewModel = null!;

    public MainViewModel(GamesViewModel gamesViewModel, LibraryViewModel libraryViewModel, SettingsViewModel settingsViewModel)
    {
        _gamesViewModel = gamesViewModel;
        _libraryViewModel = libraryViewModel;
        _settingsViewModel = settingsViewModel;
        _currentViewModel = _gamesViewModel;
        _currentTitle = _settingsViewModel.Strings.TitleGames;

        _settingsViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(SettingsViewModel.Strings)) UpdateCurrentTitle();
        };
    }

    private void UpdateCurrentTitle()
    {
        CurrentTitle = CurrentViewModel switch
        {
            GamesViewModel => _settingsViewModel.Strings.TitleGames,
            LibraryViewModel => _settingsViewModel.Strings.TitleLibrary,
            _ => _settingsViewModel.Strings.TitleSettings
        };
    }

    [RelayCommand]
    private void NavigateToGames()
    {
        CurrentViewModel = _gamesViewModel;
        CurrentTitle = _settingsViewModel.Strings.TitleGames;
    }

    [RelayCommand]
    private void NavigateToLibrary()
    {
        CurrentViewModel = _libraryViewModel;
        CurrentTitle = _settingsViewModel.Strings.TitleLibrary;
    }

    [RelayCommand]
    private void NavigateToSettings()
    {
        CurrentViewModel = _settingsViewModel;
        CurrentTitle = _settingsViewModel.Strings.TitleSettings;
    }
}
