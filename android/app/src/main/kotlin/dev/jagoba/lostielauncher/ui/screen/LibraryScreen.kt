package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.ContentStatusBanner
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.LibraryCardActionsReserve
import dev.jagoba.lostielauncher.ui.component.LibraryGameCard
import dev.jagoba.lostielauncher.ui.component.LibraryGameCardState
import dev.jagoba.lostielauncher.ui.dialog.DialogButton
import dev.jagoba.lostielauncher.ui.dialog.DialogButtonStyle
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryGameUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryViewModel
import dev.jagoba.lostielauncher.util.format.DownloadSpeedFormatter

private const val LIBRARY_SKELETON_COUNT = 6
internal const val LIBRARY_BANNER_KEY = "banner"

internal fun libraryGameKey(gameId: String): String = "game:$gameId"

@Composable
fun LibraryScreen(
    scrollTarget: String?,
    onScrolledToTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val listState = rememberLazyListState()
    val targetIndex = state.games.indexOfFirst { it.gameId == scrollTarget }

    LaunchedEffect(scrollTarget, targetIndex) {
        if (scrollTarget == null || targetIndex < 0) return@LaunchedEffect
        listState.scrollToItem(targetIndex + 1)
        onScrolledToTarget(scrollTarget)
    }

    when {
        state.isListVisible -> LazyColumn(
            modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(LauncherSpacing.Screen),
        ) {
            item(key = LIBRARY_BANNER_KEY) {
                ContentStatusBanner(
                    isOffline = state.isOffline,
                    isOutOfDate = state.isOutOfDateWarningVisible,
                )
            }
            items(state.games, key = { libraryGameKey(it.gameId) }) { row ->
                val gameId = row.gameId
                LibraryGameCard(
                    state = row.toCardState(),
                    onDownload = { viewModel.requestDownload(gameId) },
                    onPause = { viewModel.pause(gameId) },
                    onCancel = { viewModel.requestCancel(gameId) },
                    onUpdate = { viewModel.startUpdate(gameId) },
                    modifier = Modifier.padding(bottom = LauncherSpacing.MediumLarge),
                )
            }
        }

        state.isEmpty -> EmptyState(
            icon = R.drawable.ic_offline,
            message = if (state.isContentStale || state.isOffline) {
                strings.homeContentUnavailable
            } else {
                strings.libraryNoContent
            },
            detail = strings.serverMaintenanceNotificationMessage.takeIf { state.isOffline },
            action = {
                DialogButton(
                    label = strings.btnRetry,
                    style = DialogButtonStyle.ACCENT,
                    onClick = viewModel::refresh,
                    horizontalPadding = LauncherSpacing.Screen,
                )
            },
            modifier = modifier,
        )

        else -> GameCardSkeletons(LIBRARY_SKELETON_COUNT, LibraryCardActionsReserve, modifier)
    }
}

internal fun LibraryGameUiState.toCardState() = LibraryGameCardState(
    title = game.name,
    logoUrl = game.logoUrl,
    size = formattedSize,
    version = game.version,
    status = status,
    progress = download?.percent?.toFloat() ?: 0f,
    extractionProgress = (installation as? GameInstallationState.Extracting)?.percent?.toFloat(),
    remainingTime = remainingTime,
    speed = download?.takeIf { it.status == DownloadStatus.DOWNLOADING }
        ?.let { DownloadSpeedFormatter.format(it.bytesPerSecond) },
    waitingForConnection = download?.status == DownloadStatus.QUEUED,
    canStart = canStart,
    canPause = canPause,
    canCancel = canCancel,
    canUpdate = canUpdate,
)
