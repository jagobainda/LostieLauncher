package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.apk.ApkReleaseDto
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.service.apk.SignedApk
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.ApkAbiPolicy
import dev.jagoba.lostielauncher.util.policy.ApkAsset
import dev.jagoba.lostielauncher.util.policy.ApkAssetSelection
import dev.jagoba.lostielauncher.util.policy.ApkVersionPolicy
import dev.jagoba.lostielauncher.util.policy.KirinVersionPolicy
import dev.jagoba.lostielauncher.util.version.VersionUtils
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface KirinSetupService {
    val status: StateFlow<KirinStatus?>
    val download: StateFlow<ApkDownloadState>

    suspend fun refresh()

    suspend fun isOfficialKirinInstalled(): Boolean

    suspend fun install()

    fun acknowledgeFailure()
}

@Serializable
private data class KirinRequirementsDto(val minimumVersion: String, val releaseFeedUrl: String)

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
private data class GitHubAssetDto(val name: String, @SerialName("browser_download_url") val downloadUrl: String)

@Singleton
internal class DefaultKirinSetupService @Inject constructor(
    private val remoteJson: RemoteJson,
    installers: ApkReleaseInstallerFactory,
    private val minimumVersionStore: KirinMinimumVersionStore,
    kirin: KirinOptions,
    private val options: KirinSetupOptions,
    private val clock: Clock,
    private val logger: Logger,
) : KirinSetupService {
    private val installer = installers.create(
        ApkTarget(kirin.packageName, options.installerFile, "Kirin", options.signerSha256, options.maxInstallerBytes),
    )
    private val refreshGate = Mutex()
    private val mutableStatus = MutableStateFlow<KirinStatus?>(null)
    override val status: StateFlow<KirinStatus?> = mutableStatus.asStateFlow()
    override val download: StateFlow<ApkDownloadState> = installer.download

    private var persistedMinimumLoaded = false
    private var persistedMinimumVersion: String? = null
    private var requirements: KirinRequirementsDto? = null
    private var incompatibleReleaseVersion: String? = null
    private var nextFeedAttemptAt: Instant? = null

    private val fallbackMinimumVersion: String
        get() {
            val persisted = persistedMinimumVersion
            return if (persisted != null && VersionUtils.isNewerVersion(persisted, options.fallbackMinimumVersion)) {
                persisted
            } else {
                options.fallbackMinimumVersion
            }
        }

    private val minimumVersion get() = requirements?.minimumVersion ?: fallbackMinimumVersion

    override suspend fun refresh() {
        refreshGate.withLock {
            if (!persistedMinimumLoaded) {
                persistedMinimumVersion = minimumVersionStore.read()
                persistedMinimumLoaded = true
            }
            updateStatus()
            if (requirements == null) {
                val fetched = remoteJson.fetch(
                    options.requirementsUrl,
                    KirinRequirementsDto.serializer(),
                    "Kirin requirements",
                )
                if (fetched != null) {
                    requirements = fetched
                    if (VersionUtils.isValidVersion(fetched.minimumVersion)) {
                        persistedMinimumVersion = fetched.minimumVersion
                        minimumVersionStore.write(fetched.minimumVersion)
                    }
                }
            }
            requirements?.releaseFeedUrl?.let { loadRelease(it) }
            val installedVersion = updateStatus()
            val unavailableState = incompatibleReleaseVersion
                ?.takeIf { ApkVersionPolicy.isOffered(it, installedVersion, minimumVersion) }
                ?.let { ApkDownloadState.IncompatibleAbi }
                ?: ApkDownloadState.Unavailable
            installer.settle(installedVersion, minimumVersion, unavailableState)
        }
    }

    override suspend fun install() {
        if (status.value != KirinStatus.UntrustedSigner) installer.install()
    }

    override suspend fun isOfficialKirinInstalled(): Boolean = isOfficial(installer.installedPackage())

    private fun isOfficial(installed: SignedApk?): Boolean = installed?.matches(setOf(options.signerSha256)) == true

    override fun acknowledgeFailure() {
        installer.acknowledgeFailure()
    }

    private suspend fun loadRelease(feedUrl: String) {
        if (installer.release != null) return
        val now = clock.instant()
        val retryAt = nextFeedAttemptAt
        if (retryAt != null && now < retryAt) return

        val fetched = remoteJson.fetch(feedUrl, GitHubReleaseDto.serializer(), "Kirin release feed")
        if (fetched == null) {
            nextFeedAttemptAt = now.plus(options.feedRetryInterval.toJavaDuration())
            return
        }
        if (fetched.draft || fetched.prerelease) {
            logger.info("Kirin release ${fetched.tagName} ignored because it is a draft or prerelease.")
            incompatibleReleaseVersion = null
            nextFeedAttemptAt = now.plus(options.feedRetryInterval.toJavaDuration())
            return
        }

        val assets = fetched.assets.map { ApkAsset(it.name, it.downloadUrl) }
        when (val selection = ApkAbiPolicy.select(assets, options.supportedAbis)) {
            is ApkAssetSelection.Selected -> {
                val accepted = installer.acceptRelease(ApkReleaseDto(fetched.tagName, selection.asset.downloadUrl))
                incompatibleReleaseVersion = null
                nextFeedAttemptAt = if (accepted != null) null else now.plus(options.feedRetryInterval.toJavaDuration())
            }

            ApkAssetSelection.IncompatibleAbi -> {
                logger.info("Kirin release ${fetched.tagName} has no APK compatible with ${options.supportedAbis}.")
                incompatibleReleaseVersion = fetched.tagName
                nextFeedAttemptAt = now.plus(options.feedRetryInterval.toJavaDuration())
            }

            ApkAssetSelection.NoApkAsset -> {
                logger.info("Kirin release ${fetched.tagName} has no APK asset.")
                incompatibleReleaseVersion = null
                nextFeedAttemptAt = now.plus(options.feedRetryInterval.toJavaDuration())
            }
        }
    }

    private suspend fun updateStatus(): String? {
        val installed = installer.installedPackage()
        val installedVersion = installed?.version
        val status = if (installed != null && !isOfficial(installed)) {
            KirinStatus.UntrustedSigner
        } else {
            KirinVersionPolicy.status(installedVersion, minimumVersion, installer.release?.versionName)
        }
        if (mutableStatus.value != status) logger.info("Kirin status: $status.")
        mutableStatus.value = status
        if (status == KirinStatus.Ready) installer.deleteInstaller()
        return installedVersion
    }
}
