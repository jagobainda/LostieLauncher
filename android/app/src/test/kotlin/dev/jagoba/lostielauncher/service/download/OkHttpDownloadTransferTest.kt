package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.core.di.NetworkModule
import dev.jagoba.lostielauncher.model.AllowedHosts
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.util.download.DownloadPathUtils
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource

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
        server.enqueue(
            MockResponse.Builder().code(206).addHeader("Content-Range", "bytes 6-10/11").body("world").build(),
        )

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["bytes 0-4/11", "bytes 7-10/11", "items 6-10/11", "bytes 6-"])
    fun `restarts without a range after an invalid partial response`(range: String?) = runTest {
        val destination = directory.resolve("game.zip")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        part.writeText("hello ")
        Path.of(DownloadPathUtils.getMetaFilePath(part.toString())).writeText("""{"etag":"\"v1\"","totalBytes":11}""")
        val partial = MockResponse.Builder().code(206).body("wrong")
        if (range != null) partial.addHeader("Content-Range", range)
        server.enqueue(partial.build())
        server.enqueue(MockResponse.Builder().body("fresh").build())

        val result = createSut(maximumAttempts = 1).download(request(destination)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "fresh"
        server.takeRequest().headers["Range"] shouldBe "bytes=6-"
        server.takeRequest().headers["Range"] shouldBe null
    }

    @Test
    fun `cancelling a transfer cancels the call while the body is open`() = runTest {
        val receivedBytes = CompletableDeferred<Unit>()
        val observedCall = CompletableDeferred<Call>()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            observedCall.complete(chain.call())
            chain.proceed(chain.request())
        }.build()
        server.enqueue(MockResponse.Builder().body("abcdefgh").throttleBody(4, 2, TimeUnit.SECONDS).build())
        val transfer = createSut(inactivityTimeout = 5.seconds, client = client)
        val job = launch {
            transfer.download(request(directory.resolve("game.zip"))) { receivedBytes.complete(Unit) }
        }

        receivedBytes.await()
        job.cancel()

        observedCall.await().isCanceled() shouldBe true
        job.join()
        server.requestCount shouldBe 1
    }

    @Test
    fun `fails immediately without retrying when content length exceeds maxBytes`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).body("123456").build())
        val destination = directory.resolve("app.apk")

        val result = createSut().download(request(destination, maxBytes = 5)) {}

        result.shouldBeInstanceOf<DownloadTransferResult.Failed>()
        server.requestCount shouldBe 1
        Files.exists(destination) shouldBe false
        Files.exists(Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))) shouldBe false
        Files.exists(
            Path.of(DownloadPathUtils.getMetaFilePath(DownloadPathUtils.getPartFilePath(destination.toString()))),
        ) shouldBe false
    }

    @Test
    fun `fails immediately without retrying when a chunked response exceeds maxBytes`() = runTest {
        server.enqueue(MockResponse.Builder().code(200).chunkedBody("123456", maxChunkSize = 2).build())
        val destination = directory.resolve("app.apk")

        val result = createSut().download(request(destination, maxBytes = 5)) {}

        result.shouldBeInstanceOf<DownloadTransferResult.Failed>()
        server.requestCount shouldBe 1
        Files.exists(destination) shouldBe false
        Files.exists(Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))) shouldBe false
    }

    @Test
    fun `fails and removes partial files when a resumed response exceeds maxBytes`() = runTest {
        val destination = directory.resolve("app.apk")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("1234")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":8}""")
        server.enqueue(
            MockResponse.Builder().code(206).addHeader("Content-Range", "bytes 4-7/8").body("5678").build(),
        )

        val result = createSut().download(request(destination, maxBytes = 6)) {}

        result.shouldBeInstanceOf<DownloadTransferResult.Failed>()
        server.requestCount shouldBe 1
        Files.exists(part) shouldBe false
        Files.exists(metadata) shouldBe false
    }

    @Test
    fun `discards an existing partial file larger than maxBytes and downloads cleanly`() = runTest {
        val destination = directory.resolve("app.apk")
        val part = Path.of(DownloadPathUtils.getPartFilePath(destination.toString()))
        val metadata = Path.of(DownloadPathUtils.getMetaFilePath(part.toString()))
        part.writeText("123456789")
        metadata.writeText("""{"etag":"\"v1\"","totalBytes":9}""")
        server.enqueue(MockResponse.Builder().code(200).body("12345").build())

        val result = createSut().download(request(destination, maxBytes = 5)) {}

        result shouldBe DownloadTransferResult.Success
        destination.readText() shouldBe "12345"
        server.takeRequest().headers["Range"] shouldBe null
    }

    @Test
    fun `fails immediately without retrying when a download redirects to a disallowed host`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .addHeader("Location", "https://evil.example/Kirin.apk")
                .build(),
        )
        val destination = directory.resolve("app.apk")
        val client = NetworkModule.provideDownloadClient(
            NetworkModule.provideBaseHttpClient(NetworkModule.provideUserAgentInterceptor()),
        )
        val allowed = AllowedHosts(exact = setOf(server.hostName))

        val result = createSut(client = client).download(request(destination, allowedHosts = allowed)) {}

        result.shouldBeInstanceOf<DownloadTransferResult.Failed>()
        server.requestCount shouldBe 1
        Files.exists(destination) shouldBe false
    }

    @Test
    fun `follows a redirect to another host permitted by the allowlist`() = runTest {
        MockWebServer().use { cdn ->
            cdn.start()
            server.enqueue(
                MockResponse.Builder()
                    .code(302)
                    .addHeader("Location", cdn.url("/asset/Kirin.apk").toString())
                    .build(),
            )
            cdn.enqueue(MockResponse.Builder().code(200).body("apk-bytes").build())
            val destination = directory.resolve("app.apk")
            val client = NetworkModule.provideDownloadClient(
                NetworkModule.provideBaseHttpClient(NetworkModule.provideUserAgentInterceptor()),
            )
            val allowed = AllowedHosts(exact = setOf(server.hostName, cdn.hostName))

            val result = createSut(client = client).download(request(destination, allowedHosts = allowed)) {}

            result shouldBe DownloadTransferResult.Success
            destination.readText() shouldBe "apk-bytes"
        }
    }

    private fun request(destination: Path, maxBytes: Long? = null, allowedHosts: AllowedHosts? = null) =
        DownloadTransferRequest(
            url = server.url("/game.zip").toString(),
            destinationPath = destination.toString(),
            maxBytes = maxBytes,
            allowedHosts = allowedHosts,
        )

    private fun createSut(
        maximumAttempts: Int = 3,
        inactivityTimeout: Duration = 1.seconds,
        client: OkHttpClient = OkHttpClient(),
    ): OkHttpDownloadTransfer = OkHttpDownloadTransfer(
        client = client,
        options = testTransferOptions(maximumAttempts = maximumAttempts, inactivityTimeout = inactivityTimeout),
        dispatchers = TestDispatcherProvider(Dispatchers.IO),
        timeSource = MonotonicTimeSource {
            nanos += 1_000_000
            nanos
        },
        json = Json,
        logger = logger,
    )
}
