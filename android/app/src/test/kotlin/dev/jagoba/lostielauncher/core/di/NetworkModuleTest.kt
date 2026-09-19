package dev.jagoba.lostielauncher.core.di

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeSameInstanceAs
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The composition root's network half, checked the way `CoreModuleTest` checks
 * the rest: by calling the `@Provides` functions directly, because standing up
 * the Hilt graph would need a device.
 *
 * The three clients differ on purpose and the differences are the behaviour —
 * `spec/03-services.md` explains each one — so they are asserted rather than
 * taken on trust.
 */
@DisplayName("NetworkModule")
class NetworkModuleTest {
    private val base = NetworkModule.provideBaseHttpClient(NetworkModule.provideUserAgentInterceptor())

    @Test
    fun `gives the content client a ten-second budget for the whole call`() {
        // Small JSON. A slow CDN should fall back to the cache quickly rather
        // than hold a screen empty while it thinks about it.
        NetworkModule.provideContentClient(base).callTimeoutMillis shouldBe
            TimeUnit.SECONDS.toMillis(10).toInt()
    }

    @Test
    fun `gives the security flag client a three-second budget`() {
        // This check gates every server-backed action, so it must never be what
        // the user waits on.
        NetworkModule.provideSecurityFlagClient(base).callTimeoutMillis shouldBe
            TimeUnit.SECONDS.toMillis(3).toInt()
    }

    @Test
    fun `gives the download client no wall clock at all, only a connect timeout`() {
        // Arrange / Act
        val client = NetworkModule.provideDownloadClient(base)

        // Assert — a multi-gigabyte transfer must not be killed by a wall clock,
        // but a dead host must still fail before any bytes move. Step 08 adds
        // the inactivity watchdog that replaces what these timeouts no longer
        // catch.
        client.callTimeoutMillis shouldBe 0
        client.readTimeoutMillis shouldBe 0
        client.writeTimeoutMillis shouldBe 0
        client.connectTimeoutMillis shouldBe TimeUnit.SECONDS.toMillis(20).toInt()
    }

    @Test
    fun `gives the three clients three different timeouts`() {
        // Using one client for all three would silently break two of the three
        // behaviours, and nothing else in the suite would notice.
        val timeouts = listOf(
            NetworkModule.provideContentClient(base).callTimeoutMillis,
            NetworkModule.provideSecurityFlagClient(base).callTimeoutMillis,
            NetworkModule.provideDownloadClient(base).callTimeoutMillis,
        )

        timeouts.toSet() shouldHaveSize 3
    }

    @Test
    fun `derives the three clients from one shared connection pool`() {
        // Three timeout policies, not three of everything: a client built from
        // scratch would bring its own pool and its own thread pool with it.
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
        // Arrange / Act — cleartext is off on API 28 and above, so an http://
        // endpoint here would fail at runtime rather than at review.
        val options = NetworkModule.provideContentOptions()

        // Assert
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
        // The setting that makes `"nombre": null` a failure rather than an empty
        // name. Nothing else would notice if it were turned on.
        NetworkModule.provideJson().configuration.coerceInputValues shouldBe false
        NetworkModule.provideJson().configuration.ignoreUnknownKeys shouldBe true
        NetworkModule.provideJson().configuration.isLenient shouldBe false
    }
}
