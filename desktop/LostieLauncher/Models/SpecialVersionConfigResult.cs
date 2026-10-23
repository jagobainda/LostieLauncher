namespace LostieLauncher.Models;

public enum SpecialVersionConfigOutcome
{
    Success,
    NotFound,
    NetworkError,
    InvalidResponse,
    Cancelled
}

public class SpecialVersionConfigResult
{
    private SpecialVersionConfigResult()
    {
    }

    public SpecialVersionConfigOutcome Outcome { get; init; }
    public SpecialVersionConfig? Config { get; init; }

    public static SpecialVersionConfigResult Success(SpecialVersionConfig config) =>
        new() { Outcome = SpecialVersionConfigOutcome.Success, Config = config };

    public static SpecialVersionConfigResult NotFound() => new() { Outcome = SpecialVersionConfigOutcome.NotFound };

    public static SpecialVersionConfigResult NetworkError() => new() { Outcome = SpecialVersionConfigOutcome.NetworkError };

    public static SpecialVersionConfigResult InvalidResponse() => new() { Outcome = SpecialVersionConfigOutcome.InvalidResponse };

    public static SpecialVersionConfigResult Cancelled() => new() { Outcome = SpecialVersionConfigOutcome.Cancelled };
}
