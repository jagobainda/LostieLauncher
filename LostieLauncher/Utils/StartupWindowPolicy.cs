namespace LostieLauncher.Utils;

public static class StartupWindowPolicy
{
    public static bool ShouldShowOnStartup(bool startMinimized, bool hasSeenWelcome)
        => !startMinimized || !hasSeenWelcome;
}
