using LostieLauncher.Models;
using System.IO;

namespace LostieLauncher.Utils;

public static class DownloadDirectoryProbe
{
    private const string ProbePrefix = ".lostie-probe-";
    private const string ProbeExtension = ".tmp";
    private const string RenamedExtension = ".moved";

    private static readonly byte[] ProbeContent = [0x4C, 0x4C];

    public static DirectoryProbeResult Run(string directory) => Run(
        directory,
        path => Directory.CreateDirectory(path),
        path => File.WriteAllBytes(path, ProbeContent),
        (source, destination) => File.Move(source, destination),
        File.Delete);

    internal static DirectoryProbeResult Run(
        string directory,
        Action<string> createDirectory,
        Action<string> write,
        Action<string, string> rename,
        Action<string> delete)
    {
        if (string.IsNullOrWhiteSpace(directory))
            return new DirectoryProbeResult(DirectoryProbeOutcome.CannotCreate, directory ?? string.Empty, "The download directory is empty.");

        try
        {
            createDirectory(directory);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager($"Download directory probe failed at create: '{directory}'. {ex.GetType().Name}: {ex.Message}");
            return DirectoryProbeResult.Failed(DirectoryProbeOutcome.CannotCreate, directory, ex);
        }

        var probePath = Path.Combine(directory, $"{ProbePrefix}{Guid.NewGuid():N}{ProbeExtension}");
        var renamedPath = probePath + RenamedExtension;

        try
        {
            try
            {
                write(probePath);
            }
            catch (Exception ex)
            {
                Logs.ErrorLogManager($"Download directory probe failed at write: '{directory}'. {ex.GetType().Name}: {ex.Message}");
                return DirectoryProbeResult.Failed(DirectoryProbeOutcome.CannotWrite, directory, ex);
            }

            try
            {
                rename(probePath, renamedPath);
            }
            catch (Exception ex)
            {
                Logs.ErrorLogManager($"Download directory probe failed at rename: '{directory}'. {ex.GetType().Name}: {ex.Message}");
                return DirectoryProbeResult.Failed(DirectoryProbeOutcome.CannotRename, directory, ex);
            }

            return DirectoryProbeResult.Usable(directory);
        }
        finally
        {
            TryDelete(delete, probePath);
            TryDelete(delete, renamedPath);
        }
    }

    private static void TryDelete(Action<string> delete, string path)
    {
        try
        {
            delete(path);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager($"Download directory probe could not remove its temporary file '{path}'. {ex.GetType().Name}: {ex.Message}");
        }
    }
}
