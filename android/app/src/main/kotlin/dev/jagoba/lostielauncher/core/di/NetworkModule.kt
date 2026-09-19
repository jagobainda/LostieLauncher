package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.BuildConfig
import dev.jagoba.lostielauncher.model.ContentOptions
import dev.jagoba.lostielauncher.model.DownloadOptions
import dev.jagoba.lostielauncher.service.ContentService
import dev.jagoba.lostielauncher.service.DefaultContentService
import dev.jagoba.lostielauncher.service.cdn.ContentApi
import dev.jagoba.lostielauncher.service.cdn.ContentClient
import dev.jagoba.lostielauncher.service.cdn.DownloadClient
import dev.jagoba.lostielauncher.service.cdn.MaintenanceFlagApi
import dev.jagoba.lostielauncher.service.cdn.OkHttpMaintenanceFlagApi
import dev.jagoba.lostielauncher.service.cdn.SecurityFlagClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Where the CDN lives, and how the launcher talks to it.
 *
 * This is the counterpart of the network half of `Core/DependencyInjection.cs`.
 * Every URL and every timeout is written **here**, in the composition root, and
 * nowhere else: a service receives an options record, and a client arrives
 * already configured behind its qualifier.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    /** One shared origin for the launcher's own host. The desktop's `Endpoints.CdnBaseUrl`. */
    private const val CDN_BASE_URL = "https://ericlostie-launcher.jagoba.dev"

    /**
     * Two endpoints sit on a different host from the rest. That is not a
     * mistake to tidy up: the catalogue is served by the launcher's own host
     * and these two by a general-purpose CDN.
     */
    private const val CONTENT_ENDPOINT = "$CDN_BASE_URL/games/listado.json"
    private const val NOTIFICATIONS_ENDPOINT = "https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json"
    private const val FLAG_ENDPOINT = "https://cdn.jagoba.dev/ericlostie-launcher/flag.txt"
    private const val DOWNLOAD_BASE_URL = "$CDN_BASE_URL/games"

    /** See the qualifiers in `service/cdn/HttpClients.kt` for why these three differ. */
    private const val CONTENT_TIMEOUT_SECONDS = 10L
    private const val SECURITY_FLAG_TIMEOUT_SECONDS = 3L
    private const val DOWNLOAD_CONNECT_TIMEOUT_SECONDS = 20L

    /** Zero is OkHttp's "no timeout", which is what an archive transfer needs. */
    private const val NO_TIMEOUT_SECONDS = 0L

    @Provides
    @Singleton
    fun provideContentOptions(): ContentOptions = ContentOptions(
        cdnBaseUrl = CDN_BASE_URL,
        catalogueUrl = CONTENT_ENDPOINT,
        homeContentUrl = NOTIFICATIONS_ENDPOINT,
        maintenanceFlagUrl = FLAG_ENDPOINT,
        maintenanceFlagCacheDuration = 30.seconds,
    )

    @Provides
    @Singleton
    fun provideDownloadOptions(): DownloadOptions = DownloadOptions(baseUrl = DOWNLOAD_BASE_URL)

    /**
     * `LostieLauncher/<version>`, on every request the launcher makes, as the
     * desktop sends. It is what lets the CDN's logs tell the two clients apart.
     */
    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): Interceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "LostieLauncher/${BuildConfig.VERSION_NAME}")
                .build(),
        )
    }

    /**
     * The client the other three are derived from.
     *
     * Deriving with `newBuilder()` rather than building three clients from
     * scratch is what keeps the connection pool, the dispatcher and its thread
     * pool shared between them: three clients should mean three timeout
     * policies, not three of everything. It is never injected directly, which
     * is what the qualifier is for.
     */
    @Provides
    @Singleton
    @BaseHttpClient
    fun provideBaseHttpClient(userAgent: Interceptor): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(userAgent)
        .build()

    @Provides
    @Singleton
    @ContentClient
    fun provideContentClient(@BaseHttpClient base: OkHttpClient): OkHttpClient = base.newBuilder()
        .callTimeout(CONTENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @SecurityFlagClient
    fun provideSecurityFlagClient(@BaseHttpClient base: OkHttpClient): OkHttpClient = base.newBuilder()
        .callTimeout(SECURITY_FLAG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * No call timeout and no read timeout, because a multi-gigabyte transfer
     * would trip either one; a connect timeout, because a dead host must still
     * fail before any bytes move. Step 08 adds the inactivity watchdog that
     * catches the case these timeouts deliberately no longer catch.
     */
    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadClient(@BaseHttpClient base: OkHttpClient): OkHttpClient = base.newBuilder()
        .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * The JSON reader for every remote payload.
     *
     * Three settings, and each one is a behaviour:
     * - `ignoreUnknownKeys` so the CDN can add a field without breaking every
     *   installed launcher;
     * - `isLenient` **off**, so a payload that is not quite JSON is a failure
     *   rather than a guess;
     * - `coerceInputValues` **off**, which is the load-bearing one: it is what
     *   makes an explicit `"nombre": null` fail the payload instead of quietly
     *   becoming an empty name. See `GameDto`.
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    internal fun provideContentApi(@ContentClient client: OkHttpClient, json: Json): ContentApi = Retrofit.Builder()
        // Every call passes an absolute @Url, because the endpoints do not
        // share a host. Retrofit still insists on a base, so it gets the one
        // most of them are on.
        .baseUrl("$CDN_BASE_URL/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ContentApi::class.java)

    @Provides
    @Singleton
    internal fun provideMaintenanceFlagApi(api: OkHttpMaintenanceFlagApi): MaintenanceFlagApi = api

    @Provides
    @Singleton
    internal fun provideContentService(service: DefaultContentService): ContentService = service
}

/**
 * Marks the undifferentiated client the three purpose-built ones derive from.
 *
 * It lives here rather than beside the other three in `service/cdn`, because
 * nothing outside the composition root may ask for it: a client with no timeout
 * policy is not a client anything should be using.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class BaseHttpClient
