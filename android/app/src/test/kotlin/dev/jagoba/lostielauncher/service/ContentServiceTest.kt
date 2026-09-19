package dev.jagoba.lostielauncher.service

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.ContentOptions
import dev.jagoba.lostielauncher.service.cdn.ContentApi
import dev.jagoba.lostielauncher.service.cdn.MaintenanceFlagApi
import dev.jagoba.lostielauncher.service.cdn.dto.CdnDateTime
import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import dev.jagoba.lostielauncher.service.cdn.dto.NewsItemDto
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Ported from the network half of the desktop's
 * `Services/ContentServiceTests.cs`.
 *
 * The cases about the local registry, playtime and the games directory are not
 * here and are not missing: those members are local persistence and arrive with
 * port plan step 07. The deserialization cases moved to `CdnPayloadTest`, where
 * they run against the real payloads through the real HTTP stack instead of
 * against a hand-written string.
 */
@DisplayName("DefaultContentService")
class ContentServiceTest {
    private val contentApi = mockk<ContentApi>()
    private val flagApi = mockk<MaintenanceFlagApi>()
    private val logger = mockk<Logger>(relaxed = true)

    private val options = ContentOptions(
        cdnBaseUrl = "https://content.test",
        catalogueUrl = "https://content.test/list.json",
        homeContentUrl = "https://content.test/notifications.json",
        maintenanceFlagUrl = "https://content.test/flag.txt",
        maintenanceFlagCacheDuration = 30.seconds,
    )

    /** Advanceable, because the flag cache is the one thing here that expires. */
    private val clock = MutableClock(Instant.parse("2026-06-19T12:00:00Z"))

    private fun createSut() = DefaultContentService(contentApi, flagApi, options, clock, logger)

    /** The flag answers 404, which is what the live CDN answers when maintenance is off. */
    private fun maintenanceOff() {
        coEvery { flagApi.head(any()) } returns 404
    }

    // ---- getGames ----

    @Test
    fun `returns the catalogue the server sent`() = runTest {
        // Arrange
        maintenanceOff()
        coEvery { contentApi.catalogue(options.catalogueUrl) } returns listOf(GameDto(nombre = "Demo"))

        // Act
        val games = createSut().getGames()

        // Assert
        games shouldHaveSize 1
        games[0].name shouldBe "Demo"
    }

    @Test
    fun `does not even ask for the catalogue while maintenance is on`() = runTest {
        // Arrange — the flag is there, so the catalogue endpoint must not be hit
        // at all: during maintenance it may be serving something half-written.
        coEvery { flagApi.head(any()) } returns 200

        // Act
        val games = createSut().getGames()

        // Assert
        games.shouldBeEmpty()
        coVerify(exactly = 0) { contentApi.catalogue(any()) }
    }

    @Test
    fun `returns an empty catalogue and logs when the content server fails`() = runTest {
        // Arrange — the CDN is down, which must not take the app with it.
        maintenanceOff()
        coEvery { contentApi.catalogue(any()) } throws IOException("boom")

        // Act
        val games = createSut().getGames()

        // Assert
        games.shouldBeEmpty()
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `returns an empty catalogue when the payload cannot be read`() = runTest {
        maintenanceOff()
        coEvery { contentApi.catalogue(any()) } throws SerializationException("bad json")

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `returns an empty catalogue when one entry has an unusable id`() = runTest {
        // Arrange — the desktop loses the whole payload to a bad GUID too. It is
        // harsh, and it is the behaviour being ported.
        maintenanceOff()
        coEvery { contentApi.catalogue(any()) } returns listOf(GameDto(id = "not-a-guid"))

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `skips a null catalogue element and keeps the valid entries`() = runTest {
        maintenanceOff()
        coEvery { contentApi.catalogue(any()) } returns listOf(null, GameDto(nombre = "Demo"))

        val games = createSut().getGames()

        games shouldHaveSize 1
        games[0].name shouldBe "Demo"
    }

    @Test
    fun `fetches the catalogue afresh every time, with no cache in the way`() = runTest {
        maintenanceOff()
        coEvery { contentApi.catalogue(any()) } returns emptyList()
        val sut = createSut()

        sut.getGames()
        sut.getGames()

        coVerify(exactly = 2) { contentApi.catalogue(any()) }
    }

    // ---- isServerActionBlocked ----

    @ParameterizedTest
    @ValueSource(ints = [200, 204, 299])
    fun `treats any 2xx on the flag as blocked`(status: Int) = runTest {
        coEvery { flagApi.head(any()) } returns status

        createSut().isServerActionBlocked() shouldBe true
    }

    @ParameterizedTest
    @ValueSource(ints = [301, 403, 404, 500])
    fun `treats anything but a 2xx on the flag as not blocked`(status: Int) = runTest {
        coEvery { flagApi.head(any()) } returns status

        createSut().isServerActionBlocked() shouldBe false
    }

    @Test
    fun `falls back to GET when the server refuses HEAD`() = runTest {
        // Arrange — some CDNs answer 405 to HEAD. The decision then has to come
        // from the GET, whose body is still never read.
        coEvery { flagApi.head(any()) } returns 405
        coEvery { flagApi.get(any()) } returns 200

        createSut().isServerActionBlocked() shouldBe true
        coVerify(exactly = 1) { flagApi.get(options.maintenanceFlagUrl) }
    }

    @Test
    fun `reuses the flag answer inside the cache window`() = runTest {
        maintenanceOff()
        val sut = createSut()

        sut.isServerActionBlocked()
        clock.advanceSeconds(29)
        sut.isServerActionBlocked()

        coVerify(exactly = 1) { flagApi.head(any()) }
    }

    @Test
    fun `probes the flag again once the cache window has passed`() = runTest {
        maintenanceOff()
        val sut = createSut()

        sut.isServerActionBlocked()
        clock.advanceSeconds(31)
        sut.isServerActionBlocked()

        coVerify(exactly = 2) { flagApi.head(any()) }
    }

    @Test
    fun `probes the flag again when the caller forces it`() = runTest {
        maintenanceOff()
        val sut = createSut()

        sut.isServerActionBlocked()
        sut.isServerActionBlocked(forceRefresh = true)

        coVerify(exactly = 2) { flagApi.head(any()) }
    }

    @Test
    fun `assumes not blocked when the flag cannot be reached, and remembers that`() = runTest {
        // Arrange — failing open is the point: an unreachable flag file must not
        // lock a user out of their own launcher.
        coEvery { flagApi.head(any()) } throws IOException("no route to host")
        val sut = createSut()

        // Act
        val blocked = sut.isServerActionBlocked()
        sut.isServerActionBlocked()

        // Assert — and the failure is cached too, so a dead CDN is probed once
        // per window rather than on every action.
        blocked shouldBe false
        coVerify(exactly = 1) { flagApi.head(any()) }
        verify(atLeast = 1) { logger.error(any(), any()) }
    }

    // ---- getHomeContent ----

    @Test
    fun `resolves the home content into the requested language`() = runTest {
        coEvery { contentApi.homeContent(any()) } returns homeContent("Hola", "Hello")

        val content = createSut().getHomeContent(AppLanguage.ENG)

        content.news.single().title shouldBe "Hello"
        content.isStale shouldBe false
    }

    @Test
    fun `re-projects a cached payload when the language changes, without refetching`() = runTest {
        // Arrange — the cache holds the payload unresolved precisely so that
        // switching language is free. The desktop caches the same DTO.
        coEvery { contentApi.homeContent(any()) } returns homeContent("Hola", "Hello")
        val sut = createSut()

        // Act
        sut.getHomeContent(AppLanguage.ESP).news.single().title shouldBe "Hola"
        sut.getHomeContent(AppLanguage.ENG).news.single().title shouldBe "Hello"

        // Assert
        coVerify(exactly = 1) { contentApi.homeContent(any()) }
    }

    @Test
    fun `fetches the home content once and serves the rest from the cache`() = runTest {
        coEvery { contentApi.homeContent(any()) } returns homeContent()
        val sut = createSut()

        sut.getHomeContent(AppLanguage.ESP)
        sut.getHomeContent(AppLanguage.ESP)

        coVerify(exactly = 1) { contentApi.homeContent(any()) }
    }

    @Test
    fun `bypasses the cache when the caller forces a refresh`() = runTest {
        coEvery { contentApi.homeContent(any()) } returns homeContent()
        val sut = createSut()

        sut.getHomeContent(AppLanguage.ESP)
        sut.getHomeContent(AppLanguage.ESP, forceRefresh = true)

        coVerify(exactly = 2) { contentApi.homeContent(any()) }
    }

    @Test
    fun `fetches once when many callers arrive on a cold cache at the same time`() = runTest {
        // Arrange — four screens ask at startup. Without the gate they would
        // open four requests and the last one to finish would win.
        coEvery { contentApi.homeContent(any()) } returns homeContent()
        val sut = createSut()

        // Act
        List(4) { async { sut.getHomeContent(AppLanguage.ESP) } }.awaitAll()

        // Assert
        coVerify(exactly = 1) { contentApi.homeContent(any()) }
    }

    @Test
    fun `degrades to empty content marked stale when the very first fetch fails`() = runTest {
        // Arrange — nothing cached and no network: the screen has to say
        // "unavailable" rather than "there is no news".
        coEvery { contentApi.homeContent(any()) } throws IOException("boom")

        // Act
        val content = createSut().getHomeContent(AppLanguage.ESP)

        // Assert
        content.news.shouldBeEmpty()
        content.isStale shouldBe true
    }

    @Test
    fun `keeps the last known content when a refresh fails`() = runTest {
        // Arrange
        coEvery { contentApi.homeContent(any()) } returns homeContent("Hola")
        val sut = createSut()
        sut.getHomeContent(AppLanguage.ESP)
        coEvery { contentApi.homeContent(any()) } throws IOException("boom")

        // Act
        val content = sut.getHomeContent(AppLanguage.ESP, forceRefresh = true)

        // Assert
        content.news.single().title shouldBe "Hola"
        content.isStale shouldBe true
    }

    @Test
    fun `keeps reporting stale on an unforced read after a failed refresh`() = runTest {
        // Arrange — the flag is sticky, because what is being served is still
        // the old content.
        coEvery { contentApi.homeContent(any()) } returns homeContent("Hola")
        val sut = createSut()
        sut.getHomeContent(AppLanguage.ESP)
        coEvery { contentApi.homeContent(any()) } throws IOException("boom")
        sut.getHomeContent(AppLanguage.ESP, forceRefresh = true)

        // Act
        val content = sut.getHomeContent(AppLanguage.ESP)

        // Assert
        content.isStale shouldBe true
    }

    @Test
    fun `clears the stale flag once a refresh succeeds again`() = runTest {
        coEvery { contentApi.homeContent(any()) } throws IOException("boom")
        val sut = createSut()
        sut.getHomeContent(AppLanguage.ESP)
        coEvery { contentApi.homeContent(any()) } returns homeContent("Hola")

        val content = sut.getHomeContent(AppLanguage.ESP, forceRefresh = true)

        content.news.single().title shouldBe "Hola"
        content.isStale shouldBe false
    }

    @Test
    fun `logs a failed refresh as information, not as an error, when the network is to blame`() = runTest {
        // Arrange — a phone goes through tunnels. A lost connection is expected
        // operation, and filling the error log with it hides the real signal.
        every { logger.info(any()) } just Runs
        coEvery { contentApi.homeContent(any()) } throws IOException("no route to host")

        // Act
        createSut().getHomeContent(AppLanguage.ESP)

        // Assert
        verify(exactly = 0) { logger.error(any(), any()) }
        verify(atLeast = 1) { logger.info(match { it.contains("keeping the last known content") }) }
    }

    @Test
    fun `logs a failed refresh as an error when the network is not to blame`() = runTest {
        coEvery { contentApi.homeContent(any()) } throws SerializationException("bad json")

        createSut().getHomeContent(AppLanguage.ESP).isStale shouldBe true
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    private fun homeContent(spanishTitle: String = "Hola", englishTitle: String = "Hello") = HomeContentDto(
        news = listOf(
            NewsItemDto(
                id = "11111111-1111-1111-1111-111111111111",
                title = mapOf("es" to spanishTitle, "en" to englishTitle),
                description = mapOf("es" to "."),
                tag = "x",
                date = CdnDateTime.parse("2026-01-01T00:00:00"),
            ),
        ),
    )

    /**
     * A clock a test can move.
     *
     * `Clock.fixed` cannot express "thirty-one seconds later", and the
     * alternative — sleeping — is exactly what the testing rules forbid.
     */
    private class MutableClock(private var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId) = this

        override fun instant() = now

        fun advanceSeconds(seconds: Long) {
            now = now.plusSeconds(seconds)
        }
    }
}
