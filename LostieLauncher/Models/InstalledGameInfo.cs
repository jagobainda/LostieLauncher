using CommunityToolkit.Mvvm.ComponentModel;
using LostieLauncher.Core;
using LostieLauncher.Utils;

namespace LostieLauncher.Models;

public partial class InstalledGameInfo : ObservableObject
{
    public Guid Id { get; init; }
    public string Nombre { get; init; } = string.Empty;
    public string InstalledVersion { get; init; } = string.Empty;
    public bool HasUpdate { get; init; }
    public string UpdateVersion { get; init; } = string.Empty;
    public string Logo { get; init; } = string.Empty;
    public string? LogoUrl => string.IsNullOrEmpty(Logo) ? null : $"{Endpoints.CdnBaseUrl}{Logo}";
    public string? Tipo { get; init; }
    public bool IsSpecialVersion => !string.IsNullOrEmpty(Tipo);

    [ObservableProperty]
    public partial bool IsUpdating { get; set; }

    [ObservableProperty]
    public partial bool IsUninstalling { get; set; }

    [ObservableProperty]
    public partial bool HasHelpFolder { get; set; }

    [ObservableProperty]
    public partial int PlaytimeMinutes { get; set; }

    partial void OnPlaytimeMinutesChanged(int value) => OnPropertyChanged(nameof(PlaytimeText));

    public string PlaytimeText => PlaytimeFormatter.Format(PlaytimeMinutes);
}
