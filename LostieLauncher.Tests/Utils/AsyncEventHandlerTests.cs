using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class AsyncEventHandlerTests
{
    [Fact]
    public void Wrap_WithNullCallback_Throws()
    {
        // Act
        var act = () => AsyncEventHandler.Wrap(null!);

        // Assert
        Should.Throw<ArgumentNullException>(act);
    }

    [Fact]
    public void Wrap_WhenCallbackThrowsSynchronously_DoesNotPropagate()
    {
        // Arrange — a callback that faults before any await, mimicking a body that throws
        // on the thread that raised the event (e.g. Process.Exited on the thread pool).
        var handler = AsyncEventHandler.Wrap((_, _) => throw new InvalidOperationException("boom"));

        // Act
        var act = () => handler.Invoke(null, EventArgs.Empty);

        // Assert — the wrapper swallows it; nothing escapes to crash the process.
        Should.NotThrow(act);
    }

    [Fact]
    public async Task Wrap_WhenCallbackThrowsAfterAwait_DoesNotCrashAndStillRuns()
    {
        // Arrange — the dangerous case: an exception after the first await would otherwise be an
        // unobserved async-void exception reaching AppDomain.UnhandledException.
        var ran = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var handler = AsyncEventHandler.Wrap(async (_, _) =>
        {
            await Task.Yield();
            ran.SetResult();
            throw new InvalidOperationException("boom");
        });

        // Act
        handler.Invoke(null, EventArgs.Empty);

        // Assert — the body executed and reaching here (test host alive) shows the throw was caught.
        await ran.Task;
    }

    [Fact]
    public async Task Wrap_WhenCallbackSucceeds_InvokesItWithForwardedArguments()
    {
        // Arrange
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

        // Act
        handler.Invoke(sender, args);
        await ran.Task;

        // Assert
        capturedSender.ShouldBeSameAs(sender);
        capturedArgs.ShouldBeSameAs(args);
    }
}
