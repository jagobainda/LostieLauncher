package dev.jagoba.lostielauncher.util.log

internal object BuildLoggers {
    fun debug(logcat: Logger, file: Logger): Logger = TeeLogger(logcat, file)

    fun release(logcat: Logger, file: Logger): Logger = TeeLogger(
        FilteredLogger(logcat, LogLevel.ERROR),
        FilteredLogger(file, LogLevel.INFO),
    )
}
