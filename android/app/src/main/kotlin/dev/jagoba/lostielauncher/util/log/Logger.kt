package dev.jagoba.lostielauncher.util.log

interface Logger {
    fun debug(message: String)

    fun info(message: String)

    fun error(message: String, throwable: Throwable? = null)
}
