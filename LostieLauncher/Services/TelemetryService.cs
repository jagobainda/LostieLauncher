using LostieLauncher.Models;
using Microsoft.Win32;
using System.Net.Http;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;

namespace LostieLauncher.Services;

public interface ITelemetryService
{
    public void TrackGameLaunched(string gameId, string gameVersion);
    public Task<Dictionary<string, int>> GetDownloadCountsAsync();
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
    private readonly int _cpuCores = GetCpuCores();
    private readonly string _gpuName = GetGpuName();
    private readonly int _ramGb = GetRamGb();
    private readonly string _osVersion = GetOsVersion();
    private readonly string _launcherVersion = GetLauncherVersion();

    public void TrackGameLaunched(string gameId, string gameVersion)
    {
        if (string.IsNullOrWhiteSpace(_telemetryOptions.ApiKey))
        {
            Logs.DebugLogManager("Telemetry skipped: no API key configured.");
            return;
        }

        lock (_cooldownLock)
        {
            if (_lastSentTimes.TryGetValue(gameId, out var lastSent) && DateTime.UtcNow - lastSent < CooldownPeriod)
            {
                Logs.DebugLogManager($"Telemetry cooldown active for {gameId}, skipping.");
                return;
            }

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
        Logs.DebugLogManager($"Tracking game launched: {gameId} v{gameVersion}.");
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

            var result = stats.ByGame.ToDictionary(kv => kv.Key, kv => kv.Value.TotalEvents);
            Logs.DebugLogManager($"Download counts fetched: {result.Count} games.");
            return result;
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
            request.Headers.TryAddWithoutValidation("x-launcher-key", apiKey);

            using var response = await httpClient.SendAsync(request).ConfigureAwait(false);

            if (response.IsSuccessStatusCode)
            {
                Logs.DebugLogManager($"Telemetry sent for game: {payload.GameId}.");
            }
            else
            {
                var body = await response.Content.ReadAsStringAsync().ConfigureAwait(false);
                Logs.DebugLogManager($"[Telemetry] Error {(int)response.StatusCode}: {body}");
            }
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private static int GetRamGb()
    {
        try
        {
            var status = new MEMORYSTATUSEX { dwLength = (uint)Marshal.SizeOf<MEMORYSTATUSEX>() };
            if (!GlobalMemoryStatusEx(ref status)) return 2;

            var ramGb = status.ullTotalPhys / (1024.0 * 1024.0 * 1024.0);
            return Math.Clamp((int)Math.Round(ramGb), 2, 256);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return 2; }
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
            const string classKeyPath = @"SYSTEM\CurrentControlSet\Control\Class\{4d36e968-e325-11ce-bfc1-08002be10318}";
            using var classKey = Registry.LocalMachine.OpenSubKey(classKeyPath);
            if (classKey is null) return "Unknown";

            var driverDescriptions = new List<string?>();
            foreach (var subKeyName in classKey.GetSubKeyNames())
            {
                // Adapter instances are 4-digit numeric subkeys ("0000", "0001", …); skip "Properties" and the like.
                if (subKeyName.Length != 4 || !subKeyName.All(char.IsAsciiDigit)) continue;

                using var adapterKey = classKey.OpenSubKey(subKeyName);
                driverDescriptions.Add(adapterKey?.GetValue("DriverDesc") as string);
            }

            var gpuName = FormatGpuNames(driverDescriptions);
            return gpuName.Length == 0 ? "Unknown" : gpuName;
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return "Unknown"; }
    }

    internal static string FormatGpuNames(IEnumerable<string?> driverDescriptions)
    {
        var names = driverDescriptions
            .Where(name => !string.IsNullOrWhiteSpace(name))
            .Select(name => name!.Trim())
            .Distinct(StringComparer.OrdinalIgnoreCase)
            .ToList();

        return names.Count == 0 ? string.Empty : Truncate(string.Join(" + ", names), 128);
    }

    private static string GetOsVersion()
    {
        try
        {
            var buildStr = (string?)Registry.GetValue(@"HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion", "CurrentBuildNumber", null);

            if (int.TryParse(buildStr, out var build)) return build >= 22000 ? "Windows 11" : "Windows 10";
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
        return "Unknown";
    }

    private static int GetCpuCores()
    {
        try
        {
            using var key = Registry.LocalMachine.OpenSubKey(@"HARDWARE\DESCRIPTION\System\CentralProcessor");
            var count = key?.GetSubKeyNames().Length ?? Environment.ProcessorCount;
            return Math.Clamp(count, 1, 128);
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); return Math.Clamp(Environment.ProcessorCount, 1, 128); }
    }

    private static string GetLauncherVersion()
    {
        var v = Assembly.GetExecutingAssembly().GetName().Version;
        return v is null ? "0.0.0" : $"{v.Major}.{v.Minor}.{v.Build}";
    }

    private static string Truncate(string value, int maxLength) => value.Length <= maxLength ? value : value[..maxLength];

    internal static string NormalizeVersion(string version)
    {
        var parsed = VersionUtils.ParseBaseVersion(version);

        return parsed is null
            ? "0.0.0"
            : $"{parsed.Major}.{parsed.Minor}.{Math.Max(parsed.Build, 0)}";
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool GlobalMemoryStatusEx(ref MEMORYSTATUSEX lpBuffer);

    [StructLayout(LayoutKind.Sequential)]
    private struct MEMORYSTATUSEX
    {
        public uint dwLength;
        public uint dwMemoryLoad;
        public ulong ullTotalPhys;
        public ulong ullAvailPhys;
        public ulong ullTotalPageFile;
        public ulong ullAvailPageFile;
        public ulong ullTotalVirtual;
        public ulong ullAvailVirtual;
        public ulong ullAvailExtendedVirtual;
    }
}
