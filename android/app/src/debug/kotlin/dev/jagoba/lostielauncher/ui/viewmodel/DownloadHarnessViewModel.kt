package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadSnapshot
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadHarnessState(val game: GameInfo?, val download: DownloadSnapshot?, val catalogueLoaded: Boolean)

@HiltViewModel
class DownloadHarnessViewModel @Inject constructor(
    private val contentService: ContentService,
    private val downloadManager: DownloadManager,
) : ViewModel() {
    private val game = MutableStateFlow<GameInfo?>(null)
    private val catalogueLoaded = MutableStateFlow(false)

    val state: StateFlow<DownloadHarnessState> = combine(
        game,
        catalogueLoaded,
        downloadManager.downloads,
    ) { selectedGame, loaded, downloads ->
        DownloadHarnessState(
            game = selectedGame,
            download = downloads.firstOrNull { it.gameId == selectedGame?.gameId },
            catalogueLoaded = loaded,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadHarnessState(null, null, false),
    )

    init {
        viewModelScope.launch {
            val games = contentService.getGames()
            downloadManager.purgeStale(games.mapTo(mutableSetOf(), GameInfo::gameId))
            game.value = games.minByOrNull(GameInfo::sizeGb)
            catalogueLoaded.value = true
        }
    }

    fun start() {
        val selected = game.value ?: return
        viewModelScope.launch {
            downloadManager.start(
                DownloadRequest(
                    displayName = selected.name,
                    args = GameDownloadArgs(
                        gameId = selected.gameId,
                        version = selected.version,
                        relativePath = selected.relativePath,
                    ),
                ),
            )
        }
    }

    fun pause() {
        game.value?.gameId?.let { gameId -> viewModelScope.launch { downloadManager.pause(gameId) } }
    }

    fun resume() {
        game.value?.gameId?.let { gameId -> viewModelScope.launch { downloadManager.resume(gameId) } }
    }

    fun cancel() {
        game.value?.gameId?.let { gameId -> viewModelScope.launch { downloadManager.cancel(gameId) } }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
