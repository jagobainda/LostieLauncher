package dev.jagoba.lostielauncher.util.policy

enum class ShutdownWarning {
    None,
    Download,
    Game,
    Both,
}

object ShutdownWarningPolicy {
    fun decide(isDownloading: Boolean, isGameRunning: Boolean): ShutdownWarning = when {
        isDownloading && isGameRunning -> ShutdownWarning.Both
        isDownloading -> ShutdownWarning.Download
        isGameRunning -> ShutdownWarning.Game
        else -> ShutdownWarning.None
    }
}
