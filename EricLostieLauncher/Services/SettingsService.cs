using System.IO;
using System.Text.Json;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Services;

public interface ISettingsService
{
    AppSettings Load();
    void Save(AppSettings settings);
}

public class SettingsService : ISettingsService
{
    private static readonly string SettingsPath = Path.Combine(AppContext.BaseDirectory, "launcher_settings.json");
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public AppSettings Load()
    {
        if (!File.Exists(SettingsPath)) return new AppSettings();

        try
        {
            var json = File.ReadAllText(SettingsPath);
            return JsonSerializer.Deserialize<AppSettings>(json, JsonOptions) ?? new AppSettings();
        }
        catch
        {
            return new AppSettings();
        }
    }

    public void Save(AppSettings settings)
    {
        var json = JsonSerializer.Serialize(settings, JsonOptions);
        File.WriteAllText(SettingsPath, json);
    }
}
