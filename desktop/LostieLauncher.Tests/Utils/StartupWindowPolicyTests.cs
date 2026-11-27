using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class StartupWindowPolicyTests
{
    [Fact]
    public void ShouldShowOnStartup_NotMinimized_ReturnsTrue()
    {
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: false, hasSeenWelcome: true);

        show.ShouldBeTrue();
    }

    [Fact]
    public void ShouldShowOnStartup_MinimizedAndWelcomeSeen_ReturnsFalse()
    {
        var show = StartupWindowPolicy.ShouldShowOnStartup(startMinimized: true, hasSeenWelcome: true);

        show.ShouldBeFalse();
    }

    [Fact]
    public void ShouldShowOnStartup_MinimizedButFirstLaunch_ReturnsTrue()
    {
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
