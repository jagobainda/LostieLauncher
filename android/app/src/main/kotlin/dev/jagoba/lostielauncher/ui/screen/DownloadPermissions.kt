package dev.jagoba.lostielauncher.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
internal fun rememberStorageAccessRequest(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val allFilesAccess = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onResult()
    }
    val writeStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        onResult()
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val opened = context.allFilesAccessIntents().any { intent ->
                try {
                    allFilesAccess.launch(intent)
                    true
                } catch (_: ActivityNotFoundException) {
                    false
                }
            }
            if (!opened) onResult()
        } else {
            writeStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Context.allFilesAccessIntents(): Sequence<Intent> = sequenceOf(
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.fromParts("package", packageName, null)),
    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
)
