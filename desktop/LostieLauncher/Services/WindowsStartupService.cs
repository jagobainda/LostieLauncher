using Microsoft.Win32;

namespace LostieLauncher.Services;

public interface IWindowsStartupService
{
    public bool IsEnabled();
    public bool Enable();
    public bool Disable();
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

    public bool Enable()
    {
        try
        {
            if (!TryBuildStartupCommand(Environment.ProcessPath, out var command))
            {
                Logs.ErrorLogManager("Cannot enable Windows startup: the executable path is unavailable.");
                return false;
            }

            using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
            if (key is null)
            {
                Logs.ErrorLogManager("Could not open registry run key for writing.");
                return false;
            }

            key.SetValue(AppName, command);
            Logs.InfoLogManager("Windows startup entry enabled.");
            return true;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }

    public bool Disable()
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
            if (key is null)
            {
                Logs.DebugLogManager("Run registry key unavailable; nothing to disable.");
                return true;
            }

            key.DeleteValue(AppName, false);
            Logs.InfoLogManager("Windows startup entry disabled.");
            return true;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }

    internal static bool TryBuildStartupCommand(string? processPath, out string command)
    {
        if (string.IsNullOrWhiteSpace(processPath))
        {
            command = string.Empty;
            return false;
        }

        command = $"\"{processPath}\"";
        return true;
    }
}
