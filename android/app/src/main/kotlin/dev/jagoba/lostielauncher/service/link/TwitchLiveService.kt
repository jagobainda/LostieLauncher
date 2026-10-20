package dev.jagoba.lostielauncher.service.link

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.TwitchLiveOptions
import dev.jagoba.lostielauncher.service.cdn.TwitchPreviewClient
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

fun interface TwitchLiveService {
    suspend fun isLive(): Boolean
}

internal class PreviewTwitchLiveService @Inject constructor(
    @TwitchPreviewClient private val client: OkHttpClient,
    private val options: TwitchLiveOptions,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : TwitchLiveService {
    override suspend fun isLive(): Boolean = withContext(dispatchers.io) {
        try {
            client.newCall(Request.Builder().url(options.previewUrl).head().build()).execute().use { it.isSuccessful }
        } catch (error: IOException) {
            logger.error("Checking whether the Twitch channel is live failed.", error)
            false
        }
    }
}
