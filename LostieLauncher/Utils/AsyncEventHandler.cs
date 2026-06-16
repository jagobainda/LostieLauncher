namespace LostieLauncher.Utils;

/// <summary>
/// Helpers to subscribe asynchronous callbacks to .NET events without the classic
/// <c>async void</c> hazard: an exception thrown after the first <c>await</c> in an
/// <c>async void</c> handler is unobservable, escapes to
/// <see cref="AppDomain.UnhandledException"/> and can tear down the whole process —
/// especially for events that raise on a thread-pool thread (e.g. <c>Process.Exited</c>),
/// where there is no synchronization context to marshal the failure back.
/// </summary>
public static class AsyncEventHandler
{
    /// <summary>
    /// Wraps an asynchronous callback into an <see cref="EventHandler"/> whose body is fully
    /// guarded: any exception is logged instead of propagating. The returned delegate is
    /// <c>async void</c> (as every event handler ultimately must be), but its body cannot
    /// throw, so it can never crash the process.
    /// </summary>
    public static EventHandler Wrap(Func<object?, EventArgs, Task> callback)
    {
        ArgumentNullException.ThrowIfNull(callback);

        return async (sender, e) =>
        {
            try
            {
                await callback(sender, e).ConfigureAwait(false);
            }
            catch (Exception ex)
            {
                // The logger itself can throw on I/O (disk full, permissions). Swallow that too:
                // letting it escape would re-create the very async-void crash this wrapper exists
                // to prevent.
                try { Logs.ErrorLogManager(ex); }
                catch { /* nothing safe left to do */ }
            }
        };
    }
}
