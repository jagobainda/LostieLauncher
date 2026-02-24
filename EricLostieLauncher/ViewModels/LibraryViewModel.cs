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

    [ObservableProperty]
    private ObservableCollection<GameInfo> _games = [];

    [ObservableProperty]
    private bool _isLoading;

    public LibraryViewModel(ITelemetryService telemetryService, IContentService contentService)
    {
        _telemetryService = telemetryService;
        _contentService = contentService;
        _ = LoadGamesAsync();
    }

    private async Task LoadGamesAsync()
    {
        IsLoading = true;
        var result = await _contentService.GetGamesAsync();
        Games = new ObservableCollection<GameInfo>(result);
        IsLoading = false;
    }

    [RelayCommand]
    private void StartDownload(GameDownloadArgs args)
    {
        _telemetryService.TrackDownloadStarted(args.GameId, args.Version);
    }
}
