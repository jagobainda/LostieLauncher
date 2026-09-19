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

/**
 * Everything the launcher reads from the content CDN.
 *
 * The desktop's `IContentService` also owns the local game registry and the
 * playtime ledger. Those are local persistence and they arrive with port plan
 * step 07; this interface is the network half of it.
 *
 * Nothing here throws. The launcher must stay usable with the content server
 * down, so every failure is logged and turned into an empty or last-known
 * result — `spec/03-services.md` has the full degradation table, and it is
 * behaviour, not advice.
 */
interface ContentService {
    /**
     * The game catalogue, or an empty list.
     *
     * Empty for three different reasons, all of them normal: maintenance is on,
     * the CDN did not answer, or the payload could not be read. None of them is
     * an error the caller has to handle.
     *
     * Not cached. Every call is a live fetch, as on the desktop.
     */
    suspend fun getGames(): List<GameInfo>

    /**
     * News and notifications, resolved to [language] and stripped of expired
     * items.
     *
     * The raw payload is cached in memory for the life of the process and is
     * only ever replaced by a successful fetch — it does not expire on a timer.
     * A failed refresh keeps what was already there and comes back with
     * [HomeContent.isStale] set, so the screen shows the last known content
     * under a banner instead of going blank.
     *
     * @param language the language to project onto. The cache holds the payload
     *   unresolved, so switching language re-projects without a refetch.
     * @param forceRefresh skip the cache and fetch. A forced refresh that fails
     *   still keeps the previous content.
     */
    suspend fun getHomeContent(language: AppLanguage, forceRefresh: Boolean = false): HomeContent

    /**
     * Whether the maintenance kill switch is on.
     *
     * While it is, the launcher refuses to fetch the catalogue, start a
     * download, update a game or switch to a special version.
     *
     * **It fails open.** An unreachable flag file answers `false`, because an
     * unreachable CDN must not lock a user out of their own launcher.
     */
    suspend fun isServerActionBlocked(forceRefresh: Boolean = false): Boolean
}

/** [ContentService] against the project CDN. */
@Singleton
internal class DefaultContentService @Inject constructor(
    private val contentApi: ContentApi,
    private val maintenanceFlagApi: MaintenanceFlagApi,
    private val options: ContentOptions,
    private val clock: Clock,
    private val logger: Logger,
) : ContentService {
    /**
     * Serialises the home-content refresh.
     *
     * Without it, several screens asking for content at startup would each open
     * their own request and the last one to finish would win. The desktop uses
     * a `SemaphoreSlim(1, 1)` here for the same reason.
     */
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

    /**
     * Returns the payload to project and whether it is stale, fetching first if
     * it has to.
     *
     * The staleness flag is sticky: once a refresh has failed, later unforced
     * calls keep reporting stale until a fetch succeeds, because what they are
     * serving is still the old content.
     */
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

    /**
     * A refresh that fails because the network did is ordinary and logs at info;
     * anything else is a defect and logs as an error. The desktop draws the same
     * line, and it is what keeps a trip through a tunnel out of the error log.
     */
    private fun logHomeContentRefreshFailure(e: Exception) {
        // IOException is a connection that did not happen; HttpException is a
        // server that answered with a status the payload cannot survive. The
        // desktop folds both into HttpRequestException and logs them the same.
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

    /**
     * `HEAD` first, because the body is irrelevant. A server that refuses `HEAD`
     * with 405 is asked again with `GET`, whose body is discarded unread.
     */
    private suspend fun probeMaintenanceFlag(): Boolean {
        val headStatus = maintenanceFlagApi.head(options.maintenanceFlagUrl)
        if (headStatus != HTTP_METHOD_NOT_ALLOWED) return isBlockingStatus(headStatus)

        return isBlockingStatus(maintenanceFlagApi.get(options.maintenanceFlagUrl))
    }

    /** Any 2xx means the file is there, and the file being there is the signal. */
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
