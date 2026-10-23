namespace LostieLauncher.Utils;

public enum UnhandledExceptionAction
{
    Fatal,

    KeepAlive
}

public static class UnhandledExceptionPolicy
{
    public static UnhandledExceptionAction Decide(bool startupCompleted)
        => startupCompleted ? UnhandledExceptionAction.KeepAlive : UnhandledExceptionAction.Fatal;
}
