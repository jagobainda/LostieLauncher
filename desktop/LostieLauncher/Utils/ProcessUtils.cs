using System.Diagnostics;

namespace LostieLauncher.Utils;

public interface IApplicationRestarter
{
    public string? GetExecutablePath();

    public void ReleaseSingleInstanceLock();

    public void ReacquireSingleInstanceLock();

    public void StartProcess(string exePath);

    public void Shutdown();
}

public static class ProcessUtils
{
    public static void RestartApplication() => RestartApplication(new ApplicationRestarter());

    internal static void RestartApplication(IApplicationRestarter restarter)
    {
        ArgumentNullException.ThrowIfNull(restarter);

        Logs.InfoLogManager("Restarting application.");
        try
        {
            var exePath = restarter.GetExecutablePath();

            if (string.IsNullOrEmpty(exePath))
            {
                Logs.ErrorLogManager("Cannot restart: executable path is unavailable. Keeping the launcher running.");
                return;
            }

            restarter.ReleaseSingleInstanceLock();
            try
            {
                restarter.StartProcess(exePath);
            }
            catch
            {
                restarter.ReacquireSingleInstanceLock();
                throw;
            }

            restarter.Shutdown();
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    private sealed class ApplicationRestarter : IApplicationRestarter
    {
        public string? GetExecutablePath() => Environment.ProcessPath;

        public void ReleaseSingleInstanceLock() => (Application.Current as App)?.ReleaseSingleInstanceLock();

        public void ReacquireSingleInstanceLock() => (Application.Current as App)?.ReacquireSingleInstanceLock();

        public void StartProcess(string exePath) => Process.Start(exePath);

        public void Shutdown() => Application.Current.Shutdown();
    }
}
