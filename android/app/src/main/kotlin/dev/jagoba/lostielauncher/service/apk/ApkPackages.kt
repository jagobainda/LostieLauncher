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
import javax.inject.Inject

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

internal interface ApkPackages {
    fun installedVersion(packageName: String): String?

    fun archiveVersion(apk: File, packageName: String): String?

    fun install(apk: File)
}

internal class AndroidApkPackages @Inject constructor(
    @ApplicationContext private val context: Context,
    private val options: ApkInstallOptions,
) : ApkPackages {
    override fun installedVersion(packageName: String): String? = try {
        packageInfo(packageName).versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    override fun archiveVersion(apk: File, packageName: String): String? =
        archiveInfo(apk)?.takeIf { it.packageName == packageName }?.versionName

    override fun install(apk: File) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(FileProvider.getUriForFile(context, options.fileProviderAuthority, apk), APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun packageInfo(packageName: String): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }

    private fun archiveInfo(apk: File): PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageArchiveInfo(apk.path, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageArchiveInfo(apk.path, 0)
    }
}
