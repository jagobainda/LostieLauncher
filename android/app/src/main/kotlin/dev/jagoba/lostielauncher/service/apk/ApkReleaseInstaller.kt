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
import kotlinx.coroutines.flow.updateAndGet
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
    var offered: ApkReleaseDto? = null
        private set

    @Volatile
    private var unavailableState: ApkDownloadState = ApkDownloadState.Unavailable

    suspend fun loadRelease(feedUrl: String, force: Boolean = false): ApkReleaseDto? {
        if (release == null || force) {
            acceptRelease(
                remoteJson.fetch(
                    feedUrl,
                    ApkReleaseDto.serializer(),
                    "${target.label} release feed",
                    allowedHosts = target.allowedFeedHosts,
                ),
            )
        }
        return release
    }

    fun acceptRelease(candidate: ApkReleaseDto?): ApkReleaseDto? {
        val accepted = candidate?.takeIf { HttpsUrls.isAllowedHttps(it.downloadUrl, target.allowedDownloadHosts) }
        if (candidate != null && accepted == null) {
            logger.error("${target.label} release download URL was rejected.")
        }
        release = accepted?.also { logger.info("${target.label} release feed offers ${it.versionName}.") }
        return release
    }

    suspend fun installedPackage(): SignedApk? = withContext(dispatchers.io) {
        packages.installedPackage(target.packageName)
    }

    fun settle(
        installedVersion: String?,
        minimumVersion: String?,
        unavailableState: ApkDownloadState = ApkDownloadState.Unavailable,
        settleInstalling: Boolean = true,
    ): ApkReleaseDto? {
        this.unavailableState = unavailableState
        val previous = mutableDownload.value
        offered = release?.takeIf { ApkVersionPolicy.isOffered(it.versionName, installedVersion, minimumVersion) }
        val updated = mutableDownload.updateAndGet { current ->
            when (current) {
                is ApkDownloadState.Downloading -> current

                ApkDownloadState.Failed,
                ApkDownloadState.PermissionRequired,
                ApkDownloadState.Cancelled,
                -> if (offered == null) unavailableState else current

                ApkDownloadState.Installing -> when {
                    offered == null -> unavailableState
                    settleInstalling -> ApkDownloadState.Cancelled
                    else -> current
                }

                ApkDownloadState.Checking,
                ApkDownloadState.Unavailable,
                ApkDownloadState.IncompatibleAbi,
                ApkDownloadState.Available,
                -> idle()
            }
        }
        if (previous == ApkDownloadState.Installing && updated == ApkDownloadState.Cancelled) {
            logger.info("${target.label} installation was cancelled or did not complete.")
        } else if (previous == ApkDownloadState.Installing && offered == null) {
            logger.info("${target.label} installation completed.")
        }
        return offered
    }

    suspend fun install(): Boolean {
        val release = offered ?: return false
        if (!packages.canRequestPackageInstalls()) {
            if (transitionTo(ApkDownloadState.PermissionRequired, *STARTABLE_STATES)) {
                logger.info("${target.label} installation waiting for unknown-sources permission.")
            }
            return false
        }
        if (!transitionTo(ApkDownloadState.Downloading(0), *STARTABLE_STATES)) return false
        val outcome = try {
            openInstaller(release)
        } catch (cancelled: CancellationException) {
            mutableDownload.value = idle()
            throw cancelled
        }
        mutableDownload.value = outcome
        return outcome == ApkDownloadState.Installing
    }

    suspend fun onPermissionResult(): Boolean {
        if (mutableDownload.value != ApkDownloadState.PermissionRequired) return false
        return if (packages.canRequestPackageInstalls()) {
            install()
        } else {
            dismissPermissionRequest()
            false
        }
    }

    fun dismissPermissionRequest() {
        mutableDownload.compareAndSet(ApkDownloadState.PermissionRequired, idle())
    }

    fun dismissCancelled() {
        mutableDownload.compareAndSet(ApkDownloadState.Cancelled, idle())
    }

    fun acknowledgeFailure() {
        mutableDownload.compareAndSet(ApkDownloadState.Failed, idle())
    }

    suspend fun deleteInstaller() {
        withContext(dispatchers.io) { target.installerFile.parentFile?.deleteRecursively() }
    }

    private fun idle(): ApkDownloadState = if (offered == null) unavailableState else ApkDownloadState.Available

    private fun transitionTo(next: ApkDownloadState, vararg allowed: ApkDownloadState): Boolean {
        while (true) {
            val current = mutableDownload.value
            if (current !in allowed) return false
            if (mutableDownload.compareAndSet(current, next)) return true
        }
    }

    private suspend fun openInstaller(release: ApkReleaseDto): ApkDownloadState {
        val apk = target.installerFile
        if (!isInstallable(apk, release.versionName)) {
            logger.info("${target.label} ${release.versionName} download started.")
            val result = transfer.download(
                DownloadTransferRequest(release.downloadUrl, apk.path, target.maxBytes, target.allowedDownloadHosts),
            ) {
                mutableDownload.value = ApkDownloadState.Downloading(it.percent.roundToInt())
            }
            if (result != DownloadTransferResult.Success) {
                logger.error("${target.label} download failed: $result.")
                return ApkDownloadState.Failed
            }
            if (!isInstallable(apk, release.versionName)) {
                logger.error("The downloaded ${target.label} APK failed package, version or signer validation.")
                withContext(dispatchers.io) { apk.delete() }
                return ApkDownloadState.Failed
            }
        }
        if (!packages.canRequestPackageInstalls()) {
            logger.info("${target.label} installation waiting for unknown-sources permission.")
            return ApkDownloadState.PermissionRequired
        }
        return try {
            packages.install(apk)
            logger.info("${target.label} installer opened.")
            ApkDownloadState.Installing
        } catch (error: Exception) {
            logger.error("${target.label} installer could not be opened.", error)
            ApkDownloadState.Failed
        }
    }

    private suspend fun isInstallable(apk: File, version: String): Boolean = withContext(dispatchers.io) {
        if (!apk.isFile) return@withContext false
        if (target.maxBytes != null && apk.length() > target.maxBytes) return@withContext false
        val archive = packages.archivePackage(apk, target.packageName) ?: return@withContext false
        val expected = target.pinnedSigner?.let { setOf(it) } ?: packages.installedPackage(target.packageName)?.signers
        archive.matches(expected) && ApkVersionPolicy.isSupported(archive.version, version)
    }

    private companion object {
        val STARTABLE_STATES = arrayOf(
            ApkDownloadState.Available,
            ApkDownloadState.PermissionRequired,
            ApkDownloadState.Cancelled,
        )
    }
}
