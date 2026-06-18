namespace LostieLauncher.Utils;

internal sealed class WaitHandleSignalListener : IDisposable
{
    private readonly Action _onSignaled;
    private RegisteredWaitHandle? _registration;
    private bool _disposed;

    private WaitHandleSignalListener(Action onSignaled) => _onSignaled = onSignaled;

    public static WaitHandleSignalListener Start(WaitHandle waitHandle, Action onSignaled)
    {
        ArgumentNullException.ThrowIfNull(waitHandle);
        ArgumentNullException.ThrowIfNull(onSignaled);

        var listener = new WaitHandleSignalListener(onSignaled);
        listener._registration = ThreadPool.RegisterWaitForSingleObject(waitHandle, listener.OnWait, null, Timeout.Infinite, executeOnlyOnce: false);
        return listener;
    }

    private void OnWait(object? state, bool timedOut)
    {
        if (timedOut) return;

        try { _onSignaled(); }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;

        var registration = _registration;
        _registration = null;
        if (registration is null) return;

        using var completed = new ManualResetEvent(false);
        registration.Unregister(completed);
        completed.WaitOne();
    }
}
