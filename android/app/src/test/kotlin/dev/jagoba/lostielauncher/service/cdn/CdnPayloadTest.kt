package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.core.di.NetworkModule
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.ContentOptions
import dev.jagoba.lostielauncher.model.NotificationType
import dev.jagoba.lostielauncher.service.DefaultContentService
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.mockk
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The whole read path, end to end, against the payloads the CDN actually
 * serves.
 *
 * `app/src/test/resources/cdn/` holds byte-for-byte captures of
 * `https://ericlostie-launcher.jagoba.dev/games/listado.json` and
 * `https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json`,
 * downloaded on 2026-09-19. They are identical in content to the captures in
 * `spec/samples/`, which were taken a day earlier. Re-capture them by fetching
 * those two URLs again.
 *
 * Nothing here is mocked below the socket: a real `OkHttpClient` talks to a
 * loopback `MockWebServer` through the real Retrofit stack and the real
 * `Json`. That is the only way a test can prove the payload parses rather than
 * prove that a hand-written fixture does.
 */
@DisplayName("CDN payloads")
class CdnPayloadTest {
    private lateinit var server: MockWebServer

    private val logger = mockk<Logger>(relaxed = true)

    /** After the captured payload's `date` and well before its `expires_at`. */
    private val clock: Clock = Clock.fixed(Instant.parse("2026-09-19T12:00:00Z"), ZoneOffset.UTC)

    @BeforeEach
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun stopServer() {
        server.close()
    }

    // ---- the real catalogue ----

    @Test
    fun `reads the live game catalogue`() = runTest {
        // Arrange
        enqueueCatalogue(readResource("cdn/listado.json"))
        val sut = createSut()

        // Act
        val games = sut.getGames()

        // Assert — seven entries at the time of capture, and the first one is
        // the flagship game.
        games shouldHaveSize 7
        val anil = games.first()
        anil.id shouldBe UUID.fromString("6b49940d-5910-49e5-aab8-f933cd51c388")
        anil.name shouldBe "Pokémon Añil"
        anil.version shouldBe "v4.13.0"
        anil.sizeGb shouldBe 0.5889
        anil.formattedSize shouldBe "603 MB"
        anil.gameId shouldBe "pok-mon-a-il"
        anil.relativePath shouldBe "/pokemon-anil/4-13-0.zip"
        anil.sha256 shouldBe "e379a48072c42942fd4f3f99b6aed0c4d1aff3d1f691448d65af76e035e17ac5"
    }

    @Test
    fun `resolves every logo in the live catalogue against the CDN base`() = runTest {
        enqueueCatalogue(readResource("cdn/listado.json"))

        val games = createSut().getGames()

        games.forEach { it.logoUrl shouldStartWith server.url("/").toString().removeSuffix("/") + "/public/imgs/" }
    }

    // ---- the real home content ----

    @Test
    fun `reads the live home content in each of the eight languages`() = runTest {
        // Arrange — one fetch, projected eight times. That it takes one fetch
        // is half the point: the cache holds the payload unresolved, so a
        // language change costs nothing.
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))
        val sut = createSut()

        // Act
        val titles = AppLanguage.entries.associateWith { sut.getHomeContent(it).news.single().title }

        // Assert — the values are the payload's own, including Valencian, whose
        // key is the three-letter `val` and whose text really is the Catalan
        // one. Nothing here falls back.
        titles[AppLanguage.ESP] shouldBe "v0.9.1 Beta Abierta ya disponible"
        titles[AppLanguage.ENG] shouldBe "v0.9.1 Open Beta now available"
        titles[AppLanguage.CAT] shouldBe "v0.9.1 Beta Oberta ja disponible"
        titles[AppLanguage.EUS] shouldBe "v0.9.1 Beta Irekia eskuragarri dago"
        titles[AppLanguage.GAL] shouldBe "v0.9.1 Beta Aberta xa dispoñible"
        titles[AppLanguage.POR] shouldBe "v0.9.1 Beta Aberta já disponível"
        titles[AppLanguage.VAL] shouldBe "v0.9.1 Beta Oberta ja disponible"
        titles[AppLanguage.FRA] shouldBe "v0.9.1 Bêta Ouverte désormais disponible"
        server.requestCount shouldBe 1
    }

    @Test
    fun `reads the live home content into Spanish`() = runTest {
        // Arrange
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))

        // Act
        val content = createSut().getHomeContent(AppLanguage.ESP)

        // Assert
        content.isStale shouldBe false
        val news = content.news.single()
        news.id shouldBe UUID.fromString("3b5d0ad6-9cf6-434c-8da8-25b971f5f053")
        news.title shouldBe "v0.9.1 Beta Abierta ya disponible"
        news.tag shouldBe "Release"
        news.date shouldBe LocalDateTime.of(2026, 7, 24, 0, 0)
        news.expiresAt shouldBe LocalDateTime.of(2026, 10, 21, 0, 0)

        val notification = content.notifications.single()
        notification.id shouldBe UUID.fromString("140431da-5f94-4a41-b8eb-32a926e3019d")
        notification.type shouldBe NotificationType.INFO
    }

    @Test
    fun `drops the live items once their expiry has passed`() = runTest {
        // Arrange — the captured items expire on 21 and 22 October 2026. A test
        // pinned to a clock is the only reason this suite does not start failing
        // on that date.
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))
        val sut = createSut(clock = Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"), ZoneOffset.UTC))

        // Act
        val content = sut.getHomeContent(AppLanguage.ESP)

        // Assert
        content.news.shouldBeEmpty()
        content.notifications.shouldBeEmpty()
        content.isStale shouldBe false
    }

    // ---- degradation ----

    @Test
    fun `degrades to an empty catalogue when the server answers 500`() = runTest {
        enqueueCatalogueStatus(500)

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `degrades to an empty catalogue when the body is not JSON`() = runTest {
        enqueueCatalogue("<html>maintenance</html>")

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `degrades to an empty catalogue when an entry has an explicit null name`() = runTest {
        // Arrange — desktop BUG-053. An explicit null is not a missing field:
        // it fails the payload, because a null name would crash a card later.
        enqueueCatalogue("""[{"id":"11111111-1111-1111-1111-111111111111","nombre":null,"version":"1.0.0"}]""")

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `keeps an entry whose name is merely absent`() = runTest {
        // Arrange — the other half of the same rule: a *missing* property keeps
        // the model default and the catalogue still loads.
        enqueueCatalogue("""[{"id":"11111111-1111-1111-1111-111111111111","version":"1.0.0"}]""")

        val games = createSut().getGames()

        games shouldHaveSize 1
        games[0].name shouldBe ""
        games[0].gameId shouldBe ""
    }

    @Test
    fun `keeps reading a catalogue that has grown a field this build knows nothing about`() = runTest {
        // Arrange — the CDN must be able to add a field without breaking every
        // launcher already installed.
        enqueueCatalogue("""[{"nombre":"Demo","somethingNew":{"nested":true}}]""")

        createSut().getGames() shouldHaveSize 1
    }

    @Test
    fun `degrades to stale empty content when the home payload is not JSON`() = runTest {
        enqueueHomeContent("nonsense")

        val content = createSut().getHomeContent(AppLanguage.ESP)

        content.news.shouldBeEmpty()
        content.isStale shouldBe true
    }

    @Test
    fun `degrades to stale empty content when the notification type is unknown`() = runTest {
        // Arrange — a severity this build cannot render is better not shown than
        // shown as harmless.
        enqueueHomeContent(
            """{"news":[],"notifications":[{"title":{"es":"x"},"message":{"es":"y"},"type":"Catastrophe",
            |"date":"2026-01-01T00:00:00"}]}
            """.trimMargin(),
        )

        createSut().getHomeContent(AppLanguage.ESP).isStale shouldBe true
    }

    // ---- the maintenance flag over a real socket ----

    @Test
    fun `reads the maintenance flag with a HEAD that never touches the body`() = runTest {
        // Arrange
        server.enqueue(MockResponse.Builder().code(200).body("blocked").build())
        val sut = createSut()

        // Act
        val blocked = sut.isServerActionBlocked()

        // Assert
        blocked shouldBe true
        server.takeRequest().method shouldBe "HEAD"
    }

    @Test
    fun `asks again with GET when the server answers 405 to HEAD`() = runTest {
        // Arrange
        server.enqueue(MockResponse.Builder().code(405).build())
        server.enqueue(MockResponse.Builder().code(200).body("blocked").build())

        // Act
        val blocked = createSut().isServerActionBlocked()

        // Assert
        blocked shouldBe true
        server.takeRequest().method shouldBe "HEAD"
        server.takeRequest().method shouldBe "GET"
    }

    @Test
    fun `sends the launcher user agent on every request`() = runTest {
        // Arrange — the CDN's logs are how the two clients are told apart.
        enqueueCatalogue(readResource("cdn/listado.json"))

        // Act
        createSut().getGames()

        // Assert
        server.takeRequest().headers["User-Agent"] shouldStartWith "LostieLauncher/"
    }

    /**
     * Queues a catalogue response.
     *
     * The flag is probed before the catalogue is fetched, so the 404 that means
     * "maintenance is off" has to be first in the queue. Home content does not
     * go through the flag, which is why it has its own helper.
     */
    private fun enqueueCatalogue(body: String) {
        server.enqueue(MockResponse.Builder().code(404).build())
        server.enqueue(MockResponse.Builder().code(200).body(body).build())
    }

    private fun enqueueCatalogueStatus(code: Int) {
        server.enqueue(MockResponse.Builder().code(404).build())
        server.enqueue(MockResponse.Builder().code(code).build())
    }

    private fun enqueueHomeContent(body: String) {
        server.enqueue(MockResponse.Builder().code(200).body(body).build())
    }

    private fun readResource(path: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream(path)) { "Missing test resource: $path" }
            .bufferedReader()
            .use { it.readText() }

    private fun createSut(clock: Clock = this.clock): DefaultContentService {
        val base = server.url("/").toString().removeSuffix("/")
        val options = ContentOptions(
            cdnBaseUrl = base,
            catalogueUrl = "$base/games/listado.json",
            homeContentUrl = "$base/homepage-notifications.json",
            maintenanceFlagUrl = "$base/flag.txt",
            maintenanceFlagCacheDuration = 30.seconds,
        )
        // The production interceptor, not a stand-in: the user-agent header is
        // asserted below and a test double would assert itself.
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkModule.provideUserAgentInterceptor())
            .build()
        val contentApi = Retrofit.Builder()
            .baseUrl("$base/")
            .client(client)
            // The production `Json` too, for the same reason as the interceptor.
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(ContentApi::class.java)
        val flagApi = OkHttpMaintenanceFlagApi(client, TestDispatchers)

        return DefaultContentService(contentApi, flagApi, options, clock, logger)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    /** Real I/O over loopback, so the calls need a real dispatcher rather than a test one. */
    private object TestDispatchers : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Unconfined
    }
}
