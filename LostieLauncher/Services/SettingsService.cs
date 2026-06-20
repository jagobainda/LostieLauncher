using LostieLauncher.Models;
using System.IO;
using System.Text.Json;

namespace LostieLauncher.Services;

public interface ISettingsService
{
    public AppSettings Load();
    public void Save(AppSettings settings);
    public string GetGamesRootDirectory();
    public void EnsureGamesRootDirectoryExists();
}

public class SettingsService : ISettingsService, IDisposable
{
    private const string AppSubfolder = "LostieLauncher";
    private const string SettingsFileName = "launcher_settings.json";
    private static readonly string DefaultDownloadDirectory = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };
    private static readonly TimeSpan DefaultSaveDelay = TimeSpan.FromMilliseconds(500);

    private readonly string _settingsDirectory;
    private readonly string _settingsPath;
    private readonly string _legacySettingsPath;
    private readonly TimeSpan _saveDelay;

    private readonly object _gate = new();
    private readonly System.Threading.Timer _saveTimer;
    private AppSettings? _cachedSettings;
    private AppSettings? _pendingSave;
    private bool _disposed;

    public SettingsService()
        : this(
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), AppSubfolder),
            Path.Combine(AppContext.BaseDirectory, SettingsFileName),
            DefaultSaveDelay)
    {
    }

    internal SettingsService(string settingsDirectory, string legacySettingsPath, TimeSpan saveDelay)
    {
        _settingsDirectory = settingsDirectory;
        _settingsPath = Path.Combine(settingsDirectory, SettingsFileName);
        _legacySettingsPath = legacySettingsPath;
        _saveDelay = saveDelay;
        _saveTimer = new System.Threading.Timer(_ => FlushPendingSave(), null, Timeout.Infinite, Timeout.Infinite);
    }

    public AppSettings Load()
    {
        lock (_gate)
        {
            return _cachedSettings ??= LoadFromDisk();
        }
    }

    private AppSettings LoadFromDisk()
    {
        MigrateLegacySettings();
        Logs.DebugLogManager("Loading settings from disk.");
        if (!File.Exists(_settingsPath))
        {
            Logs.DebugLogManager("Settings file not found, using defaults.");
            return SanitizeSettings(new AppSettings());
        }

        try
        {
            var json = File.ReadAllText(_settingsPath);
            var settings = SanitizeSettings(JsonSerializer.Deserialize<AppSettings>(json, JsonOptions) ?? new AppSettings());
            Logs.DebugLogManager($"Settings loaded successfully (language={settings.Language}, theme={settings.Theme}).");
            return settings;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return SanitizeSettings(new AppSettings());
        }
    }

    internal static AppSettings SanitizeSettings(AppSettings settings)
    {
        if (!Enum.IsDefined(settings.Language))
        {
            Logs.ErrorLogManager($"Invalid Language value '{(int)settings.Language}' in settings; falling back to default '{AppLanguage.Esp}'.");
            settings.Language = AppLanguage.Esp;
        }

        if (!Enum.IsDefined(settings.Theme))
        {
            Logs.ErrorLogManager($"Invalid Theme value '{(int)settings.Theme}' in settings; falling back to default '{AppTheme.Volcarona}'.");
            settings.Theme = AppTheme.Volcarona;
        }

        settings.DownloadDirectory = NormalizeDownloadDirectory(settings.DownloadDirectory);

        return settings;
    }

    internal static string NormalizeDownloadDirectory(string? path)
    {
        if (string.IsNullOrWhiteSpace(path))
        {
            Logs.ErrorLogManager($"Empty DownloadDirectory in settings; falling back to default '{DefaultDownloadDirectory}'.");
            return DefaultDownloadDirectory;
        }

        try
        {
            if (!Path.IsPathFullyQualified(path))
            {
                Logs.ErrorLogManager($"Relative DownloadDirectory '{path}' in settings (would resolve against the working directory); falling back to default '{DefaultDownloadDirectory}'.");
                return DefaultDownloadDirectory;
            }

            return Path.GetFullPath(path);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return DefaultDownloadDirectory;
        }
    }

    public void Save(AppSettings settings)
    {
        ArgumentNullException.ThrowIfNull(settings);

        lock (_gate)
        {
            if (_disposed) return;

            var sanitized = SanitizeSettings(settings);
            _cachedSettings = sanitized;
            _pendingSave = sanitized;
            _saveTimer.Change(_saveDelay, Timeout.InfiniteTimeSpan);
        }
    }

    private void FlushPendingSave()
    {
        lock (_gate)
        {
            if (_pendingSave is null) return;
            WriteToDisk(_pendingSave);
            _pendingSave = null;
        }
    }

    protected virtual void WriteToDisk(AppSettings settings)
    {
        try
        {
            Directory.CreateDirectory(_settingsDirectory);
            var json = JsonSerializer.Serialize(settings, JsonOptions);
            File.WriteAllText(_settingsPath, json);
            Logs.DebugLogManager("Settings saved to disk.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private void MigrateLegacySettings()
    {
        if (File.Exists(_settingsPath) || !File.Exists(_legacySettingsPath)) return;
        try
        {
            Directory.CreateDirectory(_settingsDirectory);
            File.Move(_legacySettingsPath, _settingsPath);
            Logs.DebugLogManager("Legacy settings migrated to persistent path.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    public string GetGamesRootDirectory()
    {
        var settings = Load();
        return Path.Combine(settings.DownloadDirectory, AppSubfolder);
    }

    public void EnsureGamesRootDirectoryExists()
    {
        var path = GetGamesRootDirectory();
        Directory.CreateDirectory(path);
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    protected virtual void Dispose(bool disposing)
    {
        if (!disposing) return;

        lock (_gate)
        {
            if (_disposed) return;
            _disposed = true;
        }

        _saveTimer.Dispose();
        FlushPendingSave();
    }
}
