using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class UnhandledExceptionPolicyTests
{
    [Fact]
    public void Decide_BeforeStartupCompleted_IsFatal()
    {
        // Before startup completes there is no window: swallowing would leave a phantom process
        // (the BUG-014 + BUG-032 interaction), so the only safe outcome is a clean shutdown.
        var action = UnhandledExceptionPolicy.Decide(startupCompleted: false);

        action.ShouldBe(UnhandledExceptionAction.Fatal);
    }

    [Fact]
    public void Decide_AfterStartupCompleted_KeepsAlive()
    {
        // After startup a single runtime glitch must not kill the user's session.
        var action = UnhandledExceptionPolicy.Decide(startupCompleted: true);

        action.ShouldBe(UnhandledExceptionAction.KeepAlive);
    }
}
