package dev.jagoba.lostielauncher.service.link

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.core.di.NetworkModule
import dev.jagoba.lostielauncher.model.TwitchLiveOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("PreviewTwitchLiveService")
class PreviewTwitchLiveServiceTest {
    private lateinit var server: MockWebServer

    private val logger = mockk<Logger>(relaxed = true)

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
    fun `is live when the preview answers directly`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).build())

        val live = createSut().isLive()

        live shouldBe true
        server.takeRequest().method shouldBe "HEAD"
    }

    @Test
    fun `is not live when the preview redirects to the offline placeholder`() = runTest {
        server.enqueue(MockResponse.Builder().code(302).addHeader("Location", "/404_preview.jpg").build())
        server.enqueue(MockResponse.Builder().code(200).build())

        val live = createSut().isLive()

        live shouldBe false
        server.requestCount shouldBe 1
    }

    @Test
    fun `is not live and logs when the preview cannot be reached`() = runTest {
        val url = server.url("/preview.jpg").toString()
        server.close()

        val live = createSut(url).isLive()

        live shouldBe false
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `a malformed preview URL only disables the live indicator`() = runTest {
        createSut("invalid").isLive() shouldBe false

        verify(exactly = 1) { logger.error(any(), ofType<IllegalArgumentException>()) }
    }

    @Test
    fun `cancellation aborts the preview request without reporting a lookup failure`() = runTest {
        val observedCall = CompletableDeferred<Call>()
        val client = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            observedCall.complete(chain.call())
            chain.proceed(chain.request())
        }.build()
        server.enqueue(MockResponse.Builder().headersDelay(2, TimeUnit.SECONDS).build())
        val sut = createSut(client = client)
        val job = launch { sut.isLive() }

        val call = observedCall.await()
        job.cancel()

        call.isCanceled() shouldBe true
        job.join()
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    private fun createSut(url: String = server.url("/preview.jpg").toString(), client: OkHttpClient = OkHttpClient()) =
        PreviewTwitchLiveService(
            NetworkModule.provideTwitchPreviewClient(client),
            TwitchLiveOptions(url),
            TestDispatcherProvider(Dispatchers.IO),
            logger,
        )
}
