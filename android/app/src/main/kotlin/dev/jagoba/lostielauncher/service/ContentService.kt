package dev.jagoba.lostielauncher.service

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.ContentOptions
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.service.cdn.CdnMappers
import dev.jagoba.lostielauncher.service.cdn.ContentApi
import dev.jagoba.lostielauncher.service.cdn.MaintenanceFlagApi
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

interface ContentService {
    suspend fun getGames(): List<GameInfo>

    suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean = false): HomeContent

    suspend fun isServerActionBlocked(forceRefresh: Boolean = false): Boolean
}

@Singleton
internal class DefaultContentService @Inject constructor(
    private val contentApi: ContentApi,
    private val maintenanceFlagApi: MaintenanceFlagApi,
    private val options: ContentOptions,
    private val clock: Clock,
    private val logger: Logger,
) : ContentService {
    private val homeContentGate = Mutex()

    private var homeContentCache: HomeContentDto? = null
    private var homeContentIsStale = false

    @Volatile
    private var maintenanceFlagCache: MaintenanceFlagCache? = null

    override suspend fun getGames(): List<GameInfo> {
        try {
            if (isServerActionBlocked()) {
                logger.info("Games list request skipped because server actions are blocked by the maintenance flag.")
                return emptyList()
            }

            logger.debug("Fetching games list from remote.")
            val games = contentApi.catalogue(options.catalogueUrl)
                .filterNotNull()
                .map { CdnMappers.toDomain(it, options.cdnBaseUrl) }
            logger.info("Fetched ${games.size} games from remote.")
            return games
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Fetching the games list failed; the catalogue is empty for this call.", e)
            return emptyList()
        }
    }

    override suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean): HomeContent {
        val (cache, isStale) = resolveHomeContentCache(forceRefresh)
        if (cache == null) return HomeContent(isStale = isStale)

        return try {
            val content = CdnMappers.toDomain(cache, language, clock, isStale)
            logger.debug(
                "Home content resolved for language ${language.code}: " +
                    "${content.news.size} news, ${content.notifications.size} notifications.",
            )
            content
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Projecting the home content failed; reporting it as unavailable.", e)
            HomeContent(isStale = true)
        }
    }

    private suspend fun resolveHomeContentCache(forceRefresh: Boolean): Pair<HomeContentDto?, Boolean> =
        homeContentGate.withLock {
            val cached = homeContentCache
            if (!forceRefresh && cached != null) {
                logger.debug("Using cached home content.")
                return@withLock cached to homeContentIsStale
            }

            try {
                logger.debug("Fetching home content from remote.")
                val fetched = contentApi.homeContent(options.homeContentUrl)
                homeContentCache = fetched
                homeContentIsStale = false
                logger.debug(
                    "Home content fetched: ${fetched.news.size} raw news, " +
                        "${fetched.notifications.size} raw notifications.",
                )
                fetched to false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logHomeContentRefreshFailure(e)
                homeContentIsStale = true
                homeContentCache to true
            }
        }

    private fun logHomeContentRefreshFailure(e: Exception) {
        if (e is IOException || e is HttpException) {
            logger.info(
                "Home content refresh failed (${e.javaClass.simpleName}: ${e.message}); " +
                    "keeping the last known content.",
            )
        } else {
            logger.error("Home content refresh failed for a reason that is not the network.", e)
        }
    }

    override suspend fun isServerActionBlocked(forceRefresh: Boolean): Boolean {
        val cached = maintenanceFlagCache
        if (!forceRefresh && cached != null && clock.instant() < cached.expiresAt) return cached.blocked

        return try {
            val blocked = probeMaintenanceFlag()
            cacheMaintenanceFlag(blocked)
            blocked
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("The maintenance flag check failed; assuming server actions are not blocked.", e)
            cacheMaintenanceFlag(blocked = false)
            false
        }
    }

    private suspend fun probeMaintenanceFlag(): Boolean {
        val headStatus = maintenanceFlagApi.head(options.maintenanceFlagUrl)
        if (headStatus != HTTP_METHOD_NOT_ALLOWED) return isBlockingStatus(headStatus)

        return isBlockingStatus(maintenanceFlagApi.get(options.maintenanceFlagUrl))
    }

    private fun isBlockingStatus(status: Int): Boolean {
        if (status in HTTP_OK..HTTP_LAST_SUCCESS) {
            logger.info("Maintenance flag detected. Server-backed actions are temporarily blocked.")
            return true
        }

        logger.debug("Maintenance flag check: status $status - not blocking.")
        return false
    }

    private fun cacheMaintenanceFlag(blocked: Boolean) {
        maintenanceFlagCache = MaintenanceFlagCache(
            blocked = blocked,
            expiresAt = clock.instant().plus(options.maintenanceFlagCacheDuration.toJavaDuration()),
        )
    }

    private data class MaintenanceFlagCache(val blocked: Boolean, val expiresAt: Instant)

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_LAST_SUCCESS = 299
        const val HTTP_METHOD_NOT_ALLOWED = 405
    }
}
