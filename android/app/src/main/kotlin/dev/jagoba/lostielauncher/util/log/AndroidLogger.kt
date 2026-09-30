package dev.jagoba.lostielauncher.util.log

import android.util.Log

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
        const val TAG = "LostieLauncher"
    }
}
