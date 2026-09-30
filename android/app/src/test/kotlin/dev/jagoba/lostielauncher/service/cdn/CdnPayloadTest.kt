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

@DisplayName("CDN payloads")
class CdnPayloadTest {
    private lateinit var server: MockWebServer

    private val logger = mockk<Logger>(relaxed = true)

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

    @Test
    fun `reads the live game catalogue`() = runTest {
        enqueueCatalogue(readResource("cdn/listado.json"))
        val sut = createSut()

        val games = sut.getGames()

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

    @Test
    fun `reads the live home content in each of the eight languages`() = runTest {
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))
        val sut = createSut()

        val titles = AppLanguage.entries.associateWith { sut.getHomeContent(it).news.single().title }

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
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))

        val content = createSut().getHomeContent(AppLanguage.ESP)

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
        enqueueHomeContent(readResource("cdn/homepage-notifications.json"))
        val sut = createSut(clock = Clock.fixed(Instant.parse("2026-10-23T00:00:00Z"), ZoneOffset.UTC))

        val content = sut.getHomeContent(AppLanguage.ESP)

        content.news.shouldBeEmpty()
        content.notifications.shouldBeEmpty()
        content.isStale shouldBe false
    }

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
        enqueueCatalogue("""[{"id":"11111111-1111-1111-1111-111111111111","nombre":null,"version":"1.0.0"}]""")

        createSut().getGames().shouldBeEmpty()
    }

    @Test
    fun `keeps an entry whose name is merely absent`() = runTest {
        enqueueCatalogue("""[{"id":"11111111-1111-1111-1111-111111111111","version":"1.0.0"}]""")

        val games = createSut().getGames()

        games shouldHaveSize 1
        games[0].name shouldBe ""
        games[0].gameId shouldBe ""
    }

    @Test
    fun `keeps reading a catalogue that has grown a field this build knows nothing about`() = runTest {
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
        enqueueHomeContent(
            """{"news":[],"notifications":[{"title":{"es":"x"},"message":{"es":"y"},"type":"Catastrophe",
            |"date":"2026-01-01T00:00:00"}]}
            """.trimMargin(),
        )

        createSut().getHomeContent(AppLanguage.ESP).isStale shouldBe true
    }

    @Test
    fun `reads the maintenance flag with a HEAD that never touches the body`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("blocked").build())
        val sut = createSut()

        val blocked = sut.isServerActionBlocked()

        blocked shouldBe true
        server.takeRequest().method shouldBe "HEAD"
    }

    @Test
    fun `asks again with GET when the server answers 405 to HEAD`() = runTest {
        server.enqueue(MockResponse.Builder().code(405).build())
        server.enqueue(MockResponse.Builder().code(200).body("blocked").build())

        val blocked = createSut().isServerActionBlocked()

        blocked shouldBe true
        server.takeRequest().method shouldBe "HEAD"
        server.takeRequest().method shouldBe "GET"
    }

    @Test
    fun `sends the launcher user agent on every request`() = runTest {
        enqueueCatalogue(readResource("cdn/listado.json"))

        createSut().getGames()

        server.takeRequest().headers["User-Agent"] shouldStartWith "LostieLauncher/"
    }

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
        val client = OkHttpClient.Builder()
            .addInterceptor(NetworkModule.provideUserAgentInterceptor())
            .build()
        val contentApi = Retrofit.Builder()
            .baseUrl("$base/")
            .client(client)
            .addConverterFactory(NetworkModule.provideJson().asConverterFactory(JSON_MEDIA_TYPE))
            .build()
            .create(ContentApi::class.java)
        val flagApi = OkHttpMaintenanceFlagApi(client, TestDispatchers)

        return DefaultContentService(contentApi, flagApi, options, clock, logger)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }

    private object TestDispatchers : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Unconfined
    }
}
