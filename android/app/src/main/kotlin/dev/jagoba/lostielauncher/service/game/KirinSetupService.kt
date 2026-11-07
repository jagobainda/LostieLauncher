package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.apk.ApkReleaseDto
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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface KirinSetupService {
    val status: StateFlow<KirinStatus?>
    val download: StateFlow<ApkDownloadState>

    suspend fun refresh()

    suspend fun install()

    fun acknowledgeFailure()
}

@Serializable
private data class KirinRequirementsDto(val minimumVersion: String, val releaseFeedUrl: String)

private object GitHubReleaseFormat : JsonTransformingSerializer<ApkReleaseDto>(ApkReleaseDto.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val release = element.jsonObject
        val apk = release.getValue("assets").jsonArray.map { it.jsonObject }
            .first { it.getValue("name").jsonPrimitive.content.endsWith(".apk") }
        return buildJsonObject {
            put("versionName", release.getValue("tag_name"))
            put("downloadUrl", apk.getValue("browser_download_url"))
        }
    }
}

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
            requirements?.releaseFeedUrl?.let { installer.loadRelease(it, format = GitHubReleaseFormat) }
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
