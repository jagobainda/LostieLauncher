namespace EricLostieLauncher.Utils;

public static class ProcessUtils
{
    public static void RestartApplication()
    {
        var exePath = System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName;

        if (exePath is not null) System.Diagnostics.Process.Start(exePath);

        System.Diagnostics.Process.GetCurrentProcess().Kill();
    }
}
