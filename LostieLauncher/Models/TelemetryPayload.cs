using System.Text.Json.Serialization;

namespace LostieLauncher.Models;

public class TelemetryPayload
{
    [JsonPropertyName("gameId")]
    public string GameId { get; init; } = string.Empty;

    [JsonPropertyName("gameVersion")]
    public string GameVersion { get; init; } = string.Empty;

    [JsonPropertyName("launcherVersion")]
    public string LauncherVersion { get; init; } = string.Empty;

    [JsonPropertyName("os")]
    public string Os { get; init; } = string.Empty;

    [JsonPropertyName("cpuName")]
    public string CpuName { get; init; } = string.Empty;

    [JsonPropertyName("cpuCores")]
    public int CpuCores { get; init; }

    [JsonPropertyName("gpuName")]
    public string GpuName { get; init; } = string.Empty;

    [JsonPropertyName("ramGb")]
    public int RamGb { get; init; }
}
