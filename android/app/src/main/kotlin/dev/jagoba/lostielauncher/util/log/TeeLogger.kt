package dev.jagoba.lostielauncher.util.log

internal class TeeLogger(private val first: Logger, private val second: Logger) : Logger {
    override fun debug(message: String) {
        runCatching { first.debug(message) }
        runCatching { second.debug(message) }
    }

    override fun info(message: String) {
        runCatching { first.info(message) }
        runCatching { second.info(message) }
    }

    override fun error(message: String, throwable: Throwable?) {
        runCatching { first.error(message, throwable) }
        runCatching { second.error(message, throwable) }
    }
}
