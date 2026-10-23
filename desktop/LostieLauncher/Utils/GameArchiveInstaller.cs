using SharpCompress.Archives;
using SharpCompress.Common;
using SharpCompress.Readers;
using System.IO;

namespace LostieLauncher.Utils;

public static class GameArchiveInstaller
{
    public static async Task ExtractAsync(string zipPath, string extractDir) => await Task.Run(() =>
    {
        var tempDir = extractDir + ".tmp";
        var backupDir = extractDir + ".old";

        if (!DeleteLeftoverDirectory(tempDir))
            throw new IOException($"Could not clear the leftover extraction folder '{tempDir}'.");

        DeleteLeftoverDirectory(backupDir);

        Directory.CreateDirectory(tempDir);
        try
        {
            Logs.DebugLogManager($"Extracting archive: {Path.GetFileName(zipPath)}.");
            var readerOptions = new ReaderOptions
            {
                ArchiveEncoding = new ArchiveEncoding { Default = System.Text.Encoding.UTF8 }
            };

            var tempDirFull = Path.GetFullPath(tempDir) + Path.DirectorySeparatorChar;
            var entryCount = 0;
            using (var stream = File.OpenRead(zipPath))
            using (var archive = ArchiveFactory.OpenArchive(stream, readerOptions))
            {
                foreach (var entry in archive.Entries.Where(e => !e.IsDirectory && e.Key is not null))
                {
                    var destPath = Path.GetFullPath(Path.Combine(tempDir, entry.Key!));
                    if (!destPath.StartsWith(tempDirFull, StringComparison.OrdinalIgnoreCase))
                        throw new InvalidOperationException($"Zip Slip attempt detected in entry: {entry.Key}");
                    Directory.CreateDirectory(Path.GetDirectoryName(destPath)!);
                    using var entryStream = entry.OpenEntryStream();
                    using var outStream = new FileStream(destPath, FileMode.Create, FileAccess.Write, FileShare.None);
                    entryStream.CopyTo(outStream);
                    entryCount++;
                }
            }
            Logs.DebugLogManager($"Temp extraction complete: {entryCount} files.");

            AtomicSwapDirectories(tempDir, backupDir, extractDir);

            try { File.Delete(zipPath); } catch (Exception ex) { Logs.ErrorLogManager(ex); }
            Logs.DebugLogManager($"Extraction complete: {entryCount} files installed.");
        }
        catch
        {
            DeleteLeftoverDirectory(tempDir);
            throw;
        }
    });

    private static bool DeleteLeftoverDirectory(string path)
    {
        try
        {
            if (!Directory.Exists(path)) return true;

            var result = DirectoryRemover.Delete(path);
            if (!result.Deleted) Logs.ErrorLogManager($"Could not remove leftover directory '{path}' after {result.Attempts} attempt(s). Blocked at: {result.BlockingPath}.");

            return result.Deleted;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return false;
        }
    }

    internal static void AtomicSwapDirectories(string sourceDir, string backupDir, string targetDir)
    {
        var hadExisting = Directory.Exists(targetDir);
        if (hadExisting)
        {
            DeleteLeftoverDirectory(backupDir);
            Directory.Move(targetDir, backupDir);
        }

        try
        {
            Directory.Move(sourceDir, targetDir);
        }
        catch
        {
            if (hadExisting)
            {
                try { Directory.Move(backupDir, targetDir); } catch { }
            }
            throw;
        }

        if (hadExisting) DeleteLeftoverDirectory(backupDir);
    }
}
