using Microsoft.Win32;

namespace EricLostieLauncher.Services;

public interface IWindowsStartupService
{
    bool IsEnabled();
    void Enable();
    void Disable();
}

public class WindowsStartupService : IWindowsStartupService
{
    private const string RunKey = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Run";
    private const string AppName = "EricLostieLauncher";

    public bool IsEnabled()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, false);
        return key?.GetValue(AppName) != null;
    }

    public void Enable()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
        key?.SetValue(AppName, $"\"{Environment.ProcessPath}\"");
    }

    public void Disable()
    {
        using var key = Registry.CurrentUser.OpenSubKey(RunKey, true);
        key?.DeleteValue(AppName, false);
    }
}
