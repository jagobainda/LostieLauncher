using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;
using EricLostieLauncher.Views.Dialogs;

namespace EricLostieLauncher.ViewModels;

public partial class LibraryViewModel : ObservableObject
{
    private readonly ITelemetryService _telemetryService;
    private readonly IContentService _contentService;
    private readonly ISettingsService _settingsService;
    private readonly TaskCompletionSource _libraryLoadedTcs = new();

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    private ObservableCollection<GameInfo> _games = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    private bool _isLoading;

    public bool IsEmpty => !IsLoading && Games.Count == 0;
    public bool IsListVisible => !IsLoading && Games.Count > 0;

    public Task LibraryLoadedTask => _libraryLoadedTcs.Task;

    public LibraryViewModel(ITelemetryService telemetryService, IContentService contentService, ISettingsService settingsService)
    {
        _telemetryService = telemetryService;
        _contentService = contentService;
        _settingsService = settingsService;
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
        Logs.DebugLogManager($"Games library loaded: {result.Count} games.");
        IsLoading = false;
        _libraryLoadedTcs.TrySetResult();
    }

    [RelayCommand]
    private void StartDownload(GameDownloadArgs args)
    {
        var game = Games.FirstOrDefault(g => g.GameId == args.GameId);
        if (game is null) return;

        var downloadPath = _contentService.GetGameDirectory(game.Nombre);
        var strings = SettingsViewModel.Instance.Strings;

        var confirmed = DownloadConfirmDialog.Show(game, args, downloadPath, strings);
        if (confirmed is null) return;

        Logs.InfoLogManager($"Download started: {confirmed.GameId} v{confirmed.Version}{(confirmed.Key is not null ? " (with key)" : "")}.");
        _telemetryService.TrackDownloadStarted(confirmed.GameId, confirmed.Version);
    }
}
