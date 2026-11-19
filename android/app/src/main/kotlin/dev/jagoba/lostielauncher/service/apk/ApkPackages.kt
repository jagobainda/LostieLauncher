package dev.jagoba.lostielauncher.service.apk

import android.content.Context
import android.content.Intent
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

internal data class SignedApk(val version: String, val signers: Set<String>) {
    fun matches(expected: Set<String>?): Boolean = signers.isNotEmpty() && signers == expected
}

internal interface ApkPackages {
    fun installedPackage(packageName: String): SignedApk?

    fun archivePackage(apk: File, packageName: String): SignedApk?

    fun install(apk: File)
}

internal class AndroidApkPackages @Inject constructor(
    @ApplicationContext private val context: Context,
    private val options: ApkInstallOptions,
) : ApkPackages {
    override fun installedPackage(packageName: String): SignedApk? = readPackage(packageName)

    override fun archivePackage(apk: File, packageName: String): SignedApk? = readPackage(packageName, apk)

    override fun install(apk: File) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(FileProvider.getUriForFile(context, options.fileProviderAuthority, apk), APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
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
