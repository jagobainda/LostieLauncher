package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.CatalogueResult
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.link.TwitchLiveService
import dev.jagoba.lostielauncher.service.network.NetworkMonitor
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.Logger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeData(
    val isLoading: Boolean = true,
    val content: HomeContent = HomeContent(),
    val isOffline: Boolean = false,
    val isTwitchLive: Boolean = false,
)

data class CatalogueData(
    val isLoading: Boolean = true,
    val games: List<GameInfo> = emptyList(),
    val isStale: Boolean = false,
)

@Singleton
class LauncherDataCoordinator @Inject constructor(
    private val content: ContentService,
    private val downloads: DownloadManager,
    private val twitch: TwitchLiveService,
    private val settings: SettingsStore,
    private val network: NetworkMonitor,
    private val options: HomeRefreshOptions,
    private val logger: Logger,
) {
    private val homeGate = Mutex()
    private val catalogueGate = Mutex()
    private val refreshGate = Mutex()
    private val availabilityRevision = AtomicLong(0L)
    private val offlineLock = Any()
    private var latestOfflineRevision = 0L
    private val mutableHome = MutableStateFlow(HomeData())
    private val mutableCatalogue = MutableStateFlow(CatalogueData())
    private val mutableRefreshing = MutableStateFlow(false)
    private val visible = MutableStateFlow(true)
    private var started = false

    val home: StateFlow<HomeData> = mutableHome.asStateFlow()
    val catalogue: StateFlow<CatalogueData> = mutableCatalogue.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()

    @Synchronized
    fun start(scope: CoroutineScope) {
        if (started) return
        started = true
        scope.launch {
            settings.settings.map { it.language }.distinctUntilChanged().collect { language ->
                refreshHome(language, force = false)
            }
        }
        scope.launch {
            if (!settings.settings.first().hasSeenWelcome) {
                settings.settings.first { it.hasSeenWelcome }
                refreshTwitchLive()
            }
        }
        scope.launch { refreshCatalogue() }
        scope.launch {
            network.isOnline.drop(1).collect { online ->
                val language = settings.settings.first().language
                if (online) {
                    logger.info("Internet connection restored. Reloading home and catalogue.")
                    reload(language, showLoading = false)
                } else {
                    logger.info("Internet connection lost. Entering offline mode.")
                    refreshHome(language, force = false, showLoading = false)
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(options.interval.inWholeMilliseconds)
                visible.first { it }
                refreshHome(settings.settings.first().language, showLoading = false)
                if (mutableCatalogue.value.isStale) {
                    refreshCatalogue(showLoading = false)
                }
            }
        }
    }

    suspend fun refreshHome(language: AppLanguage, force: Boolean = true, showLoading: Boolean = true) {
        loadHome(language, force, showLoading, checkAvailability = true)
    }

    private suspend fun loadHome(
        language: AppLanguage,
        force: Boolean,
        showLoading: Boolean,
        checkAvailability: Boolean,
    ) {
        homeGate.withLock {
            if (showLoading) mutableHome.update { it.copy(isLoading = true) }
            try {
                val allowTwitch = settings.settings.first().hasSeenWelcome
                coroutineScope {
                    val live = if (allowTwitch) async { twitch.isLive() } else null
                    if (checkAvailability) {
                        val revision = availabilityRevision.incrementAndGet()
                        publishOffline(content.isServerActionBlocked(force), revision)
                    }
                    val feed = content.getHomeContent(language, force)
                    val isLive = live?.await()
                    mutableHome.update { current ->
                        current.copy(
                            content = feed,
                            isTwitchLive = isLive ?: current.isTwitchLive,
                            isLoading = false,
                        )
                    }
                    logger.debug(
                        "Home content loaded: ${feed.news.size} news, ${feed.notifications.size} notifications. " +
                            "Offline mode: ${mutableHome.value.isOffline}. Stale: ${feed.isStale}.",
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Home content refresh failed.", error)
                mutableHome.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun refreshTwitchLive() {
        homeGate.withLock {
            try {
                val isLive = twitch.isLive()
                mutableHome.update { it.copy(isTwitchLive = isLive) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Twitch live status refresh failed.", error)
            }
        }
    }

    suspend fun refreshCatalogue(showLoading: Boolean = true, force: Boolean = false) {
        loadCatalogue(showLoading, force, blockedOverride = null, availabilityRevisionOverride = null)
    }

    private suspend fun loadCatalogue(
        showLoading: Boolean,
        force: Boolean,
        blockedOverride: Boolean?,
        availabilityRevisionOverride: Long?,
    ) {
        catalogueGate.withLock {
            if (showLoading) mutableCatalogue.update { it.copy(isLoading = true) }
            try {
                val revision = availabilityRevisionOverride ?: availabilityRevision.incrementAndGet()
                val result = if (blockedOverride == true) CatalogueResult.Blocked else content.getGames(force)
                when (result) {
                    is CatalogueResult.Loaded -> {
                        val games = distinctGames(result.games)
                        mutableCatalogue.value = CatalogueData(isLoading = false, games = games, isStale = false)
                        publishOffline(false, revision)
                        purgeStaleDownloads()
                    }

                    CatalogueResult.Blocked -> {
                        mutableCatalogue.update { it.copy(isLoading = false, isStale = true) }
                        publishOffline(true, revision)
                    }

                    CatalogueResult.Failed -> {
                        mutableCatalogue.update { it.copy(isLoading = false, isStale = true) }
                        if (mutableHome.value.isOffline) {
                            publishOffline(
                                blockedOverride ?: content.isServerActionBlocked(forceRefresh = false),
                                revision,
                            )
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Catalogue refresh failed.", error)
                mutableCatalogue.update { it.copy(isLoading = false, isStale = true) }
            }
        }
    }

    suspend fun purgeStaleDownloads() {
        val games = catalogue.value.games
        if (games.isNotEmpty()) downloads.purgeStale(games.mapTo(mutableSetOf()) { it.gameId })
    }

    suspend fun refreshAll(language: AppLanguage) {
        if (!refreshGate.tryLock()) return
        mutableRefreshing.value = true
        try {
            reload(language)
        } finally {
            mutableRefreshing.value = false
            refreshGate.unlock()
        }
    }

    private suspend fun reload(language: AppLanguage, showLoading: Boolean = true) {
        val revision = availabilityRevision.incrementAndGet()
        val offline = try {
            content.isServerActionBlocked(forceRefresh = true)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Server availability check failed during reload.", error)
            false
        }
        publishOffline(offline, revision)
        coroutineScope {
            val homeRefresh =
                async { loadHome(language, force = true, showLoading = showLoading, checkAvailability = false) }
            val catalogueRefresh = async {
                loadCatalogue(
                    showLoading = showLoading,
                    force = false,
                    blockedOverride = offline,
                    availabilityRevisionOverride = revision,
                )
            }
            homeRefresh.await()
            catalogueRefresh.await()
        }
    }

    private fun publishOffline(offline: Boolean, revision: Long) {
        synchronized(offlineLock) {
            if (revision < latestOfflineRevision) return
            latestOfflineRevision = revision
            mutableHome.update { it.copy(isOffline = offline) }
        }
    }

    fun setVisible(isVisible: Boolean) {
        visible.value = isVisible
    }

    private fun distinctGames(games: List<GameInfo>): List<GameInfo> {
        val seen = mutableSetOf<String>()
        return games.filter { game ->
            seen.add(game.gameId).also { unique ->
                if (!unique) logger.info("Skipping duplicate catalogue entry: '${game.name}' (id: ${game.gameId}).")
            }
        }
    }
}
