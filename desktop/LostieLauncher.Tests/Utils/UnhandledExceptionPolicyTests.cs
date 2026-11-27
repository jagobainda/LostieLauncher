using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class UnhandledExceptionPolicyTests
{
    [Fact]
    public void Decide_BeforeStartupCompleted_IsFatal()
    {
        var action = UnhandledExceptionPolicy.Decide(startupCompleted: false);

        action.ShouldBe(UnhandledExceptionAction.Fatal);
    }

    [Fact]
    public void Decide_AfterStartupCompleted_KeepsAlive()
    {
        var action = UnhandledExceptionPolicy.Decide(startupCompleted: true);

        action.ShouldBe(UnhandledExceptionAction.KeepAlive);
    }
}
