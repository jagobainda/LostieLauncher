using LostieLauncher.Models;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text.Json;

namespace LostieLauncher.Services;

public interface IDownloadService
{
    DownloadState State { get; }

    Task<SpecialVersionConfig?> FetchSpecialVersionConfigAsync(string key, CancellationToken ct = default);

    Task<DownloadResult> DownloadAsync(string url, string destinationPath, IProgress<DownloadProgressInfo>? progress = null, CancellationToken ct = default);
}

public class DownloadService(IHttpClientFactory httpClientFactory, DownloadOptions downloadOptions) : IDownloadService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly DownloadOptions _downloadOptions = downloadOptions;

    private const int BufferSize = 64 * 1024;
    private const int MaxRetries = 2;

    public DownloadState State { get; private set; } = DownloadState.Idle;

    public async Task<SpecialVersionConfig?> FetchSpecialVersionConfigAsync(string key, CancellationToken ct = default)
    {
        try
        {
            var client = _httpClientFactory.CreateClient("Content");
            var configUrl = $"{_downloadOptions.BaseUrl}/{key}/game.config";
            using var response = await client.GetAsync(configUrl, ct).ConfigureAwait(false);

            if (response.StatusCode == HttpStatusCode.NotFound)
            {
                Logs.InfoLogManager($"Special version config not found for key: {key}");
                return null;
            }

            response.EnsureSuccessStatusCode();

            var content = await response.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            var config = SpecialVersionConfig.Parse(content);

            if (config is null)
            {
                Logs.InfoLogManager($"Failed to parse special version config for key: {key}");
                return null;
            }

            Logs.InfoLogManager($"Special version config loaded: {config.Tipo} v{config.Version}");
            return config;
        }
        catch (OperationCanceledException)
        {
            return null;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return null;
        }
    }

    public async Task<DownloadResult> DownloadAsync(string url, string destinationPath, IProgress<DownloadProgressInfo>? progress = null, CancellationToken ct = default)
    {
        var partPath = destinationPath + ".part";

        try
        {
            var dir = Path.GetDirectoryName(destinationPath);
            if (!string.IsNullOrEmpty(dir)) Directory.CreateDirectory(dir);

            State = DownloadState.Downloading;

            for (int attempt = 0; attempt <= MaxRetries; attempt++)
            {
                try
                {
                    await DownloadCoreAsync(url, partPath, destinationPath, progress, ct).ConfigureAwait(false);
                    State = DownloadState.Completed;
                    return DownloadResult.Succeeded();
                }
                catch (OperationCanceledException)
                {
                    State = DownloadState.Paused;
                    Logs.InfoLogManager("Download paused by user.");
                    return DownloadResult.Cancelled();
                }
                catch (HttpRequestException ex) when (attempt < MaxRetries && !ct.IsCancellationRequested)
                {
                    Logs.InfoLogManager($"Download attempt {attempt + 1}/{MaxRetries + 1} failed ({ex.Message}), retrying...");
                    await Task.Delay(TimeSpan.FromSeconds(attempt + 1), CancellationToken.None).ConfigureAwait(false);
                }
                catch (IOException ex) when (attempt < MaxRetries && !ct.IsCancellationRequested)
                {
                    Logs.InfoLogManager($"IO error on attempt {attempt + 1}/{MaxRetries + 1} ({ex.Message}), retrying...");
                    await Task.Delay(TimeSpan.FromSeconds(attempt + 1), CancellationToken.None).ConfigureAwait(false);
                }
            }

            State = DownloadState.Failed;
            Logs.ErrorLogManager($"Download failed after {MaxRetries + 1} attempts, giving up.");
            return DownloadResult.Failed("Download failed after maximum retries.");
        }
        catch (Exception ex)
        {
            State = DownloadState.Failed;
            Logs.ErrorLogManager(ex);
            return DownloadResult.Failed(ex.Message);
        }
    }

    private static void CleanupPartFile(string partPath)
    {
        try { if (File.Exists(partPath)) File.Delete(partPath); }
        catch { Logs.ErrorLogManager("Something went wrong while trying to delete part files"); }
    }

    private async Task DownloadCoreAsync(string url, string partPath, string finalPath, IProgress<DownloadProgressInfo>? progress, CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient("Download");
        long existingBytes = 0;

        if (File.Exists(partPath))
        {
            existingBytes = new FileInfo(partPath).Length;
        }

        using var request = new HttpRequestMessage(HttpMethod.Get, url);

        if (existingBytes > 0)
        {
            request.Headers.Range = new RangeHeaderValue(existingBytes, null);
            Logs.DebugLogManager($"Resuming download from byte {existingBytes}.");
        }

        using var response = await client.SendAsync(request, HttpCompletionOption.ResponseHeadersRead, ct).ConfigureAwait(false);

        if (response.StatusCode == HttpStatusCode.RequestedRangeNotSatisfiable)
        {
            Logs.InfoLogManager("Server returned 416 — partial file is already complete.");
            File.Move(partPath, finalPath, overwrite: true);
            progress?.Report(new DownloadProgressInfo(100, 0));
            return;
        }

        if (response.StatusCode == HttpStatusCode.OK)
        {
            if (existingBytes > 0)
            {
                Logs.InfoLogManager("Server returned 200 instead of 206 — restarting download from scratch.");
                File.Delete(partPath);
                existingBytes = 0;
            }
        }
        else if (response.StatusCode == HttpStatusCode.PartialContent)
        {
            Logs.DebugLogManager("Server returned 206 Partial Content — resuming.");
        }
        else
        {
            response.EnsureSuccessStatusCode();
        }

        var contentLength = response.Content.Headers.ContentLength;
        var totalBytes = contentLength.HasValue ? contentLength.Value + existingBytes : -1;

        await using var contentStream = await response.Content.ReadAsStreamAsync(ct).ConfigureAwait(false);
        var fileMode = existingBytes > 0 ? FileMode.Append : FileMode.Create;

        await using (var fileStream = new FileStream(partPath, fileMode, FileAccess.Write, FileShare.None, BufferSize, useAsync: true))
        {
            var buffer = new byte[BufferSize];
            long totalRead = existingBytes;
            int bytesRead;
            var speedwatch = Stopwatch.StartNew();
            long lastSpeedBytes = existingBytes;
            double currentSpeed = 0;

            while ((bytesRead = await contentStream.ReadAsync(buffer.AsMemory(0, BufferSize), ct).ConfigureAwait(false)) > 0)
            {
                await fileStream.WriteAsync(buffer.AsMemory(0, bytesRead), ct).ConfigureAwait(false);
                totalRead += bytesRead;

                var elapsedMs = speedwatch.ElapsedMilliseconds;
                if (elapsedMs >= 500)
                {
                    var deltaBytes = totalRead - lastSpeedBytes;
                    currentSpeed = deltaBytes / (elapsedMs / 1000.0);
                    lastSpeedBytes = totalRead;
                    speedwatch.Restart();
                }

                if (totalBytes > 0)
                {
                    progress?.Report(new DownloadProgressInfo((double)totalRead / totalBytes * 100.0, currentSpeed, totalBytes, totalRead));
                }
            }

            await fileStream.FlushAsync(CancellationToken.None).ConfigureAwait(false);
        }

        File.Move(partPath, finalPath, overwrite: true);
        progress?.Report(new DownloadProgressInfo(100, 0));
    }
}
