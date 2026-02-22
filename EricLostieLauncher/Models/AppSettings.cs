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
}
