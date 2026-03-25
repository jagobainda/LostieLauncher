using System.Text.Json.Serialization;

namespace EricLostieLauncher.Models;

public class PlaytimeRecord
{
    [JsonPropertyName("id")]
    public Guid Id { get; init; }

    [JsonPropertyName("playtimeMinutes")]
    public int PlaytimeMinutes { get; init; }
}
