namespace LostieLauncher.Utils;

public static class ProcessUtils
{
    public static void RestartApplication()
    {
        Logs.InfoLogManager("Restarting application.");
        var exePath = System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName;

        if (exePath is not null) System.Diagnostics.Process.Start(exePath);

        Application.Current.Shutdown();
    }
}
