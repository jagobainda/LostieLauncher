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
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, false);
        return key?.GetValue(AppName) != null;
    }

    public void Enable()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
        key?.SetValue(AppName, $"\"{Environment.ProcessPath}\"");
        Logs.InfoLogManager("Windows startup entry enabled.");
    }

    public void Disable()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
        key?.DeleteValue(AppName, false);
        Logs.InfoLogManager("Windows startup entry disabled.");
    }
}
