package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.content.kirinFaqFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.KirinDownloadState
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("KirinViewModel")
@OptIn(ExperimentalCoroutinesApi::class)
class KirinViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = TestSettingsStore()
    private val links = TestExternalLinkService()
    private val status = MutableStateFlow<KirinStatus?>(null)
    private val download = MutableStateFlow<KirinDownloadState>(KirinDownloadState.Checking)
    private val kirin = mockk<KirinSetupService>(relaxed = true) {
        every { status } returns this@KirinViewModelTest.status
        every { download } returns this@KirinViewModelTest.download
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun createSut() = KirinViewModel(kirin, settings, links)

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("statuses")
    fun `blocks the launcher only below the minimum version`(status: KirinStatus?, blocking: Boolean) =
        runTest(dispatcher) {
            this@KirinViewModelTest.status.value = status
            val sut = createSut()
            runCurrent()

            sut.state.value.isBlocking shouldBe blocking
        }

    @Test
    fun `follows the shared download state`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()

        download.value = KirinDownloadState.Downloading(42)
        runCurrent()

        sut.state.value.download shouldBe KirinDownloadState.Downloading(42)
    }

    @Test
    fun `installs and acknowledges failures through the service`() = runTest(dispatcher) {
        val sut = createSut()

        sut.install()
        sut.dismissDownloadFailure()
        runCurrent()

        coVerify(exactly = 1) { kirin.install() }
        verify(exactly = 1) { kirin.acknowledgeFailure() }
    }

    @Test
    fun `opens a link from the thanks to Kirin's creator`() = runTest(dispatcher) {
        val sut = createSut()

        sut.openLink("https://playkirin.com/")

        links.openedUrls shouldBe listOf("https://playkirin.com/")
    }

    @Test
    fun `explains why Kirin is needed in the selected language`() = runTest(dispatcher) {
        settings.current.value = settings.current.value.copy(language = AppLanguage.ENG)
        val sut = createSut()
        runCurrent()

        sut.state.value.faq shouldBe kirinFaqFor(AppLanguage.ENG)
    }

    private companion object {
        @JvmStatic
        fun statuses() = listOf(
            arrayOf(null, false),
            arrayOf(KirinStatus.Ready, false),
            arrayOf(KirinStatus.UpdateRecommended("0.4.0", "0.4.2"), false),
            arrayOf(KirinStatus.Missing("0.4.0"), true),
            arrayOf(KirinStatus.Outdated("0.3.5", "0.4.0"), true),
        )
    }
}
