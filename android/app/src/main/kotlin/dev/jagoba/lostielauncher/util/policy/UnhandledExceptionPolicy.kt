package dev.jagoba.lostielauncher.util.policy

/** What to do with an exception nothing else handled. */
enum class UnhandledExceptionAction {
    Fatal,
    KeepAlive,
}

/**
 * Decides whether an unhandled exception ends the app, ported from the
 * desktop's `Utils/UnhandledExceptionPolicy.cs`.
 *
 * The asymmetry is the whole content: a failure **before** startup completed
 * happens with nothing on screen, so swallowing it leaves a process the user
 * can neither see nor close, and the only honest outcome is to stop. After
 * startup there is a window full of state, and a single runtime glitch must not
 * take the user's session with it.
 *
 * Member order matches the desktop's enum, as the layer rules require.
 */
object UnhandledExceptionPolicy {
    fun decide(startupCompleted: Boolean): UnhandledExceptionAction =
        if (startupCompleted) UnhandledExceptionAction.KeepAlive else UnhandledExceptionAction.Fatal
}
