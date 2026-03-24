using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.Models;

public partial class InstalledGameInfo : ObservableObject
{
    public string Nombre { get; init; } = string.Empty;
    public string InstalledVersion { get; init; } = string.Empty;
    public bool HasUpdate { get; init; }
    public string UpdateVersion { get; init; } = string.Empty;

    [ObservableProperty]
    public partial bool IsUpdating { get; set; }
}
