package dev.jagoba.lostielauncher.ui.screen

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

internal class DownloadPermissions(val requestPermission: () -> Unit, val openSettings: () -> Unit)

@Composable
internal fun rememberNotificationPermissionRequest(
    onResult: (permanentlyDenied: Boolean) -> Unit,
): DownloadPermissions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onResult(false)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val permanentlyDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity.isPermanentlyDenied(Manifest.permission.POST_NOTIFICATIONS, granted)
        onResult(permanentlyDenied)
    }
    return DownloadPermissions(
        requestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onResult(false)
            }
        },
        openSettings = {
            settingsLauncher.launchFirst(context.appNotificationSettingsIntents()) { onResult(false) }
        },
    )
}

@Composable
internal fun rememberStorageAccessRequest(onResult: (permanentlyDenied: Boolean) -> Unit): DownloadPermissions {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onResult(false)
    }
    val writeStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onResult(activity.isPermanentlyDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE, granted))
    }
    return DownloadPermissions(
        requestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                settingsLauncher.launchFirst(context.allFilesAccessIntents()) { onResult(false) }
            } else {
                writeStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        },
        openSettings = {
            settingsLauncher.launchFirst(context.appDetailsSettingsIntents()) { onResult(false) }
        },
    )
}

private fun Activity?.isPermanentlyDenied(permission: String, granted: Boolean): Boolean =
    !granted && this != null && !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

private fun ManagedActivityResultLauncher<Intent, *>.launchFirst(
    intents: Sequence<Intent>,
    onUnavailable: () -> Unit,
) {
    val opened = intents.any { intent ->
        try {
            launch(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
    if (!opened) onUnavailable()
}

@RequiresApi(Build.VERSION_CODES.R)
private fun Context.allFilesAccessIntents(): Sequence<Intent> = sequenceOf(
    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.fromParts("package", packageName, null)),
    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
)

private fun Context.appDetailsSettingsIntents(): Sequence<Intent> = sequenceOf(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    Intent(Settings.ACTION_APPLICATION_SETTINGS),
)

private fun Context.appNotificationSettingsIntents(): Sequence<Intent> = sequenceOf(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
) + appDetailsSettingsIntents()
