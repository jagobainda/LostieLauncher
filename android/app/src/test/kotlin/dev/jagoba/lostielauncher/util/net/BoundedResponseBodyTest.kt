package dev.jagoba.lostielauncher.util.net

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BoundedResponseBody")
class BoundedResponseBodyTest {
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
    fun `readBoundedText returns a body at the exact limit`() {
        server.enqueue(MockResponse.Builder().body("12345").build())
        val client = OkHttpClient()

        val text = client.newCall(Request.Builder().url(server.url("/exact")).build()).execute().use { response ->
            response.body.readBoundedText(maxBytes = 5, label = "Feed")
        }

        text shouldBe "12345"
    }

    @Test
    fun `readBoundedText rejects a body whose content length exceeds the limit`() {
        server.enqueue(MockResponse.Builder().body("123456").build())
        val client = OkHttpClient()

        shouldThrow<ResponseTooLargeException> {
            client.newCall(Request.Builder().url(server.url("/large")).build()).execute().use { response ->
                response.body.readBoundedText(maxBytes = 5, label = "Feed")
            }
        }
    }

    @Test
    fun `readBoundedText rejects a chunked body that exceeds the limit`() {
        server.enqueue(MockResponse.Builder().chunkedBody("123456", maxChunkSize = 2).build())
        val client = OkHttpClient()

        shouldThrow<ResponseTooLargeException> {
            client.newCall(Request.Builder().url(server.url("/chunked")).build()).execute().use { response ->
                response.body.readBoundedText(maxBytes = 5, label = "Feed")
            }
        }
    }

    @Test
    fun `interceptor allows a body at the exact limit`() {
        server.enqueue(MockResponse.Builder().body("12345").build())
        val client = OkHttpClient.Builder().addInterceptor(BoundedResponseBodyInterceptor(maxBytes = 5)).build()

        val text = client.newCall(Request.Builder().url(server.url("/exact")).build()).execute().use { response ->
            response.body.string()
        }

        text shouldBe "12345"
    }

    @Test
    fun `interceptor rejects a body whose content length exceeds the limit without exposing the path`() {
        server.enqueue(MockResponse.Builder().body("123456").build())
        val client = OkHttpClient.Builder().addInterceptor(BoundedResponseBodyInterceptor(maxBytes = 5)).build()

        val error = shouldThrow<ResponseTooLargeException> {
            client.newCall(Request.Builder().url(server.url("/games/SECRET-KEY-1234/game.config")).build())
                .execute()
                .close()
        }

        error.message.orEmpty() shouldNotContain "SECRET-KEY-1234"
        error.message.orEmpty() shouldNotContain "game.config"
    }

    @Test
    fun `interceptor rejects a chunked body that exceeds the limit without exposing the path`() {
        server.enqueue(MockResponse.Builder().chunkedBody("123456", maxChunkSize = 2).build())
        val client = OkHttpClient.Builder().addInterceptor(BoundedResponseBodyInterceptor(maxBytes = 5)).build()

        val error = shouldThrow<ResponseTooLargeException> {
            client.newCall(Request.Builder().url(server.url("/games/SECRET-KEY-1234/game.config")).build())
                .execute()
                .use { response -> response.body.string() }
        }

        error.message.orEmpty() shouldNotContain "SECRET-KEY-1234"
        error.message.orEmpty() shouldNotContain "game.config"
    }

    @Test
    fun `interceptor leaves HEAD responses untouched`() {
        server.enqueue(MockResponse.Builder().code(200).addHeader("Content-Length", "100").build())
        val client = OkHttpClient.Builder().addInterceptor(BoundedResponseBodyInterceptor(maxBytes = 5)).build()

        val code = client.newCall(Request.Builder().url(server.url("/head")).head().build()).execute().use { it.code }

        code shouldBe 200
    }
}
