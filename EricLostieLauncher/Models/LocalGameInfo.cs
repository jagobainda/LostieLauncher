using System.Text.Json.Serialization;

namespace EricLostieLauncher.Models;

public class LocalGameInfo
{
    [JsonPropertyName("nombre")]
    public string Nombre { get; init; } = string.Empty;

    [JsonPropertyName("version")]
    public string Version { get; init; } = string.Empty;
}
