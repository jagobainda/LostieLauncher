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

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CDN_BASE_URL = "https://ericlostie-launcher.jagoba.dev"

    private const val CONTENT_ENDPOINT = "$CDN_BASE_URL/games/listado.json"
    private const val NOTIFICATIONS_ENDPOINT = "https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json"
    private const val FLAG_ENDPOINT = "https://cdn.jagoba.dev/ericlostie-launcher/flag.txt"
    private const val DOWNLOAD_BASE_URL = "$CDN_BASE_URL/games"

    private const val CONTENT_TIMEOUT_SECONDS = 10L
    private const val SECURITY_FLAG_TIMEOUT_SECONDS = 3L
    private const val DOWNLOAD_CONNECT_TIMEOUT_SECONDS = 20L

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

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): Interceptor = Interceptor { chain ->
        chain.proceed(
            chain.request().newBuilder()
                .header("User-Agent", "LostieLauncher/${BuildConfig.VERSION_NAME}")
                .build(),
        )
    }

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

    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadClient(@BaseHttpClient base: OkHttpClient): OkHttpClient = base.newBuilder()
        .connectTimeout(DOWNLOAD_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(NO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    internal fun provideContentApi(@ContentClient client: OkHttpClient, json: Json): ContentApi = Retrofit.Builder()
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class BaseHttpClient
