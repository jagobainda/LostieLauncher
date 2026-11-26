package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.AllowedHosts
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RemoteJson")
class RemoteJsonTest {
    private lateinit var server: MockWebServer
    private val dispatcher = StandardTestDispatcher()
    private val logger = mockk<Logger>(relaxed = true)
    private val deserializer = MapSerializer(String.serializer(), String.serializer())
    private val url get() = server.url("/feed.json").toString()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun createSut(client: OkHttpClient = OkHttpClient()) =
        RemoteJson(client, Json, TestDispatcherProvider(dispatcher), logger)

    @Test
    fun `decodes a successful response`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().body("""{"versionName":"1.0.0"}""").build())
        val sut = createSut()

        val result = sut.fetch(url, deserializer, "Feed")

        result shouldBe mapOf("versionName" to "1.0.0")
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `returns nothing and logs at info when the server answers with an error`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().code(503).build())
        val sut = createSut()

        val result = sut.fetch(url, deserializer, "Feed")

        result shouldBe null
        verify(exactly = 1) { logger.info(match { it.contains("Feed could not be read") && it.contains("503") }) }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `returns nothing and logs at info when the server cannot be reached`() = runTest(dispatcher) {
        val unreachableUrl = url
        server.close()
        val sut = createSut()

        val result = sut.fetch(unreachableUrl, deserializer, "Feed")

        result shouldBe null
        verify(exactly = 1) { logger.info(match { it.contains("Feed could not be read") }) }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `returns nothing and logs when the body is not valid JSON`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().body("{not json").build())
        val sut = createSut()

        val result = sut.fetch(url, deserializer, "Feed")

        result shouldBe null
        verify(exactly = 1) { logger.error("Feed could not be read.", any()) }
    }

    @Test
    fun `rethrows a cancellation without logging it`() = runTest(dispatcher) {
        val client = mockk<OkHttpClient> { every { newCall(any()) } throws CancellationException("cancelled") }
        val sut = createSut(client)

        shouldThrow<CancellationException> { sut.fetch(url, deserializer, "Feed") }

        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `returns nothing and logs when the response exceeds the size limit`() = runTest(dispatcher) {
        val body = """{"versionName":"${"1".repeat(64)}"}"""
        server.enqueue(MockResponse.Builder().body(body).build())
        server.enqueue(MockResponse.Builder().chunkedBody(body, maxChunkSize = 8).build())
        val sut = createSut()

        sut.fetch(url, deserializer, "Feed", maxBytes = 32) shouldBe null
        sut.fetch(url, deserializer, "Feed", maxBytes = 32) shouldBe null
        verify(exactly = 2) { logger.error("Feed could not be read.", any()) }
    }

    @Test
    fun `rejects non-HTTPS and disallowed hosts before contacting the client when an allowlist is set`() =
        runTest(dispatcher) {
            val client = mockk<OkHttpClient>()
            val allowed = AllowedHosts(exact = setOf("api.github.com"))
            val sut = createSut(client)

            sut.fetch("http://api.github.com/feed.json", deserializer, "Feed", allowedHosts = allowed) shouldBe null
            sut.fetch("https://evil.example/feed.json", deserializer, "Feed", allowedHosts = allowed) shouldBe null

            verify(exactly = 0) { client.newCall(any()) }
            verify(exactly = 2) { logger.error("Feed could not be read.", any()) }
        }

    @Test
    fun `attaches the host allowlist tag to outgoing requests`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().body("""{"versionName":"1.0.0"}""").build())
        val allowed = AllowedHosts(exact = setOf("api.github.com"))
        var observedTag: AllowedHosts? = null
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                observedTag = request.tag(AllowedHosts::class.java)
                chain.proceed(request.newBuilder().url(server.url(request.url.encodedPath)).build())
            }
            .build()
        val sut = createSut(client)

        val result = sut.fetch("https://api.github.com/feed.json", deserializer, "Feed", allowedHosts = allowed)

        result shouldBe mapOf("versionName" to "1.0.0")
        observedTag shouldBe allowed
    }
}
