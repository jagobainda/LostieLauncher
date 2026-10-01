package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.util.download.DownloadPathUtils
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("OkHttpDownloadTransfer")
class OkHttpDownloadTransferTest {
    @TempDir
    lateinit var directory: Path

    private lateinit var server: MockWebServer
    private val errors = mutableListOf<Throwable?>()
    private val logger = object : Logger {
        override fun debug(message: String) = Unit

        override fun info(message: String) = Unit

        override fun error(message: String, throwable: Throwable?) {
            errors += throwable
        }
    }
    private var nanos = 0L

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
    fun `writes a complete response and reports success`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("ETag", "\"v1\"")
                .body("downloaded")
                .build(),
        )
        val destination = directory.resolve("game.zip")
        val progress = mutableListOf<DownloadProgress>()

        val result = createSut().download(request(destination)) { progress += it }

        withClue(errors.joinToString { it?.stackTraceToString().orEmpty() }) {
            result shouldBe DownloadTransferResult.Success
        }
        destination.readText() shouldBe "downloaded"
        progress.shouldNotBeEmpty()
        progress.last().percent shouldBe 100.0
    }

    @Test
    fun `resumes an unchanged resource with range and if range`() = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("hello ")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":11}""")
        server.enqueue(MockResponse.Builder().code(206).body("world").build())

        val result = createSut().download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "hello world"
        val recorded = server.takeRequest()
        recorded.headers["Range"] shouldBe "bytes=6-"
        recorded.headers["If-Range"] shouldBe "\"v1\""
    }

    @Test
    fun `rewrites a partial when the server returns a full response`() = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("old")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":3}""")
        server.enqueue(MockResponse.Builder().code(200).body("new").build())

        val result = createSut().download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "new"
    }

    @Test
    fun `discards a partial that has no validator`() = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        part.writeText("old")
        server.enqueue(MockResponse.Builder().code(200).body("fresh").build())

        val result = createSut().download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "fresh"
        server.takeRequest().headers["Range"] shouldBe null
    }

    @Test
    fun `finalizes a complete partial after range not satisfiable`() = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("complete")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":8}""")
        server.enqueue(MockResponse.Builder().code(416).build())

        val result = createSut().download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "complete"
    }

    @Test
    fun `invalidates a mismatched partial and retries from zero`() = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("short")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":8}""")
        server.enqueue(MockResponse.Builder().code(416).build())
        server.enqueue(MockResponse.Builder().code(200).body("complete").build())

        val result = createSut().download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "complete"
        server.requestCount shouldBe 2
    }

    @Test
    fun `retries server failures three times before failing`() = runTest {
        repeat(3) { server.enqueue(MockResponse.Builder().code(500).build()) }

        val result = createSut().download(request(directory.resolve("game.zip"))) {}

        result shouldBe DownloadTransferResult.Failed("Download failed after maximum retries.")
        server.requestCount shouldBe 3
    }

    @Test
    fun `fails when the response body remains inactive beyond the timeout`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body("late")
                .bodyDelay(200, TimeUnit.MILLISECONDS)
                .build(),
        )

        val result = createSut(
            maximumAttempts = 1,
            inactivityTimeout = 25.milliseconds,
        ).download(request(directory.resolve("game.zip"))) {}

        result shouldBe DownloadTransferResult.Failed("Download failed after maximum retries.")
    }

    @Test
    fun `keeps a slow transfer alive while each read arrives before the timeout`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body("alive")
                .throttleBody(1, 20, TimeUnit.MILLISECONDS)
                .build(),
        )
        val destination = directory.resolve("game.zip")

        val result = createSut(
            maximumAttempts = 1,
            inactivityTimeout = 100.milliseconds,
        ).download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "alive"
    }

    private fun request(destination: Path) = DownloadTransferRequest(
        url = server.url("/game.zip").toString(),
        destinationPath = destination.toString(),
    )

    private fun createSut(maximumAttempts: Int = 3, inactivityTimeout: Duration = 1.seconds): OkHttpDownloadTransfer =
        OkHttpDownloadTransfer(
            client = OkHttpClient(),
            options = DownloadTransferOptions(
                cacheDirectoryName = "downloads",
                cacheMaxAge = java.time.Duration.ofDays(14),
                bufferSizeBytes = 4,
                maximumAttempts = maximumAttempts,
                retryBaseDelay = 1.milliseconds,
                inactivityTimeout = inactivityTimeout,
                progressSampleInterval = 1.milliseconds,
                finalizationMaximumAttempts = 3,
                finalizationBaseDelay = 1.milliseconds,
            ),
            dispatchers = RealIoDispatchers,
            timeSource = MonotonicTimeSource {
                nanos += 1_000_000
                nanos
            },
            json = Json,
            logger = logger,
        )

    private object RealIoDispatchers : DispatcherProvider {
        override val io = Dispatchers.IO
        override val default = Dispatchers.Default
        override val main = Dispatchers.Unconfined
    }
}
