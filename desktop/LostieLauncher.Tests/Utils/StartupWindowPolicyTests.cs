using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class StartupWindowPolicyTests
{
    [Fact]
    public void ShouldShowOnStartup_NotMinimized_ReturnsTrue()
    {
        // The normal case: the window is shown.
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: false, hasSeenWelcome: true);

        show.ShouldBeTrue();
    }

    [Fact]
    public void ShouldShowOnStartup_MinimizedAndWelcomeSeen_ReturnsFalse()
    {
        // The BUG-049 case: starting minimized must NOT show the window (no Show()-then-Hide() flash).
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: true, hasSeenWelcome: true);

        show.ShouldBeFalse();
    }

    [Fact]
    public void ShouldShowOnStartup_MinimizedButFirstLaunch_ReturnsTrue()
    {
        // Defensive: on a first launch the window is shown so the welcome dialog is not left floating
        // over a hidden launcher. (On a clean install StartMinimized defaults to false, so this only
        // guards a hand-edited/imported settings file.)
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: true, hasSeenWelcome: false);

        show.ShouldBeTrue();
    }

    [Fact]
    public void ShouldShowOnStartup_NotMinimizedAndFirstLaunch_ReturnsTrue()
    {
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: false, hasSeenWelcome: false);

        show.ShouldBeTrue();
    }
}
