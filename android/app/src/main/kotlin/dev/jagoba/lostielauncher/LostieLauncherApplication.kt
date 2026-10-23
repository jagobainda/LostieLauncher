package dev.jagoba.lostielauncher

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import dev.jagoba.lostielauncher.core.lifecycle.ApplicationLifecycleObserver
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.log.UncaughtExceptionLogger
import javax.inject.Inject

@HiltAndroidApp
class LostieLauncherApplication : Application() {
    @Inject
    internal lateinit var lifecycleObserver: ApplicationLifecycleObserver

    @Inject
    internal lateinit var logger: Logger

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtExceptionLogger(logger, Thread.getDefaultUncaughtExceptionHandler()),
        )
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
}
