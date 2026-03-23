using System.Globalization;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;
using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.Models;

public partial class GameInfo : ObservableObject
{
    [JsonPropertyName("nombre")]
    public string Nombre { get; init; } = string.Empty;

    [JsonPropertyName("pesoGB")]
    public double PesoGB { get; init; }

    [JsonPropertyName("version")]
    public string Version { get; init; } = string.Empty;

    [JsonPropertyName("descripcion")]
    public string Descripcion { get; init; } = string.Empty;

    [JsonPropertyName("url")]
    public string Url { get; init; } = string.Empty;

    [JsonPropertyName("rutaRelativa")]
    public string RutaRelativa { get; init; } = string.Empty;

    [JsonIgnore]
    public string GameId => SlugRegex().Replace(Nombre.ToLowerInvariant(), "-");

    [JsonIgnore]
    public string PesoFormateado => PesoGB >= 1 ? $"{PesoGB.ToString("0.#", CultureInfo.InvariantCulture)} GB" : $"{(PesoGB * 1024).ToString("0", CultureInfo.InvariantCulture)} MB";

    [JsonIgnore]
    public int TotalDownloads { get; set; }

    [ObservableProperty]
    [property: JsonIgnore]
    private GameDownloadStatus _downloadStatus = GameDownloadStatus.Available;

    [ObservableProperty]
    [property: JsonIgnore]
    private double _downloadProgressValue;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(DownloadSpeedText))]
    [property: JsonIgnore]
    private double _downloadSpeedBytesPerSec;

    [ObservableProperty]
    [property: JsonIgnore]
    private string _downloadRemainingText = string.Empty;

    [JsonIgnore]
    public string DownloadSpeedText => DownloadSpeedBytesPerSec switch
    {
        > 1_048_576 => $"{DownloadSpeedBytesPerSec / 1_048_576.0:0.0} MB/s",
        > 1024 => $"{DownloadSpeedBytesPerSec / 1024.0:0.0} KB/s",
        _ => "0 KB/s"
    };

    [GeneratedRegex(@"[^a-z0-9]+")]
    private static partial Regex SlugRegex();
}
