using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class GamesViewModel : ObservableObject
{
    private readonly IContentService _contentService;
    private readonly LibraryViewModel _libraryViewModel;

    [ObservableProperty]
    private ObservableCollection<InstalledGameInfo> _installedGames = [];

    [ObservableProperty]
    private bool _isLoading;

    public GamesViewModel(IContentService contentService, LibraryViewModel libraryViewModel)
    {
        _contentService = contentService;
        _libraryViewModel = libraryViewModel;
        _ = LoadInstalledGamesAsync();
    }

    private async Task LoadInstalledGamesAsync()
    {
        IsLoading = true;

        await _libraryViewModel.LibraryLoadedTask;

        var localGames = await _contentService.GetLocalGamesAsync();
        var remoteGames = _libraryViewModel.Games;

        var installed = localGames.Select(local =>
        {
            var remote = remoteGames.FirstOrDefault(r => string.Equals(r.Nombre, local.Nombre, StringComparison.OrdinalIgnoreCase));
            var hasUpdate = remote != null && remote.Version != local.Version;
            return new InstalledGameInfo
            {
                Nombre = local.Nombre,
                InstalledVersion = local.Version,
                HasUpdate = hasUpdate,
                UpdateVersion = hasUpdate ? remote!.Version : string.Empty
            };
        }).ToList();

        InstalledGames = new ObservableCollection<InstalledGameInfo>(installed);
        IsLoading = false;
    }
}
