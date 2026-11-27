namespace LostieLauncher.Models;

public static class DownloadDefaults
{
    public static string DownloadDirectory { get; } =
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
}
