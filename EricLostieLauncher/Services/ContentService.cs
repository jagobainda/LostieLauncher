using System.IO;
using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Serialization;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Services;

public interface IContentService
{
    Task<List<GameInfo>> GetGamesAsync();
    Task<List<LocalGameInfo>> GetLocalGamesAsync();
    Task<HomeContent> GetHomeContentAsync();
    string GetGameDirectory(string gameName);
    Task RemoveGameRegistryAsync(string gameName);
}

public class ContentService(IHttpClientFactory httpClientFactory, ContentOptions contentOptions, ISettingsService settingsService) : IContentService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly ContentOptions _contentOptions = contentOptions;
    private readonly ISettingsService _settingsService = settingsService;
    private HomeContentDto? _homeContentCache;
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter() }
    };

    public async Task<List<GameInfo>> GetGamesAsync()
    {
        try
        {
            Logs.DebugLogManager("Fetching games list from remote.");
            var client = _httpClientFactory.CreateClient("Content");
            using var response = await client.GetAsync(_contentOptions.Endpoint).ConfigureAwait(false);
            response.EnsureSuccessStatusCode();

            var json = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
            return JsonSerializer.Deserialize<List<GameInfo>>(json, JsonOptions) ?? [];
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return [];
        }
    }

    public async Task<HomeContent> GetHomeContentAsync()
    {
        try
        {
            if (_homeContentCache is null)
            {
                Logs.DebugLogManager("Fetching home content from remote.");
                var client = _httpClientFactory.CreateClient("Content");
                using var response = await client.GetAsync(_contentOptions.NotificationsEndpoint).ConfigureAwait(false);
                response.EnsureSuccessStatusCode();

                var json = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                _homeContentCache = JsonSerializer.Deserialize<HomeContentDto>(json, JsonOptions) ?? new HomeContentDto([], []);
            }

            var settings = _settingsService.Load();
            var langCode = GetLanguageCode(settings.Language);

            return new HomeContent
            {
                News = [.. _homeContentCache.News.Select(n => new NewsItem
                {
                    Id = n.Id,
                    Title = Resolve(n.Title, langCode),
                    Description = Resolve(n.Description, langCode),
                    Tag = n.Tag,
                    Date = n.Date,
                    ExpiresAt = n.ExpiresAt
                })],
                Notifications = [.. _homeContentCache.Notifications.Select(n => new NotificationItem
                {
                    Id = n.Id,
                    Title = Resolve(n.Title, langCode),
                    Message = Resolve(n.Message, langCode),
                    Type = n.Type,
                    Date = n.Date,
                    ExpiresAt = n.ExpiresAt
                })]
            };
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return new HomeContent();
        }
    }

    private static string GetLanguageCode(AppLanguage language) => language switch
    {
        AppLanguage.Esp => "es",
        AppLanguage.Eng => "en",
        AppLanguage.Cat => "ca",
        AppLanguage.Eus => "eu",
        AppLanguage.Gal => "gl",
        AppLanguage.Por => "pt",
        AppLanguage.Val => "val",
        _ => "es"
    };

    private static string Resolve(Dictionary<string, string> localized, string languageCode) =>
        localized.TryGetValue(languageCode, out var value) ? value :
        localized.TryGetValue("es", out var fallback) ? fallback :
        localized.Values.FirstOrDefault() ?? string.Empty;

    private record HomeContentDto(List<NewsItemDto> News, List<NotificationItemDto> Notifications);

    private record NewsItemDto(
        Guid Id,
        Dictionary<string, string> Title,
        Dictionary<string, string> Description,
        string Tag,
        DateTime Date,
        [property: JsonPropertyName("expires_at")] DateTime? ExpiresAt);

    private record NotificationItemDto(
        Guid Id,
        Dictionary<string, string> Title,
        Dictionary<string, string> Message,
        NotificationType Type,
        DateTime Date,
        [property: JsonPropertyName("expires_at")] DateTime? ExpiresAt);

    public async Task<List<LocalGameInfo>> GetLocalGamesAsync()
    {
        try
        {
            Logs.DebugLogManager("Reading local games registry.");
            var gamesRoot = _settingsService.GetGamesRootDirectory();
            var path = Path.Combine(gamesRoot, "local_games.json");
            if (!File.Exists(path)) return [];

            var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
            return JsonSerializer.Deserialize<List<LocalGameInfo>>(json, JsonOptions) ?? [];
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
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
            Logs.DebugLogManager($"Removing game from registry: {gameName}.");
            var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
            var games = JsonSerializer.Deserialize<List<LocalGameInfo>>(json, JsonOptions) ?? [];
            List<LocalGameInfo> updated = [.. games.Where(g => !string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase))];
            await File.WriteAllTextAsync(path, JsonSerializer.Serialize(updated, JsonOptions)).ConfigureAwait(false);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }
}
