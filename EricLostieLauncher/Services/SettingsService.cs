using System.IO;
using System.Text.Json;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Services;

public interface ISettingsService
{
    AppSettings Load();
    void Save(AppSettings settings);
    string GetGamesRootDirectory();
    void EnsureGamesRootDirectoryExists();
}

public class SettingsService : ISettingsService
{
    private const string AppSubfolder = "EricLostieLauncher";
    private static readonly string SettingsPath = Path.Combine(AppContext.BaseDirectory, "launcher_settings.json");
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public AppSettings Load()
    {
        Logs.DebugLogManager("Loading settings from disk.");
        if (!File.Exists(SettingsPath)) return new AppSettings();

        try
        {
            var json = File.ReadAllText(SettingsPath);
            return JsonSerializer.Deserialize<AppSettings>(json, JsonOptions) ?? new AppSettings();
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return new AppSettings();
        }
    }

    public void Save(AppSettings settings)
    {
        try
        {
            var json = JsonSerializer.Serialize(settings, JsonOptions);
            File.WriteAllText(SettingsPath, json);
            Logs.DebugLogManager("Settings saved to disk.");
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
