using LostieLauncher.Models;

namespace LostieLauncher.Utils;

public static class DownloadCachePolicy
{
    public static readonly TimeSpan DefaultMaxAge = TimeSpan.FromDays(14);

    private static readonly string[] ManagedExtensions = [".zip", ".part", ".part.meta"];

    public static IReadOnlyList<string> SelectStaleFiles(IEnumerable<DownloadCacheEntry> entries, IReadOnlySet<string> knownGameIds, DateTime nowUtc, TimeSpan maxAge)
    {
        var managed = entries.Where(entry => IsManaged(entry.FileName)).ToList();
        var partFiles = managed
            .Select(entry => entry.FileName)
            .Where(name => name.EndsWith(".part", StringComparison.OrdinalIgnoreCase))
            .ToHashSet(StringComparer.OrdinalIgnoreCase);

        return [.. managed.Where(entry => IsStale(entry, partFiles, knownGameIds, nowUtc, maxAge)).Select(entry => entry.FileName)];
    }

    private static bool IsStale(DownloadCacheEntry entry, HashSet<string> partFiles, IReadOnlySet<string> knownGameIds, DateTime nowUtc, TimeSpan maxAge)
    {
        if (entry.FileName.EndsWith(".part.meta", StringComparison.OrdinalIgnoreCase)
            && !partFiles.Contains(entry.FileName[..^".meta".Length]))
            return true;

        if (nowUtc - entry.LastWriteTimeUtc > maxAge) return true;

        return !knownGameIds.Contains(GetGameId(entry.FileName));
    }

    private static bool IsManaged(string fileName) =>
        ManagedExtensions.Any(extension => fileName.EndsWith(extension, StringComparison.OrdinalIgnoreCase));

    private static string GetGameId(string fileName)
    {
        var separator = fileName.IndexOf('.');
        return separator <= 0 ? string.Empty : fileName[..separator];
    }
}
