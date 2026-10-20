package dev.jagoba.lostielauncher.service.game

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import java.io.File
import javax.inject.Inject

private const val APK_MIME_TYPE = "application/vnd.android.package-archive"

internal interface KirinPackages {
    fun installedVersion(): String?

    fun archiveVersion(apk: File): String?

    fun install(apk: File)
}

internal class AndroidKirinPackages @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kirin: KirinOptions,
    private val setup: KirinSetupOptions,
) : KirinPackages {
    override fun installedVersion(): String? = try {
        packageInfo().versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    override fun archiveVersion(apk: File): String? =
        archiveInfo(apk)?.takeIf { it.packageName == kirin.packageName }?.versionName

    override fun install(apk: File) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(FileProvider.getUriForFile(context, setup.fileProviderAuthority, apk), APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun packageInfo(): PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(kirin.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(kirin.packageName, 0)
    }

    private fun archiveInfo(apk: File): PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageArchiveInfo(apk.path, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageArchiveInfo(apk.path, 0)
    }
}
