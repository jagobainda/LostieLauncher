package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * The two JSON endpoints of the CDN.
 *
 * Both take their URL as an argument rather than declaring a path: they live on
 * different hosts, and the launcher's rule is that no URL is written inside the
 * type that does the work. The caller passes the value out of
 * [dev.jagoba.lostielauncher.model.ContentOptions].
 *
 * Backed by the content client, whose ten-second timeout is what makes a slow
 * CDN degrade to the cache instead of stalling the screen.
 */
internal interface ContentApi {
    /**
     * The game catalogue: a bare JSON array.
     *
     * The elements are nullable because the desktop drops a stray `null`
     * element instead of failing the payload over it.
     */
    @GET
    suspend fun catalogue(@Url url: String): List<GameDto?>

    /** News and notifications, unresolved and unfiltered. */
    @GET
    suspend fun homeContent(@Url url: String): HomeContentDto
}
