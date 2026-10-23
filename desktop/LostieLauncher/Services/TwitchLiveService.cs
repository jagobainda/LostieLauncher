using LostieLauncher.Models;
using System.Net.Http;

namespace LostieLauncher.Services;

public interface ITwitchLiveService
{
    public Task<bool> IsLiveAsync(CancellationToken ct = default);
}

public sealed class TwitchLiveService(IHttpClientFactory httpClientFactory, TwitchLiveOptions options) : ITwitchLiveService
{
    public async Task<bool> IsLiveAsync(CancellationToken ct = default)
    {
        try
        {
            var client = httpClientFactory.CreateClient("TwitchPreview");
            using var request = new HttpRequestMessage(HttpMethod.Head, options.PreviewUrl);
            using var response = await client.SendAsync(request, ct).ConfigureAwait(false);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex) when (ex is HttpRequestException or OperationCanceledException)
        {
            Logs.InfoLogManager($"Twitch live check failed ({ex.GetType().Name}: {ex.Message}); hiding the live indicator.");
            return false;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }
}
