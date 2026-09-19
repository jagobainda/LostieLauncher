package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * The seam over the maintenance-flag probe.
 *
 * The flag is a file whose **existence** is the whole message, so the only
 * thing worth returning is a status code — no body is ever read. Keeping the
 * transport behind this interface leaves the decisions that follow from the
 * code (2xx means blocked, 405 means ask again with GET) in the service, where
 * they can be tested without a socket.
 *
 * This one endpoint does not go through Retrofit, because there is nothing to
 * deserialize and Retrofit will not let a `@HEAD` return anything but `Unit`.
 */
internal interface MaintenanceFlagApi {
    /** Sends a `HEAD` and returns the status code. */
    suspend fun head(url: String): Int

    /** Sends a `GET`, reads the headers and closes the body unread. Returns the status code. */
    suspend fun get(url: String): Int
}

/**
 * [MaintenanceFlagApi] on the security-flag client, whose three-second timeout
 * is what keeps this check from ever being what the user waits on.
 */
internal class OkHttpMaintenanceFlagApi @Inject constructor(
    @SecurityFlagClient private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : MaintenanceFlagApi {
    override suspend fun head(url: String): Int = statusOf(Request.Builder().url(url).head().build())

    override suspend fun get(url: String): Int = statusOf(Request.Builder().url(url).get().build())

    /**
     * `use` closes the response, which for the GET fallback discards the body
     * without reading it — the desktop's `ResponseHeadersRead`.
     */
    private suspend fun statusOf(request: Request): Int = withContext(dispatchers.io) {
        client.newCall(request).execute().use { it.code }
    }
}
