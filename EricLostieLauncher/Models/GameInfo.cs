using System.Globalization;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace EricLostieLauncher.Models;

public partial class GameInfo
{
    [JsonPropertyName("nombre")]
    public string Nombre { get; init; } = string.Empty;

    [JsonPropertyName("pesoGB")]
    public double PesoGB { get; init; }

    [JsonPropertyName("version")]
    public string Version { get; init; } = string.Empty;

    [JsonIgnore]
    public string GameId => SlugRegex().Replace(Nombre.ToLowerInvariant(), "-");

    [JsonIgnore]
    public string PesoFormateado => PesoGB >= 1 ? $"{PesoGB.ToString("0.#", CultureInfo.InvariantCulture)} GB" : $"{(PesoGB * 1024).ToString("0", CultureInfo.InvariantCulture)} MB";

    [GeneratedRegex(@"[^a-z0-9]+")]
    private static partial Regex SlugRegex();
}
