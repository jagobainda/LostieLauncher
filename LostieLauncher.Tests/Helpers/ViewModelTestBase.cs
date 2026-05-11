namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Base fixture for tests of CommunityToolkit.Mvvm view models.
/// </summary>
/// <remarks>
/// <para>
/// The production app uses WPF's Dispatcher to marshal callbacks onto the UI thread.
/// Unit tests must NOT depend on a real <c>Dispatcher</c> (it requires a desktop session
/// and would deadlock in <c>dotnet test</c>). Instead, this base installs a deterministic
/// <see cref="SingleThreadSynchronizationContext"/> on the test thread so that <c>async</c>
/// continuations from <see cref="RelayCommand"/> / <see cref="AsyncRelayCommand"/> stay on
/// the same thread and can be drained with <see cref="PumpAsync"/>.
/// </para>
/// <para>
/// Inherit from this class for any view-model test that exercises asynchronous commands or
/// relies on <c>OnPropertyChanged</c> being raised on the calling thread.
/// </para>
/// </remarks>
public abstract class ViewModelTestBase : IDisposable
{
    private readonly SynchronizationContext? _previousContext;
    protected SingleThreadSynchronizationContext SyncContext { get; }

    protected ViewModelTestBase()
    {
        _previousContext = SynchronizationContext.Current;
        SyncContext = new SingleThreadSynchronizationContext();
        SynchronizationContext.SetSynchronizationContext(SyncContext);
    }

    /// <summary>
    /// Drains all queued continuations on the test synchronization context until the
    /// queue is empty (or <paramref name="timeout"/> elapses). Use after triggering an
    /// async command to flush its <c>await</c> continuations before asserting.
    /// </summary>
    protected Task PumpAsync(TimeSpan? timeout = null) =>
        SyncContext.PumpAsync(timeout ?? TimeSpan.FromSeconds(5));

    /// <summary>Convenience: record property-changed events from <paramref name="source"/>.</summary>
    protected static PropertyChangedRecorder Record(System.ComponentModel.INotifyPropertyChanged source) => new(source);

    public void Dispose()
    {
        SynchronizationContext.SetSynchronizationContext(_previousContext);
        SyncContext.Dispose();
        GC.SuppressFinalize(this);
    }
}

/// <summary>
/// Minimal single-threaded <see cref="SynchronizationContext"/> with an explicit pump,
/// modeled after Stephen Toub's "AsyncPump". Continuations posted via <see cref="Post"/>
/// are queued and only run when <see cref="PumpAsync"/> is awaited, so tests stay
/// deterministic.
/// </summary>
public sealed class SingleThreadSynchronizationContext : SynchronizationContext, IDisposable
{
    private readonly System.Collections.Concurrent.BlockingCollection<(SendOrPostCallback callback, object? state)> _queue = [];

    public override void Post(SendOrPostCallback d, object? state)
    {
        if (_queue.IsAddingCompleted) return;
        _queue.Add((d, state));
    }

    public override void Send(SendOrPostCallback d, object? state) => d(state);

    /// <summary>Run queued callbacks until the queue is idle for one tick or the timeout elapses.</summary>
    public async Task PumpAsync(TimeSpan timeout)
    {
        var deadline = DateTime.UtcNow + timeout;
        while (DateTime.UtcNow < deadline)
        {
            if (_queue.TryTake(out var item, millisecondsTimeout: 10))
            {
                item.callback(item.state);
                continue;
            }

            // Nothing queued right now: yield once to let pending Tasks reschedule, then re-check.
            await Task.Yield();
            if (_queue.Count == 0) return;
        }
    }

    public void Dispose()
    {
        _queue.CompleteAdding();
        _queue.Dispose();
    }
}
