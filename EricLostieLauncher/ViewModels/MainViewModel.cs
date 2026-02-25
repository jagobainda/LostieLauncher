using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace EricLostieLauncher.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly GlobalViewModel _globalViewModel;
    private readonly HomeViewModel _homeViewModel;
    private readonly GamesViewModel _gamesViewModel;
    private readonly LibraryViewModel _libraryViewModel;
    private readonly SettingsViewModel _settingsViewModel;

    [ObservableProperty]
    private string _currentTitle = string.Empty;

    [ObservableProperty]
    private ObservableObject _currentViewModel = null!;

    public MainViewModel(GlobalViewModel globalViewModel, HomeViewModel homeViewModel, GamesViewModel gamesViewModel, LibraryViewModel libraryViewModel, SettingsViewModel settingsViewModel)
    {
        _globalViewModel = globalViewModel;
        _homeViewModel = homeViewModel;
        _gamesViewModel = gamesViewModel;
        _libraryViewModel = libraryViewModel;
        _settingsViewModel = settingsViewModel;
        _currentViewModel = _gamesViewModel;
        _currentTitle = _settingsViewModel.Strings.TitleGames;

        _settingsViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(SettingsViewModel.Strings)) UpdateCurrentTitle();
        };

        _globalViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(GlobalViewModel.IsDownloading) or nameof(GlobalViewModel.IsRefreshing))
                RefreshDataCommand.NotifyCanExecuteChanged();
        };

        _libraryViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(LibraryViewModel.IsLoading))
                RefreshDataCommand.NotifyCanExecuteChanged();
        };

        _gamesViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(GamesViewModel.IsLoading))
                RefreshDataCommand.NotifyCanExecuteChanged();
        };
    }

    private void UpdateCurrentTitle()
    {
        CurrentTitle = CurrentViewModel switch
        {
            HomeViewModel => _settingsViewModel.Strings.TitleHome,
            GamesViewModel => _settingsViewModel.Strings.TitleGames,
            LibraryViewModel => _settingsViewModel.Strings.TitleLibrary,
            _ => _settingsViewModel.Strings.TitleSettings
        };
    }

    [RelayCommand]
    private void NavigateToHome()
    {
        CurrentViewModel = _homeViewModel;
        CurrentTitle = _settingsViewModel.Strings.TitleHome;
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

    [RelayCommand(CanExecute = nameof(CanRefreshData))]
    private async Task RefreshDataAsync()
    {
        _globalViewModel.IsRefreshing = true;
        try
        {
            await _libraryViewModel.RefreshAsync();
            await _gamesViewModel.RefreshAsync();
        }
        finally
        {
            _globalViewModel.IsRefreshing = false;
        }
    }

    private bool CanRefreshData() =>
        !_globalViewModel.IsDownloading &&
        !_globalViewModel.IsRefreshing &&
        !_libraryViewModel.IsLoading &&
        !_gamesViewModel.IsLoading;
}

