package dev.jagoba.lostielauncher.util.log

internal class UncaughtExceptionLogger(
    private val logger: Logger,
    private val next: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            logger.error("Unhandled exception on thread ${thread.name}.", throwable)
        } catch (ignored: Throwable) {
        }
        next?.uncaughtException(thread, throwable)
    }
}
