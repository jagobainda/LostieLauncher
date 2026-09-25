package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.DownloadCommandResult
import dev.jagoba.lostielauncher.model.DownloadRequest
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.GameDownloadArgs
import dev.jagoba.lostielauncher.model.GameInstallationState
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.policy.GameIdentityMatcher
import dev.jagoba.lostielauncher.util.version.VersionUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
            if (!settings.settings.first().autoUpdate) return@launch
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
                    )
                    when (downloads.start(request)) {
                        DownloadCommandResult.ACCEPTED -> {
                            val rows = downloads.downloads.first { snapshots ->
                                snapshots.any { row ->
                                    row.gameId == game.gameId && row.version == game.version &&
                                        row.status in TERMINAL_STATUSES
                                }
                            }
                            val row = rows.first { it.gameId == game.gameId && it.version == game.version }
                            if (row.status == DownloadStatus.COMPLETED) {
                                installation.observeInstallation(game.gameId).first {
                                    it is GameInstallationState.Finished
                                }
                            }
                        }

                        DownloadCommandResult.BUSY -> break

                        else -> Unit
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.error("Automatic game update failed for ${game.gameId}.", error)
                }
            }
        }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(
            DownloadStatus.COMPLETED,
            DownloadStatus.FAILED,
            DownloadStatus.PERMISSION_DENIED,
            DownloadStatus.PAUSED,
            DownloadStatus.CANCELLED,
        )
    }
}
