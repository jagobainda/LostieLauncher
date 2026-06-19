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

public class SettingsService : ISettingsService
{
    private const string AppSubfolder = "LostieLauncher";
    private static readonly string SettingsDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), AppSubfolder);
    private static readonly string SettingsPath = Path.Combine(SettingsDirectory, "launcher_settings.json");
    private static readonly string LegacySettingsPath = Path.Combine(AppContext.BaseDirectory, "launcher_settings.json");
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public AppSettings Load()
    {
        MigrateLegacySettings();
        Logs.DebugLogManager("Loading settings from disk.");
        if (!File.Exists(SettingsPath))
        {
            Logs.DebugLogManager("Settings file not found, using defaults.");
            return new AppSettings();
        }

        try
        {
            var json = File.ReadAllText(SettingsPath);
            var settings = SanitizeSettings(JsonSerializer.Deserialize<AppSettings>(json, JsonOptions) ?? new AppSettings());
            Logs.DebugLogManager($"Settings loaded successfully (language={settings.Language}, theme={settings.Theme}).");
            return settings;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return new AppSettings();
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

        return settings;
    }

    public void Save(AppSettings settings)
    {
        try
        {
            Directory.CreateDirectory(SettingsDirectory);
            var json = JsonSerializer.Serialize(settings, JsonOptions);
            File.WriteAllText(SettingsPath, json);
            Logs.DebugLogManager("Settings saved to disk.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private static void MigrateLegacySettings()
    {
        if (File.Exists(SettingsPath) || !File.Exists(LegacySettingsPath)) return;
        try
        {
            Directory.CreateDirectory(SettingsDirectory);
            File.Move(LegacySettingsPath, SettingsPath);
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
}
