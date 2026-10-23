package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import dev.jagoba.lostielauncher.model.SpecialVersionOptions
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface SpecialVersionLookup {
    data class Found(val config: SpecialVersionConfig) : SpecialVersionLookup

    data object NotFound : SpecialVersionLookup

    data object NetworkError : SpecialVersionLookup

    data object InvalidResponse : SpecialVersionLookup
}

interface SpecialVersionService {
    suspend fun lookup(key: String): SpecialVersionLookup
}

internal class CdnSpecialVersionService @Inject constructor(
    @ContentClient private val client: OkHttpClient,
    private val options: SpecialVersionOptions,
    private val dispatchers: DispatcherProvider,
    private val logger: Logger,
) : SpecialVersionService {
    override suspend fun lookup(key: String): SpecialVersionLookup = withContext(dispatchers.io) {
        try {
            val url = options.baseUrl.toHttpUrl().newBuilder()
                .addPathSegment(key)
                .addPathSegment(options.configFileName)
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                when {
                    response.code == 404 -> SpecialVersionLookup.NotFound

                    !response.isSuccessful -> SpecialVersionLookup.NetworkError

                    else -> {
                        val config = SpecialVersionConfig.parse(response.body.string())
                        if (config == null) SpecialVersionLookup.InvalidResponse else SpecialVersionLookup.Found(config)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.error("Special version configuration could not be loaded.", error)
            SpecialVersionLookup.NetworkError
        }
    }
}
