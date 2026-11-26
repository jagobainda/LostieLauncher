package dev.jagoba.lostielauncher.util.log

internal enum class LogLevel {
    DEBUG,
    INFO,
    ERROR,
}

internal class FilteredLogger(private val delegate: Logger, private val minLevel: LogLevel) : Logger {
    override fun debug(message: String) {
        if (minLevel <= LogLevel.DEBUG) delegate.debug(message)
    }

    override fun info(message: String) {
        if (minLevel <= LogLevel.INFO) delegate.info(message)
    }

    override fun error(message: String, throwable: Throwable?) {
        delegate.error(message, throwable)
    }
}
