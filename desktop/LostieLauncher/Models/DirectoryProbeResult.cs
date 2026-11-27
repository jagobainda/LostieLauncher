namespace LostieLauncher.Models;

public enum DirectoryProbeOutcome
{
    Usable,
    CannotCreate,
    CannotWrite,
    CannotRename
}

public readonly record struct DirectoryProbeResult(DirectoryProbeOutcome Outcome, string Directory, string? Error)
{
    public bool IsUsable => Outcome == DirectoryProbeOutcome.Usable;

    public static DirectoryProbeResult Usable(string directory) => new(DirectoryProbeOutcome.Usable, directory, null);

    public static DirectoryProbeResult Failed(DirectoryProbeOutcome outcome, string directory, Exception error) =>
        new(outcome, directory, $"{error.GetType().Name}: {error.Message}");
}
