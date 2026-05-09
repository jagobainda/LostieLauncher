using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Text.Json.Serialization;
using LostieLauncher.Models;

namespace LostieLauncher.Services;

public interface IContentService
{
    Task<List<GameInfo>> GetGamesAsync();
    Task<List<LocalGameInfo>> GetLocalGamesAsync();
    Task<HomeContent> GetHomeContentAsync(bool forceRefresh = false);
    Task<bool> IsServerActionBlockedAsync(bool forceRefresh = false, CancellationToken ct = default);
    string GetGameDirectory(string gameName);
    Task RegisterGameAsync(Guid gameId, string gameName, string version, string? tipo = null);
    Task RemoveGameRegistryAsync(string gameName);
    Task AddPlaytimeAsync(Guid gameId, int minutes);
    Task<Dictionary<Guid, int>> GetAllPlaytimesAsync();
}

public class ContentService(IHttpClientFactory httpClientFactory, ContentOptions contentOptions, ISettingsService settingsService) : IContentService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly ContentOptions _contentOptions = contentOptions;
    private readonly ISettingsService _settingsService = settingsService;
    private HomeContentDto? _homeContentCache;
    private bool _serverActionBlockedCache;
    private DateTime _serverActionBlockedCacheExpiresAtUtc;
    private static readonly TimeSpan ServerActionBlockedCacheDuration = TimeSpan.FromSeconds(30);
    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        Converters = { new JsonStringEnumConverter() }
    };

    public async Task<List<GameInfo>> GetGamesAsync()
    {
        try
        {
            if (await IsServerActionBlockedAsync().ConfigureAwait(false))
            {
                Logs.InfoLogManager("Games list request skipped because server actions are blocked by the maintenance flag.");
                return [];
            }

            Logs.DebugLogManager("Fetching games list from remote.");
            var client = _httpClientFactory.CreateClient("Content");
            using var response = await client.GetAsync(_contentOptions.ContentEndpoint).ConfigureAwait(false);
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

    public async Task<HomeContent> GetHomeContentAsync(bool forceRefresh = false)
    {
        try
        {
            if (forceRefresh) _homeContentCache = null;

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

            var now = DateTime.Now;

            return new HomeContent
            {
                News = [.. _homeContentCache.News
                    .Where(n => n.ExpiresAt is null || n.ExpiresAt > now)
                    .Select(n => new NewsItem
                    {
                        Id = n.Id,
                        Title = Resolve(n.Title, langCode),
                        Description = Resolve(n.Description, langCode),
                        Tag = n.Tag,
                        Date = n.Date,
                        ExpiresAt = n.ExpiresAt
                    })],
                Notifications = [.. _homeContentCache.Notifications
                    .Where(n => n.ExpiresAt is null || n.ExpiresAt > now)
                    .Select(n => new NotificationItem
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

    public async Task<bool> IsServerActionBlockedAsync(bool forceRefresh = false, CancellationToken ct = default)
    {
        if (!forceRefresh && DateTime.UtcNow < _serverActionBlockedCacheExpiresAtUtc)
            return _serverActionBlockedCache;

        try
        {
            var blocked = await CheckServerActionFlagAsync(ct).ConfigureAwait(false);
            _serverActionBlockedCache = blocked;
            _serverActionBlockedCacheExpiresAtUtc = DateTime.UtcNow.Add(ServerActionBlockedCacheDuration);
            return blocked;
        }
        catch (OperationCanceledException)
        {
            Logs.InfoLogManager("Maintenance flag check timed out or was cancelled; assuming not blocked.");
            _serverActionBlockedCache = false;
            _serverActionBlockedCacheExpiresAtUtc = DateTime.UtcNow.Add(ServerActionBlockedCacheDuration);
            return false;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            _serverActionBlockedCache = false;
            _serverActionBlockedCacheExpiresAtUtc = DateTime.UtcNow.Add(ServerActionBlockedCacheDuration);
            return false;
        }
    }

    private async Task<bool> CheckServerActionFlagAsync(CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient("SecurityFlag");
        using var headRequest = new HttpRequestMessage(HttpMethod.Head, _contentOptions.FlagEndpoint);
        using var headResponse = await client.SendAsync(headRequest, HttpCompletionOption.ResponseHeadersRead, ct).ConfigureAwait(false);

        if (headResponse.StatusCode == HttpStatusCode.MethodNotAllowed)
        {
            using var getRequest = new HttpRequestMessage(HttpMethod.Get, _contentOptions.FlagEndpoint);
            using var getResponse = await client.SendAsync(getRequest, HttpCompletionOption.ResponseHeadersRead, ct).ConfigureAwait(false);
            return IsBlockingFlagStatus(getResponse.StatusCode);
        }

        return IsBlockingFlagStatus(headResponse.StatusCode);
    }

    private static bool IsBlockingFlagStatus(HttpStatusCode statusCode)
    {
        if ((int)statusCode is >= 200 and <= 299)
        {
            Logs.InfoLogManager("Maintenance flag detected. Server-backed actions are temporarily blocked.");
            return true;
        }

        return false;
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
        AppLanguage.Fra => "fr",
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
        var resolved = Path.GetFullPath(Path.Combine(gamesRoot, gameName));
        var canonicalRoot = Path.GetFullPath(gamesRoot) + Path.DirectorySeparatorChar;

        if (!resolved.StartsWith(canonicalRoot, StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException($"Invalid game directory: '{gameName}' escapes the games root.");

        return resolved;
    }

    public async Task RegisterGameAsync(Guid gameId, string gameName, string version, string? tipo = null)
    {
        try
        {
            Logs.DebugLogManager($"Registering game in local registry: {gameName} v{version}{(tipo is not null ? $" ({tipo})" : "")}.");
            var gamesRoot = _settingsService.GetGamesRootDirectory();
            Directory.CreateDirectory(gamesRoot);
            var path = Path.Combine(gamesRoot, "local_games.json");

            List<LocalGameInfo> games = [];
            if (File.Exists(path))
            {
                var existingJson = await File.ReadAllTextAsync(path).ConfigureAwait(false);
                games = JsonSerializer.Deserialize<List<LocalGameInfo>>(existingJson, JsonOptions) ?? [];
            }

            games.RemoveAll(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            games.Add(new LocalGameInfo { Id = gameId, Nombre = gameName, Version = version, Tipo = tipo });

            await File.WriteAllTextAsync(path, JsonSerializer.Serialize(games, JsonOptions)).ConfigureAwait(false);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
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

    public async Task AddPlaytimeAsync(Guid gameId, int minutes)
    {
        if (gameId == Guid.Empty) return;

        var gamesRoot = _settingsService.GetGamesRootDirectory();
        var path = Path.Combine(gamesRoot, "playtime.json");

        try
        {
            Logs.DebugLogManager($"Adding {minutes} playtime minutes for game id: {gameId}.");
            List<PlaytimeRecord> records = [];
            if (File.Exists(path))
            {
                var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
                records = JsonSerializer.Deserialize<List<PlaytimeRecord>>(json, JsonOptions) ?? [];
            }

            var existing = records.FirstOrDefault(r => r.Id == gameId);
            if (existing is not null)
                records = [.. records.Select(r => r.Id == gameId
                    ? new PlaytimeRecord { Id = r.Id, PlaytimeMinutes = r.PlaytimeMinutes + minutes }
                    : r)];
            else
                records.Add(new PlaytimeRecord { Id = gameId, PlaytimeMinutes = minutes });

            Directory.CreateDirectory(gamesRoot);
            await File.WriteAllTextAsync(path, JsonSerializer.Serialize(records, JsonOptions)).ConfigureAwait(false);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    public async Task<Dictionary<Guid, int>> GetAllPlaytimesAsync()
    {
        var gamesRoot = _settingsService.GetGamesRootDirectory();
        var path = Path.Combine(gamesRoot, "playtime.json");
        if (!File.Exists(path)) return [];

        try
        {
            var json = await File.ReadAllTextAsync(path).ConfigureAwait(false);
            var records = JsonSerializer.Deserialize<List<PlaytimeRecord>>(json, JsonOptions) ?? [];
            return records
                .Where(r => r.Id != Guid.Empty)
                .ToDictionary(r => r.Id, r => r.PlaytimeMinutes);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return [];
        }
    }
}
