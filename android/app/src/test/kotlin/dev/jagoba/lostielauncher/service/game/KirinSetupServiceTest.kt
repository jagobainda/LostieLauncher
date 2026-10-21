package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.apk.ApkPackages
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadTransferRequest
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("KirinSetupService")
class KirinSetupServiceTest {
    @TempDir
    lateinit var directory: Path

    private lateinit var server: MockWebServer
    private val responses = ConcurrentHashMap<String, MockResponse>()
    private val dispatcher = StandardTestDispatcher()
    private val transfer = mockk<DownloadTransfer>()
    private val packages = mockk<ApkPackages>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val installer get() = directory.resolve("apk/kirin/Kirin.apk").toFile()

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                responses[request.url.encodedPath] ?: MockResponse.Builder().code(404).build()
        }
        server.start()
    }

    @AfterEach
    fun tearDown() {
        server.close()
    }

    private fun createSut(): DefaultKirinSetupService {
        val dispatchers = TestDispatcherProvider(dispatcher)
        val remoteJson = RemoteJson(OkHttpClient(), Json { ignoreUnknownKeys = true }, dispatchers, logger)
        return DefaultKirinSetupService(
            remoteJson,
            ApkReleaseInstallerFactory(remoteJson, transfer, packages, dispatchers, logger),
            KirinOptions(File(""), File(""), "", "", KIRIN_PACKAGE, "", "", ""),
            KirinSetupOptions(server.url(REQUIREMENTS_PATH).toString(), FALLBACK_MINIMUM, installer),
            logger,
        )
    }

    @Test
    fun `blocks a missing Kirin with the server's minimum version`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns null
        serveRequirements(minimum = "0.4.1")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Missing("0.4.1")
    }

    @Test
    fun `blocks an installation below the server's minimum version`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.4.0"
        serveRequirements(minimum = "0.4.1")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Outdated("0.4.0", "0.4.1")
    }

    @Test
    fun `recommends the feed's release when it is newer than the installed Kirin`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.4.0-beta8"
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.UpdateRecommended("0.4.0-beta8", "0.4.2")
        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `deletes a leftover installer once Kirin is at the feed's release`() = runTest(dispatcher) {
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.4.2"
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Ready
        sut.download.value shouldBe ApkDownloadState.Unavailable
        installer.parentFile?.exists() shouldBe false
    }

    @Test
    fun `falls back to the built-in minimum and retries the requirements on the next refresh`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.3.5"
        val sut = createSut()

        sut.refresh()
        sut.status.value shouldBe KirinStatus.Outdated("0.3.5", FALLBACK_MINIMUM)
        sut.download.value shouldBe ApkDownloadState.Unavailable

        serveRequirements(minimum = "0.3.0")
        sut.refresh()
        sut.status.value shouldBe KirinStatus.Ready
    }

    @Test
    fun `offers the feed's release to a missing Kirin`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `does not offer a release below the minimum version`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.3.5")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `does not offer a download that is not served over HTTPS`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.4.0"
        serveRequirements()
        serveRelease("0.4.2", "http://example.com/Kirin.apk")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Ready
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `asks nothing more of the network once the requirements and the feed are known`() = runTest(dispatcher) {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()
        sut.refresh()

        server.requestCount shouldBe 2
    }

    @Test
    fun `downloads the release, verifies it and opens the system installer end to end`() = runTest(dispatcher) {
        val sut = offeringRelease()
        val observed = mutableListOf<ApkDownloadState>()
        downloadSucceeds { observed += sut.download.value }
        every { packages.archiveVersion(installer, KIRIN_PACKAGE) } returns "0.4.2"

        sut.install()

        observed shouldBe listOf(ApkDownloadState.Downloading(50))
        sut.download.value shouldBe ApkDownloadState.Available
        coVerify(exactly = 1) { transfer.download(DownloadTransferRequest(APK_URL, installer.path), any()) }
        verify(exactly = 1) { packages.install(installer) }
    }

    private suspend fun offeringRelease(): DefaultKirinSetupService {
        every { packages.installedVersion(KIRIN_PACKAGE) } returns "0.4.0"
        serveRequirements()
        serveRelease("0.4.2")
        return createSut().also { it.refresh() }
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

    private fun serveRequirements(minimum: String = "0.4.0") {
        serve(REQUIREMENTS_PATH, """{"minimumVersion":"$minimum","releaseFeedUrl":"${server.url(FEED_PATH)}"}""")
    }

    private fun serveRelease(version: String, url: String = APK_URL) {
        serve(FEED_PATH, """{"versionCode":23,"versionName":"$version","downloadUrl":"$url","required":false}""")
    }

    private fun serve(path: String, body: String) {
        responses[path] = MockResponse.Builder().body(body).build()
    }

    private companion object {
        const val REQUIREMENTS_PATH = "/kirin-setup-options.json"
        const val FEED_PATH = "/latest.json"
        const val FALLBACK_MINIMUM = "0.4.0"
        const val KIRIN_PACKAGE = "com.gmax.kirin"
        const val APK_URL = "https://example.com/Kirin-0.4.2.apk"
    }
}
