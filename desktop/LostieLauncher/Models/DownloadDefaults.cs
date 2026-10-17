namespace LostieLauncher.Models;

/// <summary>
/// Where a fresh install puts the game library, in one place because the value is needed
/// by the model default, by the settings sanitizer and by the ViewModel's initial value,
/// and those three must never drift apart.
/// </summary>
public static class DownloadDefaults
{
    /// <summary>
    /// The user's profile folder, so the library lands in <c>%USERPROFILE%\LostieLauncher</c>
    /// once the games subfolder is appended. It needs no elevation, and every other candidate
    /// carries a hazard a multi-gigabyte game library cannot afford:
    /// <list type="bullet">
    /// <item>Documents is redirected into OneDrive on most consumer installs (cloud quota, sync
    /// contention, Controlled Folder Access, online-only placeholders that fail offline).</item>
    /// <item><c>%LOCALAPPDATA%</c> would resolve to <c>%LOCALAPPDATA%\LostieLauncher</c>, which is
    /// the launcher's own Velopack install directory — the pack id is the same name — and the
    /// parent of its log folder. Uninstalling the launcher would delete the whole library.</item>
    /// <item>The drive root can be locked down by policy on a managed machine.</item>
    /// <item>Downloads is the one folder Windows points an automatic cleanup at: Storage Sense
    /// deletes its contents by last-access date, which would strip a game's assets while leaving
    /// the executable behind.</item>
    /// </list>
    /// </summary>
    public static string DownloadDirectory { get; } =
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
}
