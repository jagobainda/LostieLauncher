using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class AsyncEventHandlerTests
{
    [Fact]
    public void Wrap_WithNullCallback_Throws()
    {
        var act = () => AsyncEventHandler.Wrap(null!);

        Should.Throw<ArgumentNullException>(act);
    }

    [Fact]
    public void Wrap_WhenCallbackThrowsSynchronously_DoesNotPropagate()
    {
        var handler = AsyncEventHandler.Wrap((_, _) => throw new InvalidOperationException("boom"));

        var act = () => handler.Invoke(null, EventArgs.Empty);

        Should.NotThrow(act);
    }

    [Fact]
    public async Task Wrap_WhenCallbackThrowsAfterAwait_DoesNotCrashAndStillRuns()
    {
        var ran = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var handler = AsyncEventHandler.Wrap(async (_, _) =>
        {
            await Task.Yield();
            ran.SetResult();
            throw new InvalidOperationException("boom");
        });

        handler.Invoke(null, EventArgs.Empty);

        await ran.Task;
    }

    [Fact]
    public async Task Wrap_WhenCallbackSucceeds_InvokesItWithForwardedArguments()
    {
        object? capturedSender = null;
        EventArgs? capturedArgs = null;
        var ran = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var sender = new object();
        var args = new EventArgs();

        var handler = AsyncEventHandler.Wrap(async (s, e) =>
        {
            await Task.Yield();
            capturedSender = s;
            capturedArgs = e;
            ran.SetResult();
        });

        handler.Invoke(sender, args);
        await ran.Task;

        capturedSender.ShouldBeSameAs(sender);
        capturedArgs.ShouldBeSameAs(args);
    }
}
