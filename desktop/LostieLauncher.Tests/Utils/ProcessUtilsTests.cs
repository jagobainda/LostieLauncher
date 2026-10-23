using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class ProcessUtilsTests
{
    private const string ExePath = @"C:\app\LostieLauncher.exe";

    [Fact]
    public void RestartApplication_WithNullRestarter_Throws()
    {
        var act = () => ProcessUtils.RestartApplication(null!);

        Should.Throw<ArgumentNullException>(act);
    }

    [Fact]
    public void RestartApplication_WhenExecutablePathAvailable_ReleasesLockBeforeStartingBeforeShuttingDown()
    {
        var restarter = Substitute.For<IApplicationRestarter>();
        restarter.GetExecutablePath().Returns(ExePath);

        ProcessUtils.RestartApplication(restarter);

        // The single-instance lock MUST be released before the child starts, or the child sees
        // the mutex held and shuts itself down (the original BUG-007 defect).
        Received.InOrder(() =>
        {
            restarter.ReleaseSingleInstanceLock();
            restarter.StartProcess(ExePath);
            restarter.Shutdown();
        });
        restarter.DidNotReceive().ReacquireSingleInstanceLock();
    }

    [Fact]
    public void RestartApplication_WhenExecutablePathNull_KeepsLauncherRunning()
    {
        var restarter = Substitute.For<IApplicationRestarter>();
        restarter.GetExecutablePath().Returns((string?)null);

        ProcessUtils.RestartApplication(restarter);

        restarter.DidNotReceive().ReleaseSingleInstanceLock();
        restarter.DidNotReceiveWithAnyArgs().StartProcess(default!);
        restarter.DidNotReceive().Shutdown();
    }

    [Fact]
    public void RestartApplication_WhenExecutablePathEmpty_KeepsLauncherRunning()
    {
        var restarter = Substitute.For<IApplicationRestarter>();
        restarter.GetExecutablePath().Returns(string.Empty);

        ProcessUtils.RestartApplication(restarter);

        restarter.DidNotReceive().ReleaseSingleInstanceLock();
        restarter.DidNotReceiveWithAnyArgs().StartProcess(default!);
        restarter.DidNotReceive().Shutdown();
    }

    [Fact]
    public void RestartApplication_WhenStartProcessThrows_ReacquiresLockAndDoesNotShutDown()
    {
        var restarter = Substitute.For<IApplicationRestarter>();
        restarter.GetExecutablePath().Returns(ExePath);
        restarter.When(r => r.StartProcess(Arg.Any<string>())).Do(_ => throw new InvalidOperationException("boom"));

        var act = () => ProcessUtils.RestartApplication(restarter);

        Should.NotThrow(act);
        // The child never started: restore the lock so single-instance still holds, and stay alive.
        Received.InOrder(() =>
        {
            restarter.ReleaseSingleInstanceLock();
            restarter.StartProcess(ExePath);
            restarter.ReacquireSingleInstanceLock();
        });
        restarter.DidNotReceive().Shutdown();
    }
}
