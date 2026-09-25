package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameActivityState
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameLaunchService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GlobalUiState(
    val isDownloading: Boolean = false,
    val isRefreshing: Boolean = false,
    val gameActivity: GameActivityState = GameActivityState.NotSupportedYet,
) {
    val isBusy: Boolean get() = isDownloading || isRefreshing
    val isGameRunning: Boolean? get() = (gameActivity as? GameActivityState.Active)?.games?.isNotEmpty()
}

@HiltViewModel
class GlobalViewModel @Inject constructor(
    downloads: DownloadManager,
    coordinator: LauncherDataCoordinator,
    launch: GameLaunchService,
) : ViewModel() {
    val state: StateFlow<GlobalUiState> = combine(
        downloads.downloads,
        coordinator.isRefreshing,
        launch.activeSessions,
    ) { rows, refreshing, activity ->
        GlobalUiState(
            isDownloading = rows.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING },
            isRefreshing = refreshing,
            gameActivity = activity,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GlobalUiState())
}
