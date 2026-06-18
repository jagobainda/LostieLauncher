using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class WaitHandleSignalListenerTests
{
    private static readonly TimeSpan SignalTimeout = TimeSpan.FromSeconds(5);
    private static readonly TimeSpan NoSignalGrace = TimeSpan.FromMilliseconds(300);

    [Fact]
    public void Start_WithNullWaitHandle_Throws()
    {
        var act = () => WaitHandleSignalListener.Start(null!, () => { });

        Should.Throw<ArgumentNullException>(act);
    }

    [Fact]
    public void Start_WithNullCallback_Throws()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);

        var act = () => WaitHandleSignalListener.Start(handle, null!);

        Should.Throw<ArgumentNullException>(act);
    }

    [Fact]
    public void Start_WhenSignaled_InvokesCallback()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);
        using var invoked = new ManualResetEventSlim(false);

        using var listener = WaitHandleSignalListener.Start(handle, invoked.Set);

        handle.Set();

        invoked.Wait(SignalTimeout).ShouldBeTrue();
    }

    [Fact]
    public void Start_WhenSignaledMultipleTimes_InvokesCallbackEachTime()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);
        var count = 0;
        using var invoked = new AutoResetEvent(false);

        using var listener = WaitHandleSignalListener.Start(handle, () =>
        {
            Interlocked.Increment(ref count);
            invoked.Set();
        });

        handle.Set();
        invoked.WaitOne(SignalTimeout).ShouldBeTrue();
        handle.Set();
        invoked.WaitOne(SignalTimeout).ShouldBeTrue();

        Volatile.Read(ref count).ShouldBe(2);
    }

    [Fact]
    public void OnSignaled_WhenCallbackThrows_DoesNotPropagate()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);
        using var ran = new ManualResetEventSlim(false);

        using var listener = WaitHandleSignalListener.Start(handle, () =>
        {
            ran.Set();
            throw new InvalidOperationException("boom");
        });

        handle.Set();

        ran.Wait(SignalTimeout).ShouldBeTrue();
    }

    [Fact]
    public void Dispose_AfterDispose_NoFurtherCallbacksOnSignal()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);
        var count = 0;
        using var invoked = new AutoResetEvent(false);

        var listener = WaitHandleSignalListener.Start(handle, () =>
        {
            Interlocked.Increment(ref count);
            invoked.Set();
        });

        handle.Set();
        invoked.WaitOne(SignalTimeout).ShouldBeTrue();

        listener.Dispose();
        handle.Set();

        invoked.WaitOne(NoSignalGrace).ShouldBeFalse();
        Volatile.Read(ref count).ShouldBe(1);
    }

    [Fact]
    public void Dispose_IsIdempotent()
    {
        using var handle = new EventWaitHandle(false, EventResetMode.AutoReset);
        var listener = WaitHandleSignalListener.Start(handle, () => { });

        listener.Dispose();
        var act = listener.Dispose;

        Should.NotThrow(act);
    }
}
