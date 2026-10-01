namespace LostieLauncher.Models;

/// <summary>
/// Which step of the download-directory pre-flight failed. Ordered so that the
/// default value is <see cref="Usable"/>: an unconfigured probe must never block
/// a download by accident.
/// </summary>
public enum DirectoryProbeOutcome
{
    Usable,
    CannotCreate,
    CannotWrite,
    CannotRename
}

/// <summary>
/// Outcome of the write+rename pre-flight on a folder that will host downloads.
/// <see cref="Error"/> carries the underlying exception text for the log, never
/// for the UI — the dialog builds its message from <see cref="Outcome"/>.
/// </summary>
public readonly record struct DirectoryProbeResult(DirectoryProbeOutcome Outcome, string Directory, string? Error)
{
    public bool IsUsable => Outcome == DirectoryProbeOutcome.Usable;

    public static DirectoryProbeResult Usable(string directory) => new(DirectoryProbeOutcome.Usable, directory, null);

    public static DirectoryProbeResult Failed(DirectoryProbeOutcome outcome, string directory, Exception error) =>
        new(outcome, directory, $"{error.GetType().Name}: {error.Message}");
}
