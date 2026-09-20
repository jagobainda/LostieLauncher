package dev.jagoba.lostielauncher.util.policy

/** What the user has to be warned about before the launcher closes. */
enum class ShutdownWarning {
    None,
    Download,
    Game,
    Both,
}

/**
 * Decides which exit warning applies, ported from the desktop's
 * `Utils/ShutdownWarningPolicy.cs`.
 *
 * Member order matches the desktop's enum, as the layer rules require.
 */
object ShutdownWarningPolicy {
    fun decide(isDownloading: Boolean, isGameRunning: Boolean): ShutdownWarning = when {
        isDownloading && isGameRunning -> ShutdownWarning.Both
        isDownloading -> ShutdownWarning.Download
        isGameRunning -> ShutdownWarning.Game
        else -> ShutdownWarning.None
    }
}
