using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.Models;

public partial class InstalledGameInfo : ObservableObject
{
    public Guid Id { get; init; }
    public string Nombre { get; init; } = string.Empty;
    public string InstalledVersion { get; init; } = string.Empty;
    public bool HasUpdate { get; init; }
    public string UpdateVersion { get; init; } = string.Empty;
    public string Logo { get; init; } = string.Empty;
    public string? LogoUrl => string.IsNullOrEmpty(Logo) ? null : $"https://ericlostie-launcher.jagoba.dev{Logo}";

    [ObservableProperty]
    public partial bool IsUpdating { get; set; }

    [ObservableProperty]
    public partial int PlaytimeMinutes { get; set; }

    partial void OnPlaytimeMinutesChanged(int value) => OnPropertyChanged(nameof(PlaytimeText));

    public string PlaytimeText
    {
        get
        {
            if (PlaytimeMinutes <= 0) return string.Empty;
            if (PlaytimeMinutes < 60) return "< 1 h";
            var h = PlaytimeMinutes / 60;
            var m = PlaytimeMinutes % 60;
            return m > 0 ? $"{h} h {m} min" : $"{h} h";
        }
    }
}
