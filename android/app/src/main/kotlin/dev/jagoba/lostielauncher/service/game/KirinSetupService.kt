package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.KirinDownloadState
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.cdn.ContentClient
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadTransferRequest
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import dev.jagoba.lostielauncher.util.policy.KirinVersionPolicy
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

interface KirinSetupService {
    val status: StateFlow<KirinStatus?>
    val download: StateFlow<KirinDownloadState>

    suspend fun refresh()

    suspend fun install()

    fun acknowledgeFailure()
}

@Serializable
private data class KirinRequirementsDto(val minimumVersion: String, val releaseFeedUrl: String)

@Serializable
private data class KirinReleaseDto(val versionName: String, val downloadUrl: String)

@Singleton
internal class DefaultKirinSetupService @Inject constructor(
    @ContentClient private val client: OkHttpClient,
    private val transfer: DownloadTransfer,
    private val packages: KirinPackages,
    private val options: KirinSetupOptions,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : KirinSetupService {
    private val refreshGate = Mutex()
    private val mutableStatus = MutableStateFlow<KirinStatus?>(null)
    private val mutableDownload = MutableStateFlow<KirinDownloadState>(KirinDownloadState.Checking)
    override val status: StateFlow<KirinStatus?> = mutableStatus.asStateFlow()
    override val download: StateFlow<KirinDownloadState> = mutableDownload.asStateFlow()

    @Volatile
    private var requirements: KirinRequirementsDto? = null

    @Volatile
    private var release: KirinReleaseDto? = null

    @Volatile
    private var installedVersion: String? = null

    private val minimumVersion get() = requirements?.minimumVersion ?: options.fallbackMinimumVersion

    override suspend fun refresh() = refreshGate.withLock {
        updateStatus()
        if (requirements == null) {
            requirements = fetch<KirinRequirementsDto>(options.requirementsUrl, "Kirin requirements")
        }
        val feedUrl = requirements?.releaseFeedUrl
        if (release == null && feedUrl != null) {
            release = fetch<KirinReleaseDto>(feedUrl, "Kirin release feed")
                ?.takeIf { HttpsUrls.parseOrNull(it.downloadUrl) != null }
                ?.also { logger.info("Kirin release feed offers ${it.versionName}.") }
        }
        updateStatus()
        mutableDownload.update { current ->
            if (current is KirinDownloadState.Downloading || current == KirinDownloadState.Failed) current else idle()
        }
    }

    override suspend fun install() {
        val release = offeredRelease() ?: return
        if (!mutableDownload.compareAndSet(KirinDownloadState.Available, KirinDownloadState.Downloading(0))) return
        mutableDownload.value = if (openInstaller(release)) idle() else KirinDownloadState.Failed
    }

    override fun acknowledgeFailure() {
        mutableDownload.compareAndSet(KirinDownloadState.Failed, idle())
    }

    private suspend fun updateStatus() {
        installedVersion = withContext(dispatchers.io) { packages.installedVersion() }
        val status = KirinVersionPolicy.status(installedVersion, minimumVersion, release?.versionName)
        if (mutableStatus.value != status) logger.info("Kirin status: $status.")
        mutableStatus.value = status
        if (status ==
            KirinStatus.Ready
        ) {
            withContext(dispatchers.io) { options.installerFile.parentFile?.deleteRecursively() }
        }
    }

    private fun offeredRelease(): KirinReleaseDto? =
        release?.takeIf { KirinVersionPolicy.isOffered(it.versionName, installedVersion, minimumVersion) }

    private fun idle(): KirinDownloadState =
        if (offeredRelease() == null) KirinDownloadState.Unavailable else KirinDownloadState.Available

    private suspend inline fun <reified T> fetch(url: String, label: String): T? = withContext(dispatchers.io) {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("$label returned HTTP ${response.code}.")
                json.decodeFromString<T>(response.body.string())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("$label could not be read.", error)
            null
        }
    }

    private suspend fun openInstaller(release: KirinReleaseDto): Boolean {
        val apk = options.installerFile
        if (!isInstallable(apk, release.versionName)) {
            logger.info("Kirin ${release.versionName} download started.")
            val result = transfer.download(DownloadTransferRequest(release.downloadUrl, apk.path)) {
                mutableDownload.value = KirinDownloadState.Downloading(it.percent.roundToInt())
            }
            if (result != DownloadTransferResult.Success) {
                logger.error("Kirin download failed: $result.")
                return false
            }
            if (!isInstallable(apk, release.versionName)) {
                logger.error("The downloaded file is not Kirin ${release.versionName} or later.")
                withContext(dispatchers.io) { apk.delete() }
                return false
            }
        }
        return try {
            packages.install(apk)
            logger.info("Kirin installer opened.")
            true
        } catch (error: Exception) {
            logger.error("Kirin installer could not be opened.", error)
            false
        }
    }

    private suspend fun isInstallable(apk: File, version: String): Boolean = withContext(dispatchers.io) {
        apk.isFile && KirinVersionPolicy.isSupported(packages.archiveVersion(apk), version)
    }
}
