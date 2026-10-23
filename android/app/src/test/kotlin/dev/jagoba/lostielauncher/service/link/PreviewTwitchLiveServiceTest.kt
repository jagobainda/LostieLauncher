package dev.jagoba.lostielauncher.service.link

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.core.di.NetworkModule
import dev.jagoba.lostielauncher.model.TwitchLiveOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
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

    private fun createSut(url: String = server.url("/preview.jpg").toString()) = PreviewTwitchLiveService(
        NetworkModule.provideTwitchPreviewClient(OkHttpClient()),
        TwitchLiveOptions(url),
        TestDispatcherProvider(Dispatchers.IO),
        logger,
    )
}
