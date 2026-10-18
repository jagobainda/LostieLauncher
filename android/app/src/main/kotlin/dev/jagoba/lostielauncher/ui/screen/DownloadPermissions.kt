package dev.jagoba.lostielauncher.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun rememberDownloadPermissionsRequest(): () -> Unit {
    val context = LocalContext.current
    val storage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val requestStorage = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }
        } else if (!context.isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        requestStorage()
    }
    return {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.isGranted(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestStorage()
        }
    }
}

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
