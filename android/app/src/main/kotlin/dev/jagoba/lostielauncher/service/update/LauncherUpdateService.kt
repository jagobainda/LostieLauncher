package dev.jagoba.lostielauncher.service.update

import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.LauncherUpdateOptions
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface LauncherUpdateCheckResult {
    data object UpToDate : LauncherUpdateCheckResult

    data class Available(val version: String) : LauncherUpdateCheckResult

    data object Failed : LauncherUpdateCheckResult
}

interface LauncherUpdateService {
    val offeredVersion: StateFlow<String?>
    val download: StateFlow<ApkDownloadState>

    suspend fun refresh()

    suspend fun check(): LauncherUpdateCheckResult

    suspend fun install()

    fun decline()

    fun acknowledgeFailure()
}

@Singleton
internal class DefaultLauncherUpdateService @Inject constructor(
    installers: ApkReleaseInstallerFactory,
    private val options: LauncherUpdateOptions,
    private val logger: Logger,
) : LauncherUpdateService {
    private val installer = installers.create(ApkTarget(options.packageName, options.installerFile, "Launcher"))
    private val gate = Mutex()
    private val mutableOfferedVersion = MutableStateFlow<String?>(null)
    override val offeredVersion: StateFlow<String?> = mutableOfferedVersion.asStateFlow()
    override val download: StateFlow<ApkDownloadState> = installer.download

    @Volatile
    private var offered: String? = null

    @Volatile
    private var answered: String? = null

    override suspend fun refresh() {
        gate.withLock {
            val read = installer.release == null && read(force = false)
            val version = evaluate()
            if (read) logOutcome(version)
        }
    }

    override suspend fun check(): LauncherUpdateCheckResult = gate.withLock {
        if (!read(force = true)) return@withLock LauncherUpdateCheckResult.Failed
        answered = null
        val version = evaluate()
        logOutcome(version)
        if (version == null) LauncherUpdateCheckResult.UpToDate else LauncherUpdateCheckResult.Available(version)
    }

    override suspend fun install() {
        val version = offered ?: return
        if (installer.install()) answer(version)
    }

    override fun decline() {
        val version = offered ?: return
        logger.info("Update to $version declined by user.")
        answer(version)
    }

    override fun acknowledgeFailure() {
        installer.acknowledgeFailure()
        offered?.let(::answer)
    }

    private suspend fun read(force: Boolean): Boolean {
        logger.info("Checking for updates...")
        return installer.loadRelease(options.feedUrl, force) != null
    }

    private suspend fun evaluate(): String? {
        val version = installer.settle(installer.installedVersion(), null)?.versionName
        if (version == null) installer.deleteInstaller()
        offered = version
        publish()
        return version
    }

    private fun answer(version: String) {
        answered = version
        publish()
    }

    private fun publish() {
        mutableOfferedVersion.value = offered?.takeIf { it != answered }
    }

    private fun logOutcome(version: String?) {
        logger.info(if (version == null) "No updates available." else "Update available: $version.")
    }
}
