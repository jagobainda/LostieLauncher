namespace LostieLauncher.Tests.Helpers;

public sealed class TempDirectoryFixture : IDisposable
{
    public string Path { get; }

    public TempDirectoryFixture(string? prefix = null)
    {
        var name = (prefix ?? "test") + "-" + Guid.NewGuid().ToString("N");
        Path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "LostieLauncherTests", name);
        Directory.CreateDirectory(Path);
    }

    public string Combine(params string[] parts) => System.IO.Path.Combine([Path, .. parts]);

    public void Dispose()
    {
        try
        {
            if (Directory.Exists(Path)) Directory.Delete(Path, recursive: true);
        }
        catch
        {
        }
    }
}
