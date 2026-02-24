namespace EricLostieLauncher.Models;

public class InstalledGameInfo
{
    public string Nombre { get; init; } = string.Empty;
    public string InstalledVersion { get; init; } = string.Empty;
    public bool HasUpdate { get; init; }
    public string UpdateVersion { get; init; } = string.Empty;
}
