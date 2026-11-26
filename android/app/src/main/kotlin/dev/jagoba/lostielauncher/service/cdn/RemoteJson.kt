package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.AllowedHosts
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.net.DisallowedUrlException
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import dev.jagoba.lostielauncher.util.net.logNetworkOrError
import dev.jagoba.lostielauncher.util.net.readBoundedText
import dev.jagoba.lostielauncher.util.net.useCancellableResponse
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

internal class RemoteJson @Inject constructor(
    @ContentClient private val client: OkHttpClient,
    private val json: Json,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) {
    suspend fun <T> fetch(
        url: String,
        deserializer: DeserializationStrategy<T>,
        label: String,
        maxBytes: Long = DEFAULT_MAX_BYTES,
        allowedHosts: AllowedHosts? = null,
    ): T? = withContext(dispatchers.io) {
        try {
            if (allowedHosts != null && !HttpsUrls.isAllowedHttps(url, allowedHosts)) {
                throw DisallowedUrlException("$label URL was rejected.")
            }
            val request = Request.Builder()
                .url(url)
                .apply { allowedHosts?.let { tag(AllowedHosts::class.java, it) } }
                .build()
            client.newCall(request).useCancellableResponse { response ->
                if (!response.isSuccessful) throw IOException("$label returned HTTP ${response.code}.")
                json.decodeFromString(deserializer, response.body.readBoundedText(maxBytes, label))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.logNetworkOrError(
                error,
                networkMessage = { "$label could not be read ($it)." },
                unexpectedMessage = "$label could not be read.",
            )
            null
        }
    }

    companion object {
        const val DEFAULT_MAX_BYTES: Long = 512L * 1024
    }
}
