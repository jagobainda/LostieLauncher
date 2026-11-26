package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.SpecialVersionOptions
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.net.BoundedResponseBodyInterceptor
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CdnSpecialVersionService")
class SpecialVersionServiceTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    @Test
    fun `reads key config from injected base and parses it`() = runTest {
        val id = UUID.randomUUID()
        server.enqueue(
            MockResponse.Builder().body(
                "sha256=${"a".repeat(64)}\ntipo=special\njuego-principal=$id\nvers=v2\narchivo=game.zip",
            ).build(),
        )
        val sut = createSut()
        val result = sut.lookup("ABCD-1234-EFGH-5678-IJKL")
        (result as SpecialVersionLookup.Found).config.mainGameId shouldBe id
        server.takeRequest().url.encodedPath shouldBe "/games/ABCD-1234-EFGH-5678-IJKL/game.config"
    }

    @Test
    fun `not found and malformed response stay distinct`() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())
        server.enqueue(MockResponse.Builder().body("invalid").build())
        val sut = createSut()
        sut.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NotFound
        sut.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.InvalidResponse
    }

    @Test
    fun `server error is a download error outcome`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        createSut().lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NetworkError
    }

    @Test
    fun `transport setup failure is a download error outcome`() = runTest {
        val sut = createSut(baseUrl = "invalid-url")
        sut.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NetworkError
    }

    @Test
    fun `rejects an oversized valid config as an invalid response`() = runTest {
        val id = UUID.randomUUID()
        val oversized =
            "sha256=${"a".repeat(64)}\ntipo=special\njuego-principal=$id\nvers=${"v".repeat(64)}\narchivo=game.zip"
        SpecialVersionConfigParser.parse(oversized).shouldNotBeNull()
        server.enqueue(MockResponse.Builder().body(oversized).build())
        server.enqueue(MockResponse.Builder().chunkedBody(oversized, maxChunkSize = 16).build())
        val sut = createSut(maxConfigBytes = 128)

        sut.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.InvalidResponse
        sut.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.InvalidResponse
    }

    @Test
    fun `does not leak the special key in logs when the HTTP interceptor rejects an oversized response`() = runTest {
        val key = "ABCD-1234-EFGH-5678-IJKL"
        val id = UUID.randomUUID()
        val oversized =
            "sha256=${"a".repeat(64)}\ntipo=special\njuego-principal=$id\nvers=${"v".repeat(64)}\narchivo=game.zip"
        server.enqueue(MockResponse.Builder().body(oversized).build())
        val logged = mutableListOf<String>()
        val capturingLogger = object : Logger {
            override fun debug(message: String) {
                logged += message
            }

            override fun info(message: String) {
                logged += message
            }

            override fun error(message: String, throwable: Throwable?) {
                logged += message
                throwable?.let { logged += it.stackTraceToString() }
            }
        }
        val client = OkHttpClient.Builder().addInterceptor(BoundedResponseBodyInterceptor(maxBytes = 32)).build()
        val sut = createSut(client = client, logger = capturingLogger)

        sut.lookup(key) shouldBe SpecialVersionLookup.InvalidResponse
        logged.joinToString("\n") shouldNotContain key
    }

    private fun TestScope.createSut(
        baseUrl: String = server.url("/games").toString(),
        maxConfigBytes: Long = SpecialVersionOptions.DEFAULT_MAX_CONFIG_BYTES,
        client: OkHttpClient = OkHttpClient(),
        logger: Logger = mockk<Logger>(relaxed = true),
    ) = CdnSpecialVersionService(
        client,
        SpecialVersionOptions(baseUrl, "game.config", maxConfigBytes),
        TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
        logger,
    )
}
