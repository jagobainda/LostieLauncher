namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Per-test scratch directory under <c>%TEMP%\LostieLauncherTests\{guid}</c>.
/// Implement <see cref="IDisposable"/> on the test class and instantiate this in the
/// constructor to obtain a writable, isolated path. The directory is recursively deleted
/// on dispose. Failures during cleanup are swallowed so they never mask the real test
/// outcome.
/// </summary>
public sealed class TempDirectoryFixture : IDisposable
{
    public string Path { get; }

    public TempDirectoryFixture(string? prefix = null)
    {
        var name = (prefix ?? "test") + "-" + Guid.NewGuid().ToString("N");
        Path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "LostieLauncherTests", name);
        Directory.CreateDirectory(Path);
    }

    /// <summary>Combine a relative path under the scratch directory.</summary>
    public string Combine(params string[] parts) => System.IO.Path.Combine([Path, .. parts]);

    public void Dispose()
    {
        try
        {
            if (Directory.Exists(Path)) Directory.Delete(Path, recursive: true);
        }
        catch
        {
            // Cleanup is best-effort; never let tear-down failures fail the test.
        }
    }
}
