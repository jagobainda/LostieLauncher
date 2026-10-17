using System.Text.Json.Serialization;

namespace LostieLauncher.Models;

public class LocalGameInfo
{
    [JsonPropertyName("id")]
    public Guid Id { get; init; }

    [JsonPropertyName("nombre")]
    public string Nombre { get; init; } = string.Empty;

    [JsonPropertyName("version")]
    public string Version { get; init; } = string.Empty;

    [JsonPropertyName("tipo")]
    public string? Tipo { get; init; }
}
