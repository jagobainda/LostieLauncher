package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal interface MaintenanceFlagApi {
    suspend fun head(url: String): Int

    suspend fun get(url: String): Int
}

internal class OkHttpMaintenanceFlagApi @Inject constructor(
    @SecurityFlagClient private val client: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : MaintenanceFlagApi {
    override suspend fun head(url: String): Int = statusOf(Request.Builder().url(url).head().build())

    override suspend fun get(url: String): Int = statusOf(Request.Builder().url(url).get().build())

    private suspend fun statusOf(request: Request): Int = withContext(dispatchers.io) {
        client.newCall(request).execute().use { it.code }
    }
}
