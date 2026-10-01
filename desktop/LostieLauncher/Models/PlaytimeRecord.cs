using System.Text.Json.Serialization;

namespace LostieLauncher.Models;

public class PlaytimeRecord
{
    [JsonPropertyName("id")]
    public Guid Id { get; init; }

    [JsonPropertyName("playtimeMinutes")]
    public int PlaytimeMinutes { get; init; }
}
