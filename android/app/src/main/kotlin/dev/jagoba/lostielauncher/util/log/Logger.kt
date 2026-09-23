package dev.jagoba.lostielauncher.util.log

/**
 * The logging seam.
 *
 * It exists so that the degradation rules the launcher lives by — never swallow
 * a failure silently, log it and carry on with an empty result — can be written
 * in a service and asserted in a test, without `android.util.Log` (a static
 * that is a no-op off-device) in the way.
 *
 * Messages are English and say what happened, not that a method ran.
 */
interface Logger {
    fun debug(message: String)

    fun info(message: String)

    fun error(message: String, throwable: Throwable? = null)
}
