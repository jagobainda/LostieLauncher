using System.IO;

namespace EricLostieLauncher.Models;

public class AppSettings
{
    public AppLanguage Language { get; set; } = AppLanguage.Esp;
    public AppTheme Theme { get; set; } = AppTheme.Volcarona;
    public bool StartWithWindows { get; set; }
    public bool StartMinimized { get; set; }
    public bool AutoUpdate { get; set; } = true;
    public string DownloadDirectory { get; set; } = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments), "EricLostie", "Games");
    public string TelemetryApiKey { get; set; } = "4V7p0XSJ9C6FgCE7ae3c";
    public string TelemetryEndpoint { get; set; } = "http://localhost:6969/launcher/api/telemetry";
}
