package dev.jagoba.lostielauncher.service.apk

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.model.ApkInstallOptions
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val ANDROID_SYSTEM_PACKAGE = "android"

internal data class SignedApk(val version: String, val signers: Set<String>) {
    fun matches(expected: Set<String>?): Boolean = signers.isNotEmpty() && signers == expected
}

internal data class InstallerCandidate(
    val packageName: String,
    val isSystemApp: Boolean,
    val holdsInstallPermission: Boolean,
)

internal fun selectSystemInstallerPackage(candidates: List<InstallerCandidate>): String? {
    val systemCandidates = candidates.filter {
        it.isSystemApp && it.packageName.isNotBlank() && it.packageName != ANDROID_SYSTEM_PACKAGE
    }
    return systemCandidates.firstOrNull { it.holdsInstallPermission }?.packageName
        ?: systemCandidates.firstOrNull()?.packageName
}

internal interface ApkPackages {
    fun installedPackage(packageName: String): SignedApk?

    fun archivePackage(apk: File, packageName: String): SignedApk?

    fun canRequestPackageInstalls(): Boolean

    fun install(apk: File)
}

internal class AndroidApkPackages @Inject constructor(
    @ApplicationContext private val context: Context,
    private val options: ApkInstallOptions,
) : ApkPackages {
    override fun installedPackage(packageName: String): SignedApk? = readPackage(packageName)

    override fun archivePackage(apk: File, packageName: String): SignedApk? = readPackage(packageName, apk)

    override fun canRequestPackageInstalls(): Boolean = context.packageManager.canRequestPackageInstalls()

    override fun install(apk: File) {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(FileProvider.getUriForFile(context, options.fileProviderAuthority, apk), APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        val installerPackage = resolveSystemInstallerPackage(intent)
            ?: throw ActivityNotFoundException("No system package installer available.")
        intent.setPackage(installerPackage)
        context.startActivity(intent)
    }

    private fun resolveSystemInstallerPackage(intent: Intent): String? {
        val manager = context.packageManager
        val flags = PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            manager.queryIntentActivities(intent, flags)
        }
        return selectSystemInstallerPackage(
            activities.mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val appFlags = activity.applicationInfo?.flags ?: 0
                InstallerCandidate(
                    packageName = activity.packageName.orEmpty(),
                    isSystemApp = (appFlags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    holdsInstallPermission = manager.checkPermission(
                        Manifest.permission.INSTALL_PACKAGES,
                        activity.packageName,
                    ) == PackageManager.PERMISSION_GRANTED,
                )
            },
        )
    }

    private fun readPackage(packageName: String, archive: File? = null): SignedApk? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val manager = context.packageManager
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val infoFlags = PackageManager.PackageInfoFlags.of(flags.toLong())
                if (archive == null) {
                    manager.getPackageInfo(packageName, infoFlags)
                } else {
                    manager.getPackageArchiveInfo(archive.path, infoFlags)
                }
            } else if (archive == null) {
                @Suppress("DEPRECATION")
                manager.getPackageInfo(packageName, flags)
            } else {
                @Suppress("DEPRECATION")
                manager.getPackageArchiveInfo(archive.path, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        if (info == null || info.packageName != packageName) return null
        return SignedApk(info.versionName.orEmpty(), info.signerDigests())
    }

    private fun PackageInfo.signerDigests(): Set<String> {
        val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            signatures
        }
        return current.orEmpty().mapTo(mutableSetOf()) { signer ->
            MessageDigest.getInstance("SHA-256").digest(signer.toByteArray()).toHexString()
        }
    }
}
