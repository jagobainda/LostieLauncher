package dev.jagoba.lostielauncher.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.service.download.AppForegroundState
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import dev.jagoba.lostielauncher.service.presentation.GameAutoUpdateCoordinator
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import dev.jagoba.lostielauncher.util.log.LogMaintenance
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
internal class ApplicationLifecycleObserver @Inject constructor(
    private val settingsStore: SettingsStore,
    private val logMaintenance: LogMaintenance,
    private val coordinator: LauncherDataCoordinator,
    private val gameAutoUpdates: GameAutoUpdateCoordinator,
    private val installation: GameInstallationService,
    private val kirin: KirinSetupService,
    private val launcherUpdate: LauncherUpdateService,
    private val logger: Logger,
    dispatchers: DispatcherProvider,
) : DefaultLifecycleObserver,
    AppForegroundState {
    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private var startupStarted = false

    @Volatile
    override var isInForeground: Boolean = false
        private set

    override fun onCreate(owner: LifecycleOwner) {
        scope.launch {
            logMaintenance.purgeExpired()
            logger.info("Application started.")
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        isInForeground = true
        coordinator.setVisible(true)
        scope.launch { installation.refreshInstalled() }
        if (startupStarted) return
        startupStarted = true
        coordinator.start(scope)
        gameAutoUpdates.start(scope)
        scope.launch {
            if (!settingsStore.settings.first().hasSeenWelcome) {
                settingsStore.settings.first { it.hasSeenWelcome }
                kirin.refresh(allowReleaseFeed = true)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        refreshPackageInstallers()
    }

    fun refreshPackageInstallers() {
        scope.launch { kirin.refresh(allowReleaseFeed = settingsStore.settings.first().hasSeenWelcome) }
        scope.launch { launcherUpdate.refresh() }
    }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground = false
        coordinator.setVisible(false)
        flushPendingSettings()
    }

    fun flushPendingSettings() {
        scope.launch { settingsStore.flush() }
    }
}
