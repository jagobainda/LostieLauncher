package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.HomeRefreshOptions
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.log.Logger
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeData(
    val isLoading: Boolean = true,
    val content: HomeContent = HomeContent(),
    val isOffline: Boolean = false,
)

data class CatalogueData(val isLoading: Boolean = true, val games: List<GameInfo> = emptyList())

@Singleton
class LauncherDataCoordinator @Inject constructor(
    private val content: ContentService,
    private val downloads: DownloadManager,
    private val logger: Logger,
) {
    private val homeGate = Mutex()
    private val catalogueGate = Mutex()
    private val refreshGate = Mutex()
    private val mutableHome = MutableStateFlow(HomeData())
    private val mutableCatalogue = MutableStateFlow(CatalogueData())
    private val mutableRefreshing = MutableStateFlow(false)
    private val mutableGamesLoading = MutableStateFlow(false)
    private val mutableGamesRevision = MutableStateFlow(0L)
    private var started = false

    val home: StateFlow<HomeData> = mutableHome.asStateFlow()
    val catalogue: StateFlow<CatalogueData> = mutableCatalogue.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()
    val isGamesLoading: StateFlow<Boolean> = mutableGamesLoading.asStateFlow()
    val gamesRevision: StateFlow<Long> = mutableGamesRevision.asStateFlow()

    @Synchronized
    fun start(scope: CoroutineScope, settings: SettingsStore, options: HomeRefreshOptions) {
        if (started) return
        started = true
        scope.launch {
            settings.settings.map { it.language }.distinctUntilChanged().collect { language ->
                refreshHome(language, force = false)
            }
        }
        scope.launch { refreshCatalogue() }
        scope.launch {
            while (isActive) {
                delay(options.interval.inWholeMilliseconds)
                refreshHome(settings.settings.first().language, showLoading = false)
            }
        }
    }

    suspend fun refreshHome(language: AppLanguage, force: Boolean = true, showLoading: Boolean = true) {
        homeGate.withLock {
            if (showLoading) mutableHome.value = mutableHome.value.copy(isLoading = true)
            try {
                val offline = content.isServerActionBlocked(force)
                val feed = content.getHomeContent(language, force)
                mutableHome.value = HomeData(content = feed, isOffline = offline, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Home content refresh failed.", error)
                mutableHome.value = mutableHome.value.copy(isLoading = false)
            }
        }
    }

    suspend fun refreshCatalogue() {
        catalogueGate.withLock {
            mutableCatalogue.value = mutableCatalogue.value.copy(isLoading = true)
            try {
                val games = content.getGames()
                mutableCatalogue.value = CatalogueData(isLoading = false, games = games)
                if (games.isNotEmpty()) downloads.purgeStale(games.mapTo(mutableSetOf()) { it.gameId })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                logger.error("Catalogue refresh failed.", error)
                mutableCatalogue.value = mutableCatalogue.value.copy(isLoading = false)
            }
        }
    }

    suspend fun refreshAll(language: AppLanguage) {
        if (!refreshGate.tryLock()) return
        mutableRefreshing.value = true
        try {
            coroutineScope {
                val homeRefresh = async { refreshHome(language) }
                val catalogueRefresh = async { refreshCatalogue() }
                homeRefresh.await()
                catalogueRefresh.await()
            }
            mutableGamesRevision.value += 1
        } finally {
            mutableRefreshing.value = false
            refreshGate.unlock()
        }
    }

    fun setGamesLoading(loading: Boolean) {
        mutableGamesLoading.value = loading
    }

    fun refreshGamesProjection() {
        mutableGamesRevision.value += 1
    }
}
