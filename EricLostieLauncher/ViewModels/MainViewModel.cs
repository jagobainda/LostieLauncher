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
    public partial string CurrentTitle { get; set; } = string.Empty;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsHomeActive))]
    [NotifyPropertyChangedFor(nameof(IsGamesActive))]
    [NotifyPropertyChangedFor(nameof(IsLibraryActive))]
    public partial ObservableObject CurrentViewModel { get; set; } = null!;

    public bool IsHomeActive => CurrentViewModel is HomeViewModel;
    public bool IsGamesActive => CurrentViewModel is GamesViewModel;
    public bool IsLibraryActive => CurrentViewModel is LibraryViewModel;

    public MainViewModel(GlobalViewModel globalViewModel, HomeViewModel homeViewModel, GamesViewModel gamesViewModel, LibraryViewModel libraryViewModel, SettingsViewModel settingsViewModel)
    {
        _globalViewModel = globalViewModel;
        _homeViewModel = homeViewModel;
        _gamesViewModel = gamesViewModel;
        _libraryViewModel = libraryViewModel;
        _settingsViewModel = settingsViewModel;
        CurrentViewModel = _homeViewModel;
        CurrentTitle = _settingsViewModel.Strings.TitleHome;

        _gamesViewModel.NavigateToLibraryRequested += () =>
        {
            CurrentViewModel = _libraryViewModel;
            CurrentTitle = _settingsViewModel.Strings.TitleLibrary;
        };

        _settingsViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(SettingsViewModel.Strings)) UpdateCurrentTitle();
            if (e.PropertyName == nameof(SettingsViewModel.DownloadDirectory)) _ = _libraryViewModel.RefreshAsync();
        };

        _globalViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(GlobalViewModel.IsDownloading) or nameof(GlobalViewModel.IsRefreshing)) RefreshDataCommand.NotifyCanExecuteChanged();
        };

        _libraryViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(LibraryViewModel.IsLoading)) RefreshDataCommand.NotifyCanExecuteChanged();
        };

        _gamesViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(GamesViewModel.IsLoading)) RefreshDataCommand.NotifyCanExecuteChanged();
        };

        _homeViewModel.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName == nameof(HomeViewModel.IsLoading)) RefreshDataCommand.NotifyCanExecuteChanged();
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
        Logs.DebugLogManager("Data refresh started.");
        _globalViewModel.IsRefreshing = true;
        try
        {
            await Task.WhenAll(_homeViewModel.RefreshAsync(), _libraryViewModel.RefreshAsync());
            await _gamesViewModel.RefreshAsync();
            Logs.DebugLogManager("Data refresh completed.");
        }
        finally
        {
            _globalViewModel.IsRefreshing = false;
        }
    }

    private bool CanRefreshData() =>
        !_globalViewModel.IsDownloading &&
        !_globalViewModel.IsRefreshing &&
        !_homeViewModel.IsLoading &&
        !_libraryViewModel.IsLoading &&
        !_gamesViewModel.IsLoading;
}

