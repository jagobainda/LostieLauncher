package dev.jagoba.lostielauncher.util.policy

enum class UnhandledExceptionAction {
    Fatal,
    KeepAlive,
}

object UnhandledExceptionPolicy {
    fun decide(startupCompleted: Boolean): UnhandledExceptionAction =
        if (startupCompleted) UnhandledExceptionAction.KeepAlive else UnhandledExceptionAction.Fatal
}
