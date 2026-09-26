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
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.LibraryGameCard
import dev.jagoba.lostielauncher.ui.component.LibraryGameCardState
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryGameUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryViewModel
import dev.jagoba.lostielauncher.util.format.DownloadSpeedFormatter
import dev.jagoba.lostielauncher.util.format.PlaytimeFormatter

private const val LIBRARY_SKELETON_COUNT = 6

@Composable
fun LibraryScreen(
    scrollTarget: String?,
    onScrolledToTarget: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val requestNotifications = rememberNotificationPermissionRequest()
    val listState = rememberLazyListState()
    val targetIndex = state.games.indexOfFirst { it.game.gameId == scrollTarget }

    LaunchedEffect(scrollTarget, targetIndex) {
        if (scrollTarget == null || targetIndex < 0) return@LaunchedEffect
        listState.scrollToItem(targetIndex)
        onScrolledToTarget(scrollTarget)
    }

    when {
        state.isListVisible -> LazyColumn(
            modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(LauncherSpacing.Screen),
        ) {
            items(state.games, key = { it.game.gameId }) { row ->
                val gameId = row.game.gameId
                LibraryGameCard(
                    state = row.toCardState(),
                    onDownload = { viewModel.requestDownload(gameId) },
                    onPause = { viewModel.pause(gameId) },
                    onCancel = { viewModel.requestCancel(gameId) },
                    onUpdate = {
                        requestNotifications()
                        viewModel.startUpdate(gameId)
                    },
                    modifier = Modifier.padding(bottom = LauncherSpacing.MediumLarge),
                )
            }
        }

        state.isEmpty -> EmptyState(
            icon = R.drawable.ic_offline,
            message = strings.libraryNoContent,
            modifier = modifier,
        )

        else -> GameCardSkeletons(LIBRARY_SKELETON_COUNT, modifier)
    }
}

internal fun LibraryGameUiState.toCardState() = LibraryGameCardState(
    title = game.name,
    logoUrl = game.logoUrl,
    size = game.formattedSize,
    version = game.version,
    playtime = PlaytimeFormatter.format(playtimeMinutes),
    status = status,
    progress = download?.percent?.toFloat() ?: 0f,
    remainingTime = remainingTime,
    speed = download?.takeIf { it.status == DownloadStatus.DOWNLOADING }
        ?.let { DownloadSpeedFormatter.format(it.bytesPerSecond) },
    canStart = canStart,
    canPause = canPause,
    canCancel = canCancel,
    canUpdate = canUpdate,
)
