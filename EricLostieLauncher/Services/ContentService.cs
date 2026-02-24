using System.Net.Http;
using System.Text.Json;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Services;

public interface IContentService
{
    Task<List<GameInfo>> GetGamesAsync();
}

public class ContentService(IHttpClientFactory httpClientFactory, ContentOptions contentOptions) : IContentService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly ContentOptions _contentOptions = contentOptions;
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
}
