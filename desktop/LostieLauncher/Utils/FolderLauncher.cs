using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;
using System.IO;

namespace LostieLauncher.Utils;

public static class FolderLauncher
{
    public static void OpenFolder(string? path)
    {
        if (!TryGetExistingDirectory(path, out var directory))
        {
            Logs.ErrorLogManager($"Refused to open folder (not an existing directory): {path ?? "<null>"}");
            return;
        }

        try
        {
            Process.Start(new ProcessStartInfo(directory) { UseShellExecute = true });
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    public static bool TryGetExistingDirectory(string? path, [NotNullWhen(true)] out string? directory)
    {
        if (!string.IsNullOrWhiteSpace(path) && Directory.Exists(path))
        {
            directory = path;
            return true;
        }

        directory = null;
        return false;
    }
}
