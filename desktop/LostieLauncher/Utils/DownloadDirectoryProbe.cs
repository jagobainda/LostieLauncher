using LostieLauncher.Models;
using System.IO;

namespace LostieLauncher.Utils;

/// <summary>
/// Pre-flight check for a folder that is about to host a download.
/// <para>
/// A folder can accept gigabytes of writes and still refuse the rename that
/// finalizes a download, because renaming needs DELETE on the file (or
/// FILE_DELETE_CHILD on the parent) while writing does not. Probing writability
/// alone would therefore hand out a false green light on a network share, an
/// external drive with foreign ACLs or a managed machine, and the user would only
/// find out after transferring the whole file.
/// </para>
/// </summary>
public static class DownloadDirectoryProbe
{
    private const string ProbePrefix = ".lostie-probe-";
    private const string ProbeExtension = ".tmp";
    private const string RenamedExtension = ".moved";

    // Two bytes are enough to prove the write reached the filesystem while keeping
    // the probe imperceptible even on a slow network share.
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
            // Both names are removed regardless of which step failed, so the probe never
            // leaves a stray file in the user's library.
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
