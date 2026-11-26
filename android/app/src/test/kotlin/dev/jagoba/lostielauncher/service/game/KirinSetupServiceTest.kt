package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.apk.ApkPackages
import dev.jagoba.lostielauncher.service.apk.ApkReleaseInstallerFactory
import dev.jagoba.lostielauncher.service.apk.SignedApk
import dev.jagoba.lostielauncher.service.apk.succeedsWriting
import dev.jagoba.lostielauncher.service.cdn.RemoteJson
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import dev.jagoba.lostielauncher.service.download.DownloadTransferRequest
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
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
    private val packages = mockk<ApkPackages>(relaxed = true) {
        every { canRequestPackageInstalls() } returns true
    }
    private val logger = mockk<Logger>(relaxed = true)
    private val minimumVersionStore = InMemoryMinimumVersionStore()
    private val clock = AdjustableClock(Instant.parse("2026-11-25T00:00:00Z"))
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

    private fun createSut(
        supportedAbis: List<String> = listOf("arm64-v8a"),
        feedRetryInterval: Duration = 5.minutes,
    ): DefaultKirinSetupService {
        val dispatchers = TestDispatcherProvider(dispatcher)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                chain.proceed(request.newBuilder().url(server.url(request.url.encodedPath)).build())
            }
            .build()
        val remoteJson = RemoteJson(client, Json { ignoreUnknownKeys = true }, dispatchers, logger)
        return DefaultKirinSetupService(
            remoteJson,
            ApkReleaseInstallerFactory(remoteJson, transfer, packages, dispatchers, logger),
            minimumVersionStore,
            KirinOptions(File(""), File(""), "", "", KIRIN_PACKAGE, "", "", ""),
            KirinSetupOptions(
                REQUIREMENTS_URL,
                FALLBACK_MINIMUM,
                installer,
                "official",
                supportedAbis,
                feedRetryInterval,
            ),
            clock,
            logger,
        )
    }

    @Test
    fun `blocks a missing Kirin with the server's minimum version`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements(minimum = "0.4.1")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Missing("0.4.1")
        minimumVersionStore.stored shouldBe "0.4.1"
    }

    @Test
    fun `rejects an untrusted installed Kirin and blocks downloading an available update`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.1", setOf("different"))
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.UntrustedSigner
        sut.isOfficialKirinInstalled() shouldBe false
        sut.download.value shouldBe ApkDownloadState.Available
        sut.install()
        coVerify(exactly = 0) { transfer.download(any(), any()) }

        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        sut.isOfficialKirinInstalled() shouldBe true
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        sut.isOfficialKirinInstalled() shouldBe false
    }

    @Test
    fun `blocks an installation below the server's minimum version`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        serveRequirements(minimum = "0.4.1")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Outdated("0.4.0", "0.4.1")
    }

    @Test
    fun `blocks a pre-release installation below the stable minimum and offers the stable release`() =
        runTest(dispatcher) {
            every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0-beta8", setOf("official"))
            serveRequirements(minimum = "0.4.0")
            serveRelease("0.4.0")
            val sut = createSut()

            sut.refresh()

            sut.status.value shouldBe KirinStatus.Outdated("0.4.0-beta8", "0.4.0")
            sut.download.value shouldBe ApkDownloadState.Available
        }

    @Test
    fun `recommends the feed's release when it is newer than the installed Kirin`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.UpdateRecommended("0.4.0", "0.4.2")
        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `deletes a leftover installer once Kirin is at the feed's release`() = runTest(dispatcher) {
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))
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
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.3.5", setOf("official"))
        val sut = createSut()

        sut.refresh()
        sut.status.value shouldBe KirinStatus.Outdated("0.3.5", FALLBACK_MINIMUM)
        sut.download.value shouldBe ApkDownloadState.Unavailable

        serveRequirements(minimum = "0.3.0")
        sut.refresh()
        sut.status.value shouldBe KirinStatus.Ready
    }

    @Test
    fun `uses the higher of the persisted last-known minimum and the built-in minimum when requirements fail`() =
        runTest(dispatcher) {
            every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
            minimumVersionStore.stored = "0.5.0"
            val sut = createSut()

            sut.refresh()

            sut.status.value shouldBe KirinStatus.Outdated("0.4.0", "0.5.0")
        }

    @Test
    fun `prefers the built-in minimum over an older persisted minimum when requirements fail`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.3.5", setOf("official"))
        minimumVersionStore.stored = "0.3.0"
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Outdated("0.3.5", FALLBACK_MINIMUM)
    }

    @Test
    fun `does not persist an unparseable minimum version from the server`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        minimumVersionStore.stored = "0.4.1"
        serveRequirements(minimum = "not-a-version")
        val sut = createSut()

        sut.refresh()

        minimumVersionStore.stored shouldBe "0.4.1"
    }

    @Test
    fun `offers the feed's release to a missing Kirin`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `skips the GitHub release feed and keeps checking state until the release feed is allowed`() =
        runTest(dispatcher) {
            every { packages.installedPackage(KIRIN_PACKAGE) } returns null
            serveRequirements(minimum = "0.4.1")
            serveRelease("0.4.2")
            val sut = createSut()

            sut.refresh(allowReleaseFeed = false)

            sut.status.value shouldBe KirinStatus.Missing("0.4.1")
            sut.download.value shouldBe ApkDownloadState.Checking
            server.requestCount shouldBe 1

            sut.refresh(allowReleaseFeed = true)

            sut.download.value shouldBe ApkDownloadState.Available
            server.requestCount shouldBe 2
        }

    @Test
    fun `does not offer a release below the minimum version`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.3.5")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `does not offer a download that is not served over HTTPS`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        serveRequirements()
        serveRelease("0.4.2", "http://github.com/shogun-drei/kirin-updates/releases/download/0.4.2/Kirin.apk")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Ready
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `does not fetch a Kirin release feed that is not served over HTTPS`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements(feedUrl = "http://api.github.com$FEED_PATH")
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Unavailable
        server.requestCount shouldBe 1
        verify(exactly = 1) { logger.error("Kirin release feed URL was rejected.") }
    }

    @Test
    fun `does not fetch a Kirin release feed from a disallowed host`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements(feedUrl = "https://evil.example$FEED_PATH")
        serveRelease("0.4.2")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Unavailable
        server.requestCount shouldBe 1
        verify(exactly = 1) { logger.error("Kirin release feed URL was rejected.") }
    }

    @Test
    fun `does not offer a Kirin APK download from a disallowed host`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        serveRequirements()
        serveRelease("0.4.2", "https://evil.example/Kirin.apk")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Ready
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `accepts a Kirin APK download hosted on a githubusercontent subdomain`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2", "https://release-assets.githubusercontent.com/asset/Kirin-0.4.2.apk")
        val sut = createSut()

        sut.refresh()

        sut.download.value shouldBe ApkDownloadState.Available
    }

    @Test
    fun `does not offer a release without an APK`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2", apkName = "Kirin-0.4.2.zip")
        val sut = createSut()

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Missing("0.4.0")
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `selects the APK matching the device's preferred ABI regardless of asset order`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveReleaseAssets(
            version = "0.4.2",
            assets = listOf(
                "Kirin-0.4.2-armeabi-v7a.apk" to "https://github.com/shogun-drei/Kirin-v7a.apk",
                "Kirin-0.4.2-arm64-v8a.apk" to "https://github.com/shogun-drei/Kirin-arm64.apk",
            ),
        )
        val sut = createSut(supportedAbis = listOf("arm64-v8a", "armeabi-v7a"))
        sut.refresh()
        transfer.succeedsWriting(installer)
        every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

        sut.install()

        coVerify(exactly = 1) {
            transfer.download(
                DownloadTransferRequest(
                    "https://github.com/shogun-drei/Kirin-arm64.apk",
                    installer.path,
                    KirinSetupOptions.DEFAULT_MAX_INSTALLER_BYTES,
                    KirinSetupOptions.DEFAULT_ALLOWED_DOWNLOAD_HOSTS,
                ),
                any(),
            )
        }
    }

    @Test
    fun `falls back to a universal APK when no ABI-specific asset matches the device`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveReleaseAssets(
            version = "0.4.2",
            assets = listOf(
                "Kirin-0.4.2-arm64-v8a.apk" to "https://github.com/shogun-drei/Kirin-arm64.apk",
                "Kirin-0.4.2-universal.apk" to "https://github.com/shogun-drei/Kirin-universal.apk",
            ),
        )
        val sut = createSut(supportedAbis = listOf("x86_64", "x86"))
        sut.refresh()
        transfer.succeedsWriting(installer)
        every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

        sut.install()

        coVerify(exactly = 1) {
            transfer.download(
                DownloadTransferRequest(
                    "https://github.com/shogun-drei/Kirin-universal.apk",
                    installer.path,
                    KirinSetupOptions.DEFAULT_MAX_INSTALLER_BYTES,
                    KirinSetupOptions.DEFAULT_ALLOWED_DOWNLOAD_HOSTS,
                ),
                any(),
            )
        }
    }

    @Test
    fun `reports incompatible ABI and skips download when no APK matches the device`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2", apkName = "Kirin-0.4.2-arm64-v8a.apk")
        val sut = createSut(supportedAbis = listOf("x86_64", "x86"))

        sut.refresh()

        sut.status.value shouldBe KirinStatus.Missing("0.4.0")
        sut.download.value shouldBe ApkDownloadState.IncompatibleAbi
        sut.install()
        coVerify(exactly = 0) { transfer.download(any(), any()) }
    }

    @Test
    fun `reports unavailable rather than incompatible ABI when an incompatible release is below the minimum`() =
        runTest(dispatcher) {
            every { packages.installedPackage(KIRIN_PACKAGE) } returns null
            serveRequirements(minimum = "0.4.0")
            serveRelease("0.3.5", apkName = "Kirin-0.3.5-arm64-v8a.apk")
            val sut = createSut(supportedAbis = listOf("x86_64", "x86"))

            sut.refresh()

            sut.download.value shouldBe ApkDownloadState.Unavailable
        }

    @Test
    fun `ignores prerelease and draft GitHub releases`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2-beta1", prerelease = true)
        val sut = createSut()

        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Unavailable

        clock.advance(5.minutes)
        serveRelease("0.4.2", draft = true)
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Unavailable
    }

    @Test
    fun `spaces out retries to the GitHub feed after a failed fetch`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        responses[FEED_PATH] = MockResponse.Builder().code(403).build()
        val sut = createSut(feedRetryInterval = 5.minutes)

        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Unavailable
        server.requestCount shouldBe 2

        serveRelease("0.4.2")
        clock.advance(4.minutes)
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Unavailable
        server.requestCount shouldBe 2

        clock.advance(1.minutes)
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Available
        server.requestCount shouldBe 3
    }

    @Test
    fun `keeps incompatible ABI state during cooldown without repeating GitHub requests`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
        serveRequirements()
        serveRelease("0.4.2", apkName = "Kirin-0.4.2-arm64-v8a.apk")
        val sut = createSut(supportedAbis = listOf("x86_64"), feedRetryInterval = 5.minutes)

        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.IncompatibleAbi
        server.requestCount shouldBe 2

        clock.advance(2.minutes)
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.IncompatibleAbi
        server.requestCount shouldBe 2

        serveRelease("0.4.2", apkName = "Kirin-0.4.2-x86_64.apk")
        clock.advance(3.minutes)
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Available
        server.requestCount shouldBe 3
    }

    @Test
    fun `asks nothing more of the network once the requirements and the feed are known`() = runTest(dispatcher) {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns null
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
        transfer.succeedsWriting(installer) { observed += sut.download.value }
        every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

        sut.install()

        observed shouldBe listOf(ApkDownloadState.Downloading(50))
        sut.download.value shouldBe ApkDownloadState.Installing
        coVerify(exactly = 1) {
            transfer.download(
                DownloadTransferRequest(
                    APK_URL,
                    installer.path,
                    KirinSetupOptions.DEFAULT_MAX_INSTALLER_BYTES,
                    KirinSetupOptions.DEFAULT_ALLOWED_DOWNLOAD_HOSTS,
                ),
                any(),
            )
        }
        verify(exactly = 1) { packages.install(installer) }
    }

    @Test
    fun `requests unknown-sources permission before downloading Kirin and continues once granted`() =
        runTest(dispatcher) {
            val sut = offeringRelease()
            every { packages.canRequestPackageInstalls() } returns false
            transfer.succeedsWriting(installer)
            every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

            sut.install()
            sut.download.value shouldBe ApkDownloadState.PermissionRequired
            coVerify(exactly = 0) { transfer.download(any(), any()) }

            sut.dismissPermissionRequest()
            sut.download.value shouldBe ApkDownloadState.Available

            sut.install()
            sut.download.value shouldBe ApkDownloadState.PermissionRequired
            every { packages.canRequestPackageInstalls() } returns true
            sut.onInstallPermissionResult()

            sut.download.value shouldBe ApkDownloadState.Installing
            coVerify(exactly = 1) { transfer.download(any(), any()) }
            verify(exactly = 1) { packages.install(installer) }
        }

    @Test
    fun `models cancelled Kirin installation, retries from cache and clears the offer once installed`() =
        runTest(dispatcher) {
            val sut = offeringRelease()
            transfer.succeedsWriting(installer)
            every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

            sut.install()
            sut.download.value shouldBe ApkDownloadState.Installing

            sut.refresh()
            sut.status.value shouldBe KirinStatus.UpdateRecommended("0.4.0", "0.4.2")
            sut.download.value shouldBe ApkDownloadState.Cancelled

            sut.install()
            sut.download.value shouldBe ApkDownloadState.Installing
            coVerify(exactly = 1) { transfer.download(any(), any()) }
            verify(exactly = 2) { packages.install(installer) }

            every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))
            sut.refresh()

            sut.status.value shouldBe KirinStatus.Ready
            sut.download.value shouldBe ApkDownloadState.Unavailable
            installer.parentFile?.exists() shouldBe false
        }

    @Test
    fun `dismissing a cancelled Kirin installation returns to available`() = runTest(dispatcher) {
        val sut = offeringRelease()
        transfer.succeedsWriting(installer)
        every { packages.archivePackage(installer, KIRIN_PACKAGE) } returns SignedApk("0.4.2", setOf("official"))

        sut.install()
        sut.refresh()
        sut.download.value shouldBe ApkDownloadState.Cancelled

        sut.dismissInstallCancelled()
        sut.download.value shouldBe ApkDownloadState.Available
    }

    private suspend fun offeringRelease(): DefaultKirinSetupService {
        every { packages.installedPackage(KIRIN_PACKAGE) } returns SignedApk("0.4.0", setOf("official"))
        serveRequirements()
        serveRelease("0.4.2")
        return createSut().also { it.refresh() }
    }

    private fun serveRequirements(minimum: String = "0.4.0", feedUrl: String = FEED_URL) {
        serve(REQUIREMENTS_PATH, """{"minimumVersion":"$minimum","releaseFeedUrl":"$feedUrl"}""")
    }

    private fun serveRelease(
        version: String,
        url: String = APK_URL,
        apkName: String = "Kirin-$version-arm64-v8a.apk",
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) {
        serveReleaseAssets(version, listOf(apkName to url), draft, prerelease)
    }

    private fun serveReleaseAssets(
        version: String,
        assets: List<Pair<String, String>>,
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) {
        val serializedAssets = (listOf("checksums.txt" to "https://example.com/checksums.txt") + assets)
            .joinToString(",") { (name, url) -> """{"name":"$name","browser_download_url":"$url"}""" }
        serve(
            FEED_PATH,
            """{"tag_name":"$version","draft":$draft,"prerelease":$prerelease,"assets":[$serializedAssets]}""",
        )
    }

    private fun serve(path: String, body: String) {
        responses[path] = MockResponse.Builder().body(body).build()
    }

    private class InMemoryMinimumVersionStore : KirinMinimumVersionStore {
        var stored: String? = null

        override suspend fun read(): String? = stored

        override suspend fun write(version: String) {
            stored = version
        }
    }

    private class AdjustableClock(private var current: Instant) : Clock() {
        fun advance(duration: Duration) {
            current = current.plus(duration.toJavaDuration())
        }

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = current
    }

    private companion object {
        const val REQUIREMENTS_PATH = "/kirin-setup-options.json"
        const val REQUIREMENTS_URL = "https://ericlostie-launcher.jagoba.dev$REQUIREMENTS_PATH"
        const val FEED_PATH = "/releases/latest"
        const val FEED_URL = "https://api.github.com$FEED_PATH"
        const val FALLBACK_MINIMUM = "0.4.0"
        const val KIRIN_PACKAGE = "com.gmax.kirin"
        const val APK_URL = "https://github.com/shogun-drei/kirin-updates/releases/download/0.4.2/Kirin-0.4.2.apk"
    }
}
