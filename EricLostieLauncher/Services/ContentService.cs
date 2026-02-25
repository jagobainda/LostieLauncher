using System.IO;
using System.Net.Http;
using System.Text.Json;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Services;

public interface IContentService
{
    Task<List<GameInfo>> GetGamesAsync();
    Task<List<LocalGameInfo>> GetLocalGamesAsync();
    string GetGameDirectory(string gameName);
    Task RemoveGameRegistryAsync(string gameName);
}

public class ContentService(IHttpClientFactory httpClientFactory, ContentOptions contentOptions, ISettingsService settingsService) : IContentService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly ContentOptions _contentOptions = contentOptions;
    private readonly ISettingsService _settingsService = settingsService;
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };

    public async Task<List<GameInfo>> GetGamesAsync()
    {
        try
        {
            var client = _httpClientFactory.CreateClient("Content");
            using var response = await client.GetAsync(_contentOptions.Endpoint).ConfigureAwait(false);
            response.EnsureSuccessStatusCode();

            var json = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
            return JsonSerializer.Deserialize<List<GameInfo>>(json, JsonOptions) ?? [];
        }
        catch
        {
            return [];
        }
    }

    public async Task<List<LocalGameInfo>> GetLocalGamesAsync()
    {
        try
        {
            var gamesRoot = _settingsService.GetGamesRootDirectory();
            var path = Path.Combine(gamesRoot, "local_games.json");
            if (!File.Exists(path)) return [];

            var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
            return JsonSerializer.Deserialize<List<LocalGameInfo>>(json, JsonOptions) ?? [];
        }
        catch
        {
            return [];
        }
    }

    public string GetGameDirectory(string gameName)
    {
        var gamesRoot = _settingsService.GetGamesRootDirectory();
        return Path.Combine(gamesRoot, gameName);
    }

    public async Task RemoveGameRegistryAsync(string gameName)
    {
        var gamesRoot = _settingsService.GetGamesRootDirectory();
        var path = Path.Combine(gamesRoot, "local_games.json");
        if (!File.Exists(path)) return;

        try
        {
            var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
            var games = JsonSerializer.Deserialize<List<LocalGameInfo>>(json, JsonOptions) ?? [];
            var updated = games.Where(g => !string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase)).ToList();
            await File.WriteAllTextAsync(path, JsonSerializer.Serialize(updated, JsonOptions)).ConfigureAwait(false);
        }
        catch { }
    }
}
