package dev.jagoba.lostielauncher.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.viewmodel.DownloadHarnessViewModel
import java.util.Locale

@Composable
fun DownloadHarnessScreen(modifier: Modifier = Modifier, viewModel: DownloadHarnessViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.start()
    }
    val start = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.start()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(LauncherSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Card),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val game = state.game
        if (game == null) {
            if (state.catalogueLoaded) Text(strings.downloadErrorTitle) else CircularProgressIndicator()
            return@Column
        }
        Text(game.name, fontSize = LauncherType.HeadingSize, fontWeight = LauncherType.Bold)
        Text(game.version, fontSize = LauncherType.BodySize)
        val download = state.download
        if (download != null) {
            LinearProgressIndicator(
                progress = { (download.percent / 100).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(String.format(Locale.ROOT, "%.0f%%", download.percent))
            Text(statusText(download.status))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium)) {
            when (download?.status) {
                DownloadStatus.QUEUED,
                DownloadStatus.DOWNLOADING,
                -> {
                    Button(onClick = viewModel::pause) { Text(strings.btnPause) }
                    OutlinedButton(onClick = viewModel::cancel) { Text(strings.btnCancel) }
                }

                DownloadStatus.PAUSED -> {
                    Button(onClick = viewModel::resume) { Text(strings.btnResume) }
                    OutlinedButton(onClick = viewModel::cancel) { Text(strings.btnCancel) }
                }

                DownloadStatus.COMPLETED -> Button(onClick = {}, enabled = false) { Text(strings.btnDownloaded) }

                else -> Button(onClick = start) { Text(strings.btnDownload) }
            }
        }
    }
}

@Composable
private fun statusText(status: DownloadStatus): String {
    val strings = LocalStrings.current
    return when (status) {
        DownloadStatus.QUEUED,
        DownloadStatus.DOWNLOADING,
        -> strings.btnDownload

        DownloadStatus.PAUSED -> strings.btnPause

        DownloadStatus.COMPLETED -> strings.btnDownloaded

        DownloadStatus.FAILED -> strings.downloadErrorTitle

        DownloadStatus.PERMISSION_DENIED -> strings.downloadPermissionDeniedTitle

        DownloadStatus.CANCELLED -> strings.btnCancel
    }
}
