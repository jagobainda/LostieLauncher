package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.SpecialVersionOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        val service = createService()
        val result = service.lookup("ABCD-1234-EFGH-5678-IJKL")
        (result as SpecialVersionLookup.Found).config.mainGameId shouldBe id
        server.takeRequest().url.encodedPath shouldBe "/games/ABCD-1234-EFGH-5678-IJKL/game.config"
    }

    @Test
    fun `not found and malformed response stay distinct`() = runTest {
        server.enqueue(MockResponse.Builder().code(404).build())
        server.enqueue(MockResponse.Builder().body("invalid").build())
        val service = createService()
        service.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NotFound
        service.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.InvalidResponse
    }

    @Test
    fun `server error is a download error outcome`() = runTest {
        server.enqueue(MockResponse.Builder().code(500).build())
        createService().lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NetworkError
    }

    @Test
    fun `transport setup failure is a download error outcome`() = runTest {
        val service = CdnSpecialVersionService(
            OkHttpClient(),
            SpecialVersionOptions("invalid-url", "game.config"),
            TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
            mockk<Logger>(relaxed = true),
        )
        service.lookup("ABCD-1234-EFGH-5678-IJKL") shouldBe SpecialVersionLookup.NetworkError
    }

    private fun kotlinx.coroutines.test.TestScope.createService() = CdnSpecialVersionService(
        OkHttpClient(),
        SpecialVersionOptions(server.url("/games").toString(), "game.config"),
        TestDispatcherProvider(testScheduler.let { kotlinx.coroutines.test.StandardTestDispatcher(it) }),
        mockk<Logger>(relaxed = true),
    )
}
