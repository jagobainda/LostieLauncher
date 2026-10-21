package dev.jagoba.lostielauncher.service.apk

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadTransferRequest
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import dev.jagoba.lostielauncher.util.policy.ApkVersionPolicy
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
internal data class ApkReleaseDto(val versionName: String, val downloadUrl: String)

internal class ApkReleaseInstallerFactory @Inject constructor(
    private val remoteJson: RemoteJson,
    private val transfer: DownloadTransfer,
    private val packages: ApkPackages,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {
    fun create(target: ApkTarget) = ApkReleaseInstaller(target, remoteJson, transfer, packages, dispatchers, logger)
}

internal class ApkReleaseInstaller(
    private val target: ApkTarget,
    private val remoteJson: RemoteJson,
    private val transfer: DownloadTransfer,
    private val packages: ApkPackages,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {
    private val mutableDownload = MutableStateFlow<ApkDownloadState>(ApkDownloadState.Checking)
    val download: StateFlow<ApkDownloadState> = mutableDownload.asStateFlow()

    @Volatile
    var release: ApkReleaseDto? = null
        private set

    @Volatile
    private var offered: ApkReleaseDto? = null

    suspend fun loadRelease(feedUrl: String, force: Boolean = false): ApkReleaseDto? {
        if (release == null || force) {
            release = remoteJson.fetch(feedUrl, ApkReleaseDto.serializer(), "${target.label} release feed")
                ?.takeIf { HttpsUrls.parseOrNull(it.downloadUrl) != null }
                ?.also { logger.info("${target.label} release feed offers ${it.versionName}.") }
        }
        return release
    }

    suspend fun installedVersion(): String? = withContext(dispatchers.io) {
        packages.installedVersion(target.packageName)
    }

    fun settle(installedVersion: String?, minimumVersion: String?): ApkReleaseDto? {
        offered = release?.takeIf { ApkVersionPolicy.isOffered(it.versionName, installedVersion, minimumVersion) }
        mutableDownload.update { current ->
            if (current is ApkDownloadState.Downloading || current == ApkDownloadState.Failed) current else idle()
        }
        return offered
    }

    suspend fun install(): Boolean {
        val release = offered ?: return false
        if (!mutableDownload.compareAndSet(ApkDownloadState.Available, ApkDownloadState.Downloading(0))) return false
        val opened = try {
            openInstaller(release)
        } catch (cancelled: CancellationException) {
            mutableDownload.value = idle()
            throw cancelled
        }
        mutableDownload.value = if (opened) idle() else ApkDownloadState.Failed
        return opened
    }

    fun acknowledgeFailure() {
        mutableDownload.compareAndSet(ApkDownloadState.Failed, idle())
    }

    suspend fun deleteInstaller() {
        withContext(dispatchers.io) { target.installerFile.parentFile?.deleteRecursively() }
    }

    private fun idle(): ApkDownloadState =
        if (offered == null) ApkDownloadState.Unavailable else ApkDownloadState.Available

    private suspend fun openInstaller(release: ApkReleaseDto): Boolean {
        val apk = target.installerFile
        if (!isInstallable(apk, release.versionName)) {
            logger.info("${target.label} ${release.versionName} download started.")
            val result = transfer.download(DownloadTransferRequest(release.downloadUrl, apk.path)) {
                mutableDownload.value = ApkDownloadState.Downloading(it.percent.roundToInt())
            }
            if (result != DownloadTransferResult.Success) {
                logger.error("${target.label} download failed: $result.")
                return false
            }
            if (!isInstallable(apk, release.versionName)) {
                logger.error("The downloaded file is not ${target.label} ${release.versionName} or later.")
                withContext(dispatchers.io) { apk.delete() }
                return false
            }
        }
        return try {
            packages.install(apk)
            logger.info("${target.label} installer opened.")
            true
        } catch (error: Exception) {
            logger.error("${target.label} installer could not be opened.", error)
            false
        }
    }

    private suspend fun isInstallable(apk: File, version: String): Boolean = withContext(dispatchers.io) {
        apk.isFile && ApkVersionPolicy.isSupported(packages.archiveVersion(apk, target.packageName), version)
    }
}
