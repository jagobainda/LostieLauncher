namespace LostieLauncher.Models;

public class AppSettings
{
    public AppLanguage Language { get; set; } = AppLanguage.Esp;
    public AppTheme Theme { get; set; } = AppTheme.Volcarona;
    public bool StartWithWindows { get; set; }
    public bool StartMinimized { get; set; }
    public bool AutoUpdate { get; set; } = false;
    public string DownloadDirectory { get; set; } = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
    public bool HasSeenWelcome { get; set; } = false;
}
