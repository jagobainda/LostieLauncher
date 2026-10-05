package dev.jagoba.lostielauncher.core.di

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("NetworkModule")
class NetworkModuleTest {
    private val base = NetworkModule.provideBaseHttpClient(NetworkModule.provideUserAgentInterceptor())

    @Test
    fun `gives the content client a ten-second budget for the whole call`() {
        NetworkModule.provideContentClient(base).callTimeoutMillis shouldBe
            TimeUnit.SECONDS.toMillis(10).toInt()
    }

    @Test
    fun `gives the security flag client a three-second budget`() {
        NetworkModule.provideSecurityFlagClient(base).callTimeoutMillis shouldBe
            TimeUnit.SECONDS.toMillis(3).toInt()
    }

    @Test
    fun `gives the download client no wall clock at all, only a connect timeout`() {
        val client = NetworkModule.provideDownloadClient(base)

        client.callTimeoutMillis shouldBe 0
        client.readTimeoutMillis shouldBe 0
        client.writeTimeoutMillis shouldBe 0
        client.connectTimeoutMillis shouldBe TimeUnit.SECONDS.toMillis(20).toInt()
    }

    @Test
    fun `gives the three clients three different timeouts`() {
        val timeouts = listOf(
            NetworkModule.provideContentClient(base).callTimeoutMillis,
            NetworkModule.provideSecurityFlagClient(base).callTimeoutMillis,
            NetworkModule.provideDownloadClient(base).callTimeoutMillis,
        )

        timeouts.toSet() shouldHaveSize 3
    }

    @Test
    fun `derives the three clients from one shared connection pool`() {
        val pools = listOf(
            NetworkModule.provideContentClient(base),
            NetworkModule.provideSecurityFlagClient(base),
            NetworkModule.provideDownloadClient(base),
        ).map { it.connectionPool }

        pools.toSet() shouldHaveSize 1
        pools.first() shouldBeSameInstanceAs base.connectionPool
    }

    @Test
    fun `points every endpoint at an https origin`() {
        val options = NetworkModule.provideContentOptions()

        options.cdnBaseUrl shouldStartWith "https://"
        options.catalogueUrl shouldStartWith "https://"
        options.homeContentUrl shouldStartWith "https://"
        options.maintenanceFlagUrl shouldStartWith "https://"
        NetworkModule.provideDownloadOptions().baseUrl shouldStartWith "https://"
    }

    @Test
    fun `caches the maintenance flag for thirty seconds`() {
        NetworkModule.provideContentOptions().maintenanceFlagCacheDuration shouldBe 30.seconds
    }

    @Test
    fun `leaves null coercion off so an explicit null still fails a payload`() {
        NetworkModule.provideJson().configuration.coerceInputValues shouldBe false
        NetworkModule.provideJson().configuration.ignoreUnknownKeys shouldBe true
        NetworkModule.provideJson().configuration.isLenient shouldBe false
    }
}
