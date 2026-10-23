package dev.jagoba.lostielauncher.service.apk

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ApkTarget
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadTransferRequest
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
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

@DisplayName("ApkReleaseInstaller")
class ApkReleaseInstallerTest {
    @TempDir
    lateinit var directory: Path

    private lateinit var server: MockWebServer
    private val dispatcher = StandardTestDispatcher()
    private val transfer = mockk<DownloadTransfer>()
    private val packages = mockk<ApkPackages>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val installer get() = directory.resolve("apk/target/Target.apk").toFile()
    private val feedUrl get() = server.url(FEED_PATH).toString()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun createSut(): ApkReleaseInstaller {
        val dispatchers = TestDispatcherProvider(dispatcher)
        return ApkReleaseInstaller(
            ApkTarget(PACKAGE, installer, "Target"),
            RemoteJson(OkHttpClient(), Json { ignoreUnknownKeys = true }, dispatchers, logger),
            transfer,
            packages,
            dispatchers,
            logger,
        )
    }

    @Test
    fun `starts checking and offers a newer release once settled`() = runTest(dispatcher) {
        serveRelease("0.4.2")
        val sut = createSut()
        sut.download.value shouldBe ApkDownloadState.Checking

        sut.loadRelease(feedUrl)
        val offered = sut.settle("0.4.0", null)

        offered shouldBe ApkReleaseDto("0.4.2", APK_URL)
        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `reads the installed version of its own package`() = runTest(dispatcher) {
        every { packages.installedVersion(PACKAGE) } returns "1.0.0"
        val sut = createSut()

        sut.installedVersion() shouldBe "1.0.0"
    }

    @Test
    fun `does not offer a download that is not served over HTTPS`() = runTest(dispatcher) {
        serveRelease("0.4.2", "http://example.com/Target.apk")
        val sut = createSut()

        sut.loadRelease(feedUrl) shouldBe null
        sut.settle("0.4.0", null) shouldBe null

        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `reads the feed once unless forced`() = runTest(dispatcher) {
        serveRelease("0.4.2")
        serveRelease("0.4.3")
        val sut = createSut()

        sut.loadRelease(feedUrl)
        sut.loadRelease(feedUrl)
        server.requestCount shouldBe 1

        sut.loadRelease(feedUrl, force = true)?.versionName shouldBe "0.4.3"
        server.requestCount shouldBe 2
    }

    @Test
    fun `reads the feed again after a failed read`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().code(500).build())
        serveRelease("0.4.2")
        val sut = createSut()

        sut.loadRelease(feedUrl) shouldBe null
        sut.loadRelease(feedUrl)?.versionName shouldBe "0.4.2"
    }

    @Test
    fun `downloads the release, verifies it and opens the system installer`() = runTest(dispatcher) {
        val sut = offeringRelease()
        val observed = mutableListOf<ApkDownloadState>()
        downloadSucceeds { observed += sut.download.value }
        every { packages.archiveVersion(installer, PACKAGE) } returns "0.4.2"

        sut.install() shouldBe true

        observed shouldBe listOf(ApkDownloadState.Downloading(50))
        sut.download.value shouldBe ApkDownloadState.Available
        coVerify(exactly = 1) { transfer.download(DownloadTransferRequest(APK_URL, installer.path), any()) }
        verify(exactly = 1) { packages.install(installer) }
    }

    @Test
    fun `reuses a verified installer instead of downloading it again`() = runTest(dispatcher) {
        val sut = offeringRelease()
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        every { packages.archiveVersion(installer, PACKAGE) } returns "0.4.2"

        sut.install()

        coVerify(exactly = 0) { transfer.download(any(), any()) }
        verify(exactly = 1) { packages.install(installer) }
    }

    @Test
    fun `downloads again when the kept installer is older than the release`() = runTest(dispatcher) {
        val sut = offeringRelease()
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        every { packages.archiveVersion(installer, PACKAGE) } returnsMany listOf("0.4.1", "0.4.2")
        downloadSucceeds()

        sut.install()

        coVerify(exactly = 1) { transfer.download(any(), any()) }
        verify(exactly = 1) { packages.install(installer) }
    }

    @Test
    fun `rejects and deletes a download that is not the released package`() = runTest(dispatcher) {
        val sut = offeringRelease()
        downloadSucceeds()
        every { packages.archiveVersion(installer, PACKAGE) } returns null

        sut.install() shouldBe false

        sut.download.value shouldBe ApkDownloadState.Failed
        installer.exists() shouldBe false
        verify(exactly = 0) { packages.install(any()) }
    }

    @Test
    fun `keeps a failed download reported until it is acknowledged`() = runTest(dispatcher) {
        val sut = offeringRelease()
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Failed("boom")

        sut.install()
        sut.settle("0.4.0", null)
        sut.download.value shouldBe ApkDownloadState.Failed

        sut.acknowledgeFailure()
        sut.download.value shouldBe ApkDownloadState.Available
        verify(exactly = 0) { packages.install(any()) }
    }

    @Test
    fun `reports a failure when the system installer cannot be opened`() = runTest(dispatcher) {
        val sut = offeringRelease()
        downloadSucceeds()
        every { packages.archiveVersion(installer, PACKAGE) } returns "0.4.2"
        every { packages.install(any()) } throws IllegalStateException("No activity")

        sut.install() shouldBe false

        sut.download.value shouldBe ApkDownloadState.Failed
    }

    @Test
    fun `offers the release again when the download is cancelled`() = runTest(dispatcher) {
        val sut = offeringRelease()
        coEvery { transfer.download(any(), any()) } throws CancellationException("Scope cancelled")

        shouldThrow<CancellationException> { sut.install() }

        sut.download.value shouldBe ApkDownloadState.Available
        verify(exactly = 0) { packages.install(any()) }
    }

    @Test
    fun `does nothing while no release is offered`() = runTest(dispatcher) {
        serveRelease("0.4.2")
        val sut = createSut()
        sut.loadRelease(feedUrl)
        sut.settle("0.4.2", null)

        sut.install() shouldBe false

        coVerify(exactly = 0) { transfer.download(any(), any()) }
    }

    @Test
    fun `deletes the target's installer directory`() = runTest(dispatcher) {
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        val sut = createSut()

        sut.deleteInstaller()

        installer.parentFile?.exists() shouldBe false
    }

    private suspend fun offeringRelease(): ApkReleaseInstaller {
        serveRelease("0.4.2")
        return createSut().also {
            it.loadRelease(feedUrl)
            it.settle("0.4.0", null)
        }
    }

    private fun downloadSucceeds(onProgress: () -> Unit = {}) {
        coEvery { transfer.download(any(), any()) } coAnswers {
            secondArg<suspend (DownloadProgress) -> Unit>()(DownloadProgress(50.0, 0.0, 1, 2))
            onProgress()
            installer.parentFile?.mkdirs()
            installer.writeText("apk")
            DownloadTransferResult.Success
        }
    }

    private fun serveRelease(version: String, url: String = APK_URL) {
        server.enqueue(
            MockResponse.Builder()
                .body("""{"versionCode":23,"versionName":"$version","downloadUrl":"$url","required":false}""")
                .build(),
        )
    }

    private companion object {
        const val PACKAGE = "dev.example.target"
        const val FEED_PATH = "/latest.json"
        const val APK_URL = "https://example.com/Target-0.4.2.apk"
    }
}
