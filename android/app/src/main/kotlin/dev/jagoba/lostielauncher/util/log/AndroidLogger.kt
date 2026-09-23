package dev.jagoba.lostielauncher.util.log

import android.util.Log

/**
 * [Logger] on top of Logcat.
 *
 * The desktop writes to rolling files under the user's profile and keeps six
 * months of them; on Android that is the platform's job, so this adapter stays
 * thin. If the port ever needs the launcher's own log file — for a "send
 * diagnostics" action, say — it becomes a second [Logger] beside this one
 * rather than a change to it.
 */
class AndroidLogger : Logger {
    override fun debug(message: String) {
        Log.d(TAG, message)
    }

    override fun info(message: String) {
        Log.i(TAG, message)
    }

    override fun error(message: String, throwable: Throwable?) {
        Log.e(TAG, message, throwable)
    }

    private companion object {
        /** Logcat truncates tags; this one is short enough to survive it. */
        const val TAG = "LostieLauncher"
    }
}
