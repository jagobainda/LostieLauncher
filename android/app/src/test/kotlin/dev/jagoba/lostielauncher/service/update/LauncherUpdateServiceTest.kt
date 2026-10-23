package dev.jagoba.lostielauncher.service.update

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.LauncherUpdateOptions
import dev.jagoba.lostielauncher.service.apk.ApkPackages
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Path
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("LauncherUpdateService")
class LauncherUpdateServiceTest {
    @TempDir
    lateinit var directory: Path

    private lateinit var server: MockWebServer
    private val dispatcher = StandardTestDispatcher()
    private val transfer = mockk<DownloadTransfer>()
    private val packages = mockk<ApkPackages>(relaxed = true) {
        every { installedVersion(PACKAGE) } returns INSTALLED
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val installer get() = directory.resolve("apk/launcher/LostieLauncher.apk").toFile()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun createSut(): DefaultLauncherUpdateService {
        val dispatchers = TestDispatcherProvider(dispatcher)
        val remoteJson = RemoteJson(OkHttpClient(), Json { ignoreUnknownKeys = true }, dispatchers, logger)
        return DefaultLauncherUpdateService(
            ApkReleaseInstallerFactory(remoteJson, transfer, packages, dispatchers, logger),
            LauncherUpdateOptions(PACKAGE, server.url("/android/latest.json").toString(), installer),
            logger,
        )
    }

    @Test
    fun `offers a newer version`() = runTest(dispatcher) {
        serveRelease("1.1.0")
        val sut = createSut()

        sut.refresh()

        sut.offeredVersion.value shouldBe "1.1.0"
        sut.download.value shouldBe ApkDownloadState.Available
        verify(exactly = 1) { logger.info("Checking for updates...") }
        verify(exactly = 1) { logger.info("Update available: 1.1.0.") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["1.0.0", "1.0.0-beta2", "0.9.9"])
    fun `does not offer the installed version or an older one`(version: String) = runTest(dispatcher) {
        serveRelease(version)
        val sut = createSut()

        sut.refresh()

        sut.offeredVersion.value shouldBe null
        sut.download.value shouldBe ApkDownloadState.Unavailable
        verify(exactly = 1) { logger.info("No updates available.") }
    }

    @Test
    fun `does not offer a download that is not served over HTTPS`() = runTest(dispatcher) {
        serveRelease("1.1.0", "http://example.com/LostieLauncher.apk")
        val sut = createSut()

        sut.refresh()

        sut.offeredVersion.value shouldBe null
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `reads the feed once per process and again after a failed read`() = runTest(dispatcher) {
        server.enqueue(MockResponse.Builder().code(500).build())
        serveRelease("1.1.0")
        val sut = createSut()

        sut.refresh()
        sut.offeredVersion.value shouldBe null

        sut.refresh()
        sut.refresh()

        sut.offeredVersion.value shouldBe "1.1.0"
        server.requestCount shouldBe 2
    }

    @Test
    fun `a manual check reads the feed again and reports what it found`() = runTest(dispatcher) {
        serveRelease("1.0.0")
        serveRelease("1.1.0")
        server.enqueue(MockResponse.Builder().code(500).build())
        val sut = createSut()

        sut.check() shouldBe LauncherUpdateCheckResult.UpToDate
        sut.check() shouldBe LauncherUpdateCheckResult.Available("1.1.0")
        sut.check() shouldBe LauncherUpdateCheckResult.Failed

        server.requestCount shouldBe 3
    }

    @Test
    fun `declining hides the offer until a manual check finds it again`() = runTest(dispatcher) {
        serveRelease("1.1.0")
        serveRelease("1.1.0")
        val sut = createSut()
        sut.refresh()

        sut.decline()
        sut.refresh()
        sut.offeredVersion.value shouldBe null
        verify(exactly = 1) { logger.info("Update to 1.1.0 declined by user.") }

        sut.check()
        sut.offeredVersion.value shouldBe "1.1.0"
    }

    @Test
    fun `opening the system installer answers the offer`() = runTest(dispatcher) {
        serveRelease("1.1.0")
        coEvery { transfer.download(any(), any()) } coAnswers {
            installer.parentFile?.mkdirs()
            installer.writeText("apk")
            DownloadTransferResult.Success
        }
        every { packages.archiveVersion(installer, PACKAGE) } returns "1.1.0"
        val sut = createSut()
        sut.refresh()

        sut.install()

        verify(exactly = 1) { packages.install(installer) }
        sut.offeredVersion.value shouldBe null
    }

    @Test
    fun `acknowledging a failed download answers the offer`() = runTest(dispatcher) {
        serveRelease("1.1.0")
        coEvery { transfer.download(any(), any()) } returns DownloadTransferResult.Failed("boom")
        val sut = createSut()
        sut.refresh()

        sut.install()
        sut.offeredVersion.value shouldBe "1.1.0"
        sut.download.value shouldBe ApkDownloadState.Failed

        sut.acknowledgeFailure()
        sut.offeredVersion.value shouldBe null
        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `deletes a leftover installer once no update is offered`() = runTest(dispatcher) {
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        serveRelease(INSTALLED)
        val sut = createSut()

        sut.refresh()

        installer.parentFile?.exists() shouldBe false
    }

    private fun serveRelease(version: String, url: String = APK_URL) {
        server.enqueue(MockResponse.Builder().body("""{"versionName":"$version","downloadUrl":"$url"}""").build())
    }

    private companion object {
        const val PACKAGE = "dev.jagoba.lostielauncher"
        const val INSTALLED = "1.0.0"
        const val APK_URL = "https://example.com/LostieLauncher.apk"
    }
}
