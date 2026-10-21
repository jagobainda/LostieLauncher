package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.KirinVersionPolicy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

interface KirinSetupService {
    val status: StateFlow<KirinStatus?>
    val download: StateFlow<ApkDownloadState>

    suspend fun refresh()

    suspend fun install()

    fun acknowledgeFailure()
}

@Serializable
private data class KirinRequirementsDto(val minimumVersion: String, val releaseFeedUrl: String)

@Singleton
internal class DefaultKirinSetupService @Inject constructor(
    private val remoteJson: RemoteJson,
    installers: ApkReleaseInstallerFactory,
    kirin: KirinOptions,
    private val options: KirinSetupOptions,
    private val logger: Logger,
) : KirinSetupService {
    private val installer = installers.create(ApkTarget(kirin.packageName, options.installerFile, "Kirin"))
    private val refreshGate = Mutex()
    private val mutableStatus = MutableStateFlow<KirinStatus?>(null)
    override val status: StateFlow<KirinStatus?> = mutableStatus.asStateFlow()
    override val download: StateFlow<ApkDownloadState> = installer.download

    @Volatile
    private var requirements: KirinRequirementsDto? = null

    @Volatile
    private var installedVersion: String? = null

    private val minimumVersion get() = requirements?.minimumVersion ?: options.fallbackMinimumVersion

    override suspend fun refresh() {
        refreshGate.withLock {
            updateStatus()
            if (requirements == null) {
                requirements = remoteJson.fetch(
                    options.requirementsUrl,
                    KirinRequirementsDto.serializer(),
                    "Kirin requirements",
                )
            }
            requirements?.releaseFeedUrl?.let { installer.loadRelease(it) }
            updateStatus()
            installer.settle(installedVersion, minimumVersion)
        }
    }

    override suspend fun install() {
        installer.install()
    }

    override fun acknowledgeFailure() {
        installer.acknowledgeFailure()
    }

    private suspend fun updateStatus() {
        installedVersion = installer.installedVersion()
        val status = KirinVersionPolicy.status(installedVersion, minimumVersion, installer.release?.versionName)
        if (mutableStatus.value != status) logger.info("Kirin status: $status.")
        mutableStatus.value = status
        if (status == KirinStatus.Ready) installer.deleteInstaller()
    }
}
