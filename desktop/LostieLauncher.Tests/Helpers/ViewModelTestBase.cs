namespace LostieLauncher.Tests.Helpers;

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

    protected Task PumpAsync(TimeSpan? timeout = null) => SyncContext.PumpAsync(timeout ?? TimeSpan.FromSeconds(5));

    protected static PropertyChangedRecorder Record(System.ComponentModel.INotifyPropertyChanged source) => new(source);

    public void Dispose()
    {
        SynchronizationContext.SetSynchronizationContext(_previousContext);
        SyncContext.Dispose();
        GC.SuppressFinalize(this);
    }
}

public sealed class SingleThreadSynchronizationContext : SynchronizationContext, IDisposable
{
    private readonly System.Collections.Concurrent.BlockingCollection<(SendOrPostCallback callback, object? state)> _queue = [];

    public override void Post(SendOrPostCallback d, object? state)
    {
        if (_queue.IsAddingCompleted) return;
        _queue.Add((d, state));
    }

    public override void Send(SendOrPostCallback d, object? state) => d(state);

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
