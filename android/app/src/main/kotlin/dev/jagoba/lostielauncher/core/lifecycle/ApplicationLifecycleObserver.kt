package dev.jagoba.lostielauncher.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.presentation.GameAutoUpdateCoordinator
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
internal class ApplicationLifecycleObserver @Inject constructor(
    private val settingsStore: SettingsStore,
    private val logMaintenance: LogMaintenance,
    private val coordinator: LauncherDataCoordinator,
    private val homeRefreshOptions: HomeRefreshOptions,
    private val gameAutoUpdates: GameAutoUpdateCoordinator,
    dispatchers: DispatcherProvider,
) : DefaultLifecycleObserver {
    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var startupStarted = false

    override fun onCreate(owner: LifecycleOwner) {
        scope.launch { logMaintenance.purgeExpired() }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (startupStarted) return
        startupStarted = true
        coordinator.start(scope, settingsStore, homeRefreshOptions)
        gameAutoUpdates.start(scope)
    }

    override fun onStop(owner: LifecycleOwner) {
        flushPendingSettings()
    }

    fun flushPendingSettings() {
        scope.launch { settingsStore.flush() }
    }
}
