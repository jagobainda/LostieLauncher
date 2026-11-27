using System.IO;

namespace LostieLauncher.Utils;

public static class OneDrivePathPolicy
{
    private const string OneDriveSegmentPrefix = "OneDrive";

    private static readonly string[] SyncRootVariables = ["OneDrive", "OneDriveConsumer", "OneDriveCommercial"];

    public static bool IsSynced(string? path, IEnumerable<string?> syncRoots)
    {
        ArgumentNullException.ThrowIfNull(syncRoots);

        if (!TryNormalize(path, out var fullPath)) return false;

        foreach (var root in syncRoots)
        {
            if (!TryNormalize(root, out var fullRoot)) continue;
            if (IsUnder(fullPath, fullRoot)) return true;
        }

        return HasOneDriveSegment(fullPath);
    }

    public static IEnumerable<string?> GetEnvironmentSyncRoots() =>
        SyncRootVariables.Select(Environment.GetEnvironmentVariable);

    private static bool TryNormalize(string? path, out string fullPath)
    {
        fullPath = string.Empty;
        if (string.IsNullOrWhiteSpace(path)) return false;

        try
        {
            fullPath = Path.TrimEndingDirectorySeparator(Path.GetFullPath(path));
            return true;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager($"Could not normalize path '{path}' while checking for OneDrive. {ex.GetType().Name}: {ex.Message}");
            return false;
        }
    }

    private static bool IsUnder(string fullPath, string fullRoot)
    {
        if (fullPath.Equals(fullRoot, StringComparison.OrdinalIgnoreCase)) return true;

        return fullPath.StartsWith(fullRoot + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);
    }

    private static bool HasOneDriveSegment(string fullPath) => fullPath
        .Split([Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar], StringSplitOptions.RemoveEmptyEntries)
        .Any(segment => segment.Equals(OneDriveSegmentPrefix, StringComparison.OrdinalIgnoreCase)
            || segment.StartsWith(OneDriveSegmentPrefix + " - ", StringComparison.OrdinalIgnoreCase));
}
