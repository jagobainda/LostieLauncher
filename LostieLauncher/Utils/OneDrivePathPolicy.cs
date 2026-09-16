using System.IO;

namespace LostieLauncher.Utils;

/// <summary>
/// Decides whether a path lives inside a OneDrive-synced folder. Multi-gigabyte
/// game libraries do not belong there: every install is uploaded against the
/// user's cloud quota, the sync engine competes with extraction and deletion of
/// thousands of files, and dehydrated placeholders can make a game unplayable
/// offline.
/// <para>
/// <see cref="IsSynced"/> is a pure decision function — the caller supplies the
/// sync roots — so the environment lookup in
/// <see cref="GetEnvironmentSyncRoots"/> stays out of the tested logic.
/// </para>
/// </summary>
public static class OneDrivePathPolicy
{
    private const string OneDriveSegmentPrefix = "OneDrive";

    // The variables Windows sets for the personal account, and for each work or
    // school account signed into the sync client.
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

        // A sync root the environment does not advertise — a second account, a folder
        // re-pointed by hand — still lands under a directory literally named
        // "OneDrive" or "OneDrive - <tenant>".
        return HasOneDriveSegment(fullPath);
    }

    /// <summary>Reads the sync roots Windows publishes for the signed-in accounts.</summary>
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

        // The separator guard keeps "C:\OneDriveBackup" from matching the root "C:\OneDrive".
        return fullPath.StartsWith(fullRoot + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);
    }

    private static bool HasOneDriveSegment(string fullPath) => fullPath
        .Split([Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar], StringSplitOptions.RemoveEmptyEntries)
        .Any(segment => segment.Equals(OneDriveSegmentPrefix, StringComparison.OrdinalIgnoreCase)
            || segment.StartsWith(OneDriveSegmentPrefix + " - ", StringComparison.OrdinalIgnoreCase));
}
