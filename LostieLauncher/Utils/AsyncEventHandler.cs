namespace LostieLauncher.Utils;

public static class AsyncEventHandler
{
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
                try { Logs.ErrorLogManager(ex); }
                catch { }
            }
        };
    }
}
