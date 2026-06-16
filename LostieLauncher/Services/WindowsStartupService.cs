using Microsoft.Win32;

namespace LostieLauncher.Services;

public interface IWindowsStartupService
{
    public bool IsEnabled();
    public void Enable();
    public void Disable();
}

public class WindowsStartupService : IWindowsStartupService
{
    private const string RunKey = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Run";
    private const string AppName = "LostieLauncher";

    public bool IsEnabled()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey, false);
            var enabled = key?.GetValue(AppName) != null;
            Logs.DebugLogManager($"Windows startup check: {(enabled ? "enabled" : "disabled")}.");
            return enabled;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }

    public void Enable()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
            if (key is null)
            {
                Logs.ErrorLogManager("Could not open registry run key for writing.");
                return;
            }
            key.SetValue(AppName, $"\"{Environment.ProcessPath}\"");
            Logs.InfoLogManager("Windows startup entry enabled.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

    public void Disable()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
            if (key is null)
            {
                Logs.DebugLogManager("Windows startup entry not found to disable.");
                return;
            }
            key.DeleteValue(AppName, false);
            Logs.InfoLogManager("Windows startup entry disabled.");
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }
}
