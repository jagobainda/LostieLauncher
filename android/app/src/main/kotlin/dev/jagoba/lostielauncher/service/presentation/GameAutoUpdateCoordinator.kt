package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.GameIdentityMatcher
import dev.jagoba.lostielauncher.util.version.VersionUtils
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class GameAutoUpdateCoordinator @Inject constructor(
    private val settings: SettingsStore,
    private val launcher: LauncherDataCoordinator,
    private val installation: GameInstallationService,
    private val downloads: DownloadManager,
    private val content: ContentService,
    private val logger: Logger,
) {
    private var started = false

    @Synchronized
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            var updateJob: Job? = null
            val activeGameId = AtomicReference<String?>(null)
            settings.settings
                .map { it.autoUpdate }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) {
                        updateJob = launch { runUpdates(activeGameId) }
                    } else {
                        updateJob?.cancelAndJoin()
                        updateJob = null
                        activeGameId.getAndSet(null)?.let { gameId -> cancelActiveUpdate(gameId) }
                    }
                }
        }
    }

    private suspend fun runUpdates(activeGameId: AtomicReference<String?>) {
        val (catalogue, installed) = combine(launcher.catalogue, installation.installedGames) { games, local ->
            games to local
        }.first { (games, local) -> !games.isLoading && local is InstalledGamesState.Available }
        val candidates = (installed as InstalledGamesState.Available).games.mapNotNull { local ->
            val remote = GameIdentityMatcher.findRemote(local, catalogue.games)
            remote?.takeIf {
                local.type.isNullOrEmpty() && VersionUtils.isNewerVersion(it.version, local.version)
            }
        }.distinctBy { it.gameId }
        for (game in candidates) {
            try {
                if (content.isServerActionBlocked()) break
                val request = DownloadRequest(
                    game.name,
                    GameDownloadArgs(game.gameId, game.version, game.relativePath),
                    game.sha256,
                    requiresUnmeteredNetwork = true,
                )
                when (downloads.start(request)) {
                    DownloadCommandResult.ACCEPTED -> {
                        activeGameId.set(game.gameId)
                        downloads.downloads.first { snapshots ->
                            snapshots.any { row ->
                                row.gameId == game.gameId && row.version == game.version && !row.status.isActive
                            }
                        }
                        activeGameId.set(null)
                    }

                    DownloadCommandResult.BUSY -> break

                    else -> Unit
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                activeGameId.set(null)
                logger.error("Automatic game update failed for ${game.gameId}.", error)
            }
        }
    }

    private suspend fun cancelActiveUpdate(gameId: String) {
        try {
            downloads.cancel(gameId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Automatic game update could not be cancelled for $gameId.", error)
        }
    }
}
