using System.Text.Json.Serialization;

namespace EricLostieLauncher.Models;

public class StatsResponse
{
    [JsonPropertyName("byGame")]
    public Dictionary<string, GameStatsEntry>? ByGame { get; init; }
}

public class GameStatsEntry
{
    [JsonPropertyName("totalEvents")]
    public int TotalEvents { get; init; }
}
