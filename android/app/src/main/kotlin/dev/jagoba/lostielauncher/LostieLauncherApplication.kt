package dev.jagoba.lostielauncher

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import dev.jagoba.lostielauncher.core.lifecycle.ApplicationLifecycleObserver
import javax.inject.Inject

@HiltAndroidApp
class LostieLauncherApplication : Application() {
    @Inject
    internal lateinit var lifecycleObserver: ApplicationLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }
}
