using System.IO;

namespace LostieLauncher.Utils;

public static class FileFinalizer
{
    private const int DefaultMaxAttempts = 3;
    private static readonly TimeSpan DefaultRetryDelay = TimeSpan.FromMilliseconds(500);

    public static Task MoveAsync(string sourcePath, string destinationPath) =>
        MoveAsync(sourcePath, destinationPath, DefaultMaxAttempts, DefaultRetryDelay, Task.Delay);

    internal static async Task MoveAsync(string sourcePath, string destinationPath, int maxAttempts, TimeSpan retryDelay, Func<TimeSpan, Task> delay)
    {
        ArgumentOutOfRangeException.ThrowIfLessThan(maxAttempts, 1);

        for (var attempt = 1; ; attempt++)
        {
            try
            {
                File.Move(sourcePath, destinationPath, overwrite: true);
                return;
            }
            catch (Exception ex)
            {
                var retryable = attempt < maxAttempts && IsRetryable(ex, DescribeDestination(destinationPath));

                if (!retryable)
                {
                    Logs.ErrorLogManager($"Download finalization failed: {FileMoveDiagnostics.Describe(sourcePath, destinationPath, ex)}");
                    throw;
                }

                var backoff = retryDelay * attempt;
                Logs.InfoLogManager($"Finalization attempt {attempt}/{maxAttempts} failed ({ex.GetType().Name}: {ex.Message}), retrying in {backoff.TotalMilliseconds:0} ms...");
                await delay(backoff).ConfigureAwait(false);
            }
        }
    }

    internal readonly record struct DestinationState(bool IsDirectory, bool IsReadOnly);

    internal static bool IsRetryable(Exception error, DestinationState destination)
    {
        if (destination.IsDirectory || destination.IsReadOnly) return false;

        return error is UnauthorizedAccessException or IOException;
    }

    private static DestinationState DescribeDestination(string destinationPath)
    {
        try
        {
            if (Directory.Exists(destinationPath)) return new DestinationState(IsDirectory: true, IsReadOnly: false);
            if (!File.Exists(destinationPath)) return new DestinationState(IsDirectory: false, IsReadOnly: false);

            var isReadOnly = (File.GetAttributes(destinationPath) & FileAttributes.ReadOnly) != 0;
            return new DestinationState(IsDirectory: false, isReadOnly);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return new DestinationState(IsDirectory: false, IsReadOnly: false);
        }
    }
}
