using System.Net.Http;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using EricLostieLauncher.Models;
using Microsoft.Win32;

namespace EricLostieLauncher.Services;

public interface ITelemetryService
{
    void TrackDownloadStarted(string gameId, string gameVersion);
    Task<Dictionary<string, int>> GetDownloadCountsAsync();
}

public class TelemetryService(IHttpClientFactory httpClientFactory, TelemetryOptions telemetryOptions) : ITelemetryService
{
    private readonly IHttpClientFactory _httpClientFactory = httpClientFactory;
    private readonly TelemetryOptions _telemetryOptions = telemetryOptions;
    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };

    private static readonly TimeSpan CooldownPeriod = TimeSpan.FromMinutes(5);
    private readonly Dictionary<string, DateTime> _lastSentTimes = [];
    private readonly Lock _cooldownLock = new();

    private readonly string _cpuName = GetCpuName();
    private readonly int _cpuCores = Environment.ProcessorCount;
    private readonly string _gpuName = GetGpuName();
    private readonly int _ramGb = GetRamGb();
    private readonly string _osVersion = GetOsVersion();
    private readonly string _launcherVersion = GetLauncherVersion();

    public void TrackDownloadStarted(string gameId, string gameVersion)
    {
        if (string.IsNullOrWhiteSpace(_telemetryOptions.ApiKey)) return;

        lock (_cooldownLock)
        {
            if (_lastSentTimes.TryGetValue(gameId, out var lastSent) && DateTime.UtcNow - lastSent < CooldownPeriod) return;

            _lastSentTimes[gameId] = DateTime.UtcNow;
        }

        var payload = new TelemetryPayload
        {
            GameId = Truncate(gameId, 64),
            GameVersion = NormalizeVersion(gameVersion),
            LauncherVersion = _launcherVersion,
            Os = _osVersion,
            CpuName = _cpuName,
            CpuCores = _cpuCores,
            GpuName = _gpuName,
            RamGb = _ramGb
        };
        Logs.DebugLogManager($"Tracking download started: {gameId} v{gameVersion}.");
        _ = SendAsync(_httpClientFactory.CreateClient("Telemetry"), payload, $"{_telemetryOptions.Endpoint}telemetry", _telemetryOptions.ApiKey);
    }

    public async Task<Dictionary<string, int>> GetDownloadCountsAsync()
    {
        try
        {
            Logs.DebugLogManager("Fetching download counts from telemetry service.");
            var client = _httpClientFactory.CreateClient("Telemetry");
            using var response = await client.GetAsync($"{_telemetryOptions.Endpoint}stats").ConfigureAwait(false);
            response.EnsureSuccessStatusCode();

            var json = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
            var stats = JsonSerializer.Deserialize<StatsResponse>(json, JsonOptions);

            if (stats?.ByGame is null) return [];

            return stats.ByGame.ToDictionary(kv => kv.Key, kv => kv.Value.TotalEvents);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return [];
        }
    }

    private static async Task SendAsync(HttpClient httpClient, TelemetryPayload payload, string endpoint, string apiKey)
    {
        try
        {
            var json = JsonSerializer.Serialize(payload, JsonOptions);
            using var request = new HttpRequestMessage(HttpMethod.Post, endpoint);
            request.Content = new StringContent(json, Encoding.UTF8, "application/json");
            request.Headers.TryAddWithoutValidation("X-Launcher-Key", apiKey);
            await httpClient.SendAsync(request).ConfigureAwait(false);
            Logs.DebugLogManager($"Telemetry sent for game: {payload.GameId}.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private static string GetCpuName()
    {
        try
        {
            return ((string?)Registry.GetValue(@"HKEY_LOCAL_MACHINE\HARDWARE\DESCRIPTION\System\CentralProcessor\0", "ProcessorNameString", null))?.Trim() ?? "Unknown";
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return "Unknown"; }
    }

    private static string GetGpuName()
    {
        try
        {
            const string keyPath = @"HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}\0000";
            return ((string?)Registry.GetValue(keyPath, "DriverDesc", null))?.Trim() ?? "Unknown";
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return "Unknown"; }
    }

    private static int GetRamGb()
    {
        try
        {
            var status = new MEMORYSTATUSEX { dwLength = (uint)Marshal.SizeOf<MEMORYSTATUSEX>() };
            if (!GlobalMemoryStatusEx(ref status)) return 0;

            double ramGb = status.ullTotalPhys / (1024.0 * 1024.0 * 1024.0);
            return (int)Math.Round(ramGb);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return 0; }
    }

    private static string GetOsVersion()
    {
        try
        {
            var buildStr = (string?)Registry.GetValue(@"HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion", "CurrentBuildNumber", null);
            
            if (int.TryParse(buildStr, out int build)) return build >= 22000 ? "Windows 11" : "Windows 10";
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
        return "Windows";
    }

    private static string GetLauncherVersion()
    {
        var v = Assembly.GetExecutingAssembly().GetName().Version;
        return v is null ? "0.0.0" : $"{v.Major}.{v.Minor}.{v.Build}";
    }

    private static string Truncate(string value, int maxLength) => value.Length <= maxLength ? value : value[..maxLength];

    private static string NormalizeVersion(string version)
    {
        if (string.IsNullOrWhiteSpace(version)) return "0.0.0";

        var parts = version.Split('.', 4);

        return parts.Length switch
        {
            >= 3 => $"{parts[0]}.{parts[1]}.{parts[2]}",
            2    => $"{parts[0]}.{parts[1]}.0",
            _    => $"{parts[0]}.0.0"
        };
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX lpBuffer);

    [StructLayout(LayoutKind.Sequential)]
    private struct MEMORYSTATUSEX
    {
        public uint  dwLength;
        public uint  dwMemoryLoad;
        public ulong ullTotalPhys;
        public ulong ullAvailPhys;
        public ulong ullTotalPageFile;
        public ulong ullAvailPageFile;
        public ulong ullTotalVirtual;
        public ulong ullAvailVirtual;
        public ulong ullAvailExtendedVirtual;
    }
}