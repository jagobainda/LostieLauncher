using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class LibraryViewModel(ITelemetryService telemetryService) : ObservableObject
{
    private readonly ITelemetryService _telemetryService = telemetryService;

    [RelayCommand]
    private void StartDownload(GameDownloadArgs args)
    {
        _telemetryService.TrackDownloadStarted(args.GameId, args.Version);
    }
}
