namespace LostieLauncher.Utils;

public enum UnhandledExceptionAction
{
    /// <summary>The launcher must show a fatal-error notice and shut down cleanly.</summary>
    Fatal,

    /// <summary>The exception is swallowed (marked handled) and the launcher keeps running.</summary>
    KeepAlive
}

/// <summary>
/// Decides what to do with an unhandled UI-thread exception. Extracted as a pure function so the
/// policy is explicit and testable instead of an accidental, unconditional <c>Handled = true</c>
/// (the BUG-014 defect).
/// </summary>
public static class UnhandledExceptionPolicy
{
    /// <summary>
    /// Returns the action for an unhandled dispatcher exception.
    /// <para>
    /// Before startup completes, ANY unhandled exception is fatal: with
    /// <see cref="System.Windows.ShutdownMode.OnExplicitShutdown"/> and no window shown yet, swallowing it
    /// would leave a windowless phantom process that never terminates (e.g. a corrupt theme — BUG-032).
    /// </para>
    /// <para>
    /// After startup, the launcher stays alive (a single runtime glitch must not kill the user's
    /// session); the exception is logged. Notifying the user at runtime is a documented gap.
    /// </para>
    /// </summary>
    public static UnhandledExceptionAction Decide(bool startupCompleted)
        => startupCompleted ? UnhandledExceptionAction.KeepAlive : UnhandledExceptionAction.Fatal;
}
