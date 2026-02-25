using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class LibraryViewModel : ObservableObject
{
    private readonly ITelemetryService _telemetryService;
    private readonly IContentService _contentService;
    private readonly TaskCompletionSource _libraryLoadedTcs = new();

    [ObservableProperty]
    private ObservableCollection<GameInfo> _games = [];

    [ObservableProperty]
    private bool _isLoading;

    public Task LibraryLoadedTask => _libraryLoadedTcs.Task;

    public LibraryViewModel(ITelemetryService telemetryService, IContentService contentService)
    {
        _telemetryService = telemetryService;
        _contentService = contentService;
        _ = LoadGamesAsync();
    }

    public async Task RefreshAsync() => await LoadGamesAsync();

    private async Task LoadGamesAsync()
    {
        IsLoading = true;

        var result = await _contentService.GetGamesAsync();
        var localGames = await _contentService.GetLocalGamesAsync();
        var downloadCounts = await _telemetryService.GetDownloadCountsAsync();

        var installedLookup = localGames.ToDictionary(g => g.Nombre, StringComparer.OrdinalIgnoreCase);

        foreach (var game in result)
        {
            if (installedLookup.TryGetValue(game.Nombre, out var local))
            {
                game.DownloadStatus = game.Version == local.Version
                    ? GameDownloadStatus.Downloaded
                    : GameDownloadStatus.UpdateAvailable;
            }

            if (downloadCounts.TryGetValue(game.GameId, out var count))
            {
                game.TotalDownloads = count;
            }
        }

        Games = new ObservableCollection<GameInfo>(result);
        IsLoading = false;
        _libraryLoadedTcs.TrySetResult();
    }

    [RelayCommand]
    private void StartDownload(GameDownloadArgs args)
    {
        _telemetryService.TrackDownloadStarted(args.GameId, args.Version);
    }
}
