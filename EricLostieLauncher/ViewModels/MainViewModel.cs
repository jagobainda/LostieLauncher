using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace EricLostieLauncher.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly GamesViewModel _gamesViewModel;
    private readonly LibraryViewModel _libraryViewModel;
    private readonly SettingsViewModel _settingsViewModel;

    [ObservableProperty]
    private string _currentTitle = "Mis Juegos";

    [ObservableProperty]
    private ObservableObject _currentViewModel = null!;

    public MainViewModel(GamesViewModel gamesViewModel, LibraryViewModel libraryViewModel, SettingsViewModel settingsViewModel)
    {
        _gamesViewModel = gamesViewModel;
        _libraryViewModel = libraryViewModel;
        _settingsViewModel = settingsViewModel;
        _currentViewModel = _gamesViewModel;
    }

    [RelayCommand]
    private void NavigateToGames()
    {
        CurrentViewModel = _gamesViewModel;
        CurrentTitle = "Mis Juegos";
    }

    [RelayCommand]
    private void NavigateToLibrary()
    {
        CurrentViewModel = _libraryViewModel;
        CurrentTitle = "Biblioteca";
    }

    [RelayCommand]
    private void NavigateToSettings()
    {
        CurrentViewModel = _settingsViewModel;
        CurrentTitle = "Ajustes";
    }
}
