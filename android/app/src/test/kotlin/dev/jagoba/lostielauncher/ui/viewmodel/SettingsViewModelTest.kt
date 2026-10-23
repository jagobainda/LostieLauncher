package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.AppVersion
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.service.update.LauncherUpdateCheckResult
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()
    private val installation = TestInstallation()
    private val downloads = TestDownloads()
    private val launcherUpdate = mockk<LauncherUpdateService>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `state waits for stored settings`() = runTest(dispatcher) {
        val sut = createSut()
        sut.state.value shouldBe null
        runCurrent()
        sut.state.value?.theme shouldBe AppTheme.Volcarona
    }

    @Test
    fun `language change replaces resolved catalogue and persists`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.selectLanguage(AppLanguage.FRA)
        runCurrent()
        sut.state.value?.strings shouldBe stringsFor(AppLanguage.FRA)
        store.current.value.language shouldBe AppLanguage.FRA
    }

    @Test
    fun `theme change is observed through store`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.selectTheme(AppTheme.Zoroark)
        runCurrent()
        sut.state.value?.theme shouldBe AppTheme.Zoroark
    }

    @Test
    fun `external store changes still reach the same state`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        store.current.value = store.current.value.copy(theme = AppTheme.Torterra, language = AppLanguage.POR)
        runCurrent()
        sut.state.value?.theme shouldBe AppTheme.Torterra
        sut.state.value?.strings shouldBe stringsFor(AppLanguage.POR)
    }

    @Test
    fun `welcome is marked seen through store`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.markWelcomeSeen()
        runCurrent()
        sut.state.value?.hasSeenWelcome shouldBe true
    }

    @Test
    fun `stored welcome flag is published on first load`() = runTest(dispatcher) {
        store.current.value = store.current.value.copy(hasSeenWelcome = true)
        val sut = createSut()
        runCurrent()
        sut.state.value?.hasSeenWelcome shouldBe true
    }

    @Test
    fun `published settings include the installed app version`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value?.version shouldBe "v1.2.3"
    }

    @Test
    fun `games auto-update setting is observable and persisted`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value?.autoUpdate shouldBe false
        sut.setAutoUpdate(true)
        runCurrent()
        sut.state.value?.autoUpdate shouldBe true
        store.current.value.autoUpdate shouldBe true
    }

    @Test
    fun `auto update is unsupported until installed games can be known`() = runTest(dispatcher) {
        val sut = createSut()
        runCurrent()
        sut.state.value?.autoUpdateSupported shouldBe false
        sut.setAutoUpdate(true)
        runCurrent()
        sut.state.value?.autoUpdate shouldBe true
        installation.current.value = InstalledGamesState.Available(emptyList())
        runCurrent()
        sut.state.value?.autoUpdateSupported shouldBe true
    }

    @Test
    fun `does not check for updates while a download is in progress`() = runTest(dispatcher) {
        downloads.current.value = listOf(testDownload(testGame(), DownloadStatus.DOWNLOADING))
        val sut = createSut()
        runCurrent()

        sut.checkForUpdates()
        runCurrent()

        sut.state.value?.notice shouldBe SettingsNotice.UPDATE_CHECK_BUSY
        coVerify(exactly = 0) { launcherUpdate.check() }
        verify(exactly = 1) { logger.info("Manual update check initiated.") }
        verify(exactly = 1) { logger.info("Manual update check blocked: a download is in progress.") }

        sut.dismissNotice()
        runCurrent()
        sut.state.value?.notice shouldBe null
    }

    @ParameterizedTest
    @MethodSource("checkResults")
    fun `turns the manual check result into a notice`(result: LauncherUpdateCheckResult, notice: SettingsNotice?) =
        runTest(dispatcher) {
            coEvery { launcherUpdate.check() } returns result
            val sut = createSut()
            runCurrent()

            sut.checkForUpdates()
            runCurrent()

            sut.state.value?.notice shouldBe notice
        }

    @Test
    fun `marks the manual check as running until it returns`() = runTest(dispatcher) {
        val result = CompletableDeferred<LauncherUpdateCheckResult>()
        coEvery { launcherUpdate.check() } coAnswers { result.await() }
        val sut = createSut()
        runCurrent()
        val observed = mutableListOf<Boolean?>()

        sut.checkForUpdates()
        sut.checkForUpdates()
        runCurrent()
        observed += sut.state.value?.isCheckingForUpdates
        sut.checkForUpdates()
        result.complete(LauncherUpdateCheckResult.UpToDate)
        runCurrent()
        observed += sut.state.value?.isCheckingForUpdates

        observed shouldContainExactly listOf(true, false)
        coVerify(exactly = 1) { launcherUpdate.check() }
    }

    private fun createSut() =
        SettingsViewModel(store, AppVersion("1.2.3"), installation, downloads, launcherUpdate, logger)

    private class FakeSettingsStore : SettingsStore {
        val current = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = current
        override val appearance: Flow<Appearance> = current.map { it.appearance }

        override suspend fun setTheme(theme: AppTheme) {
            current.value = current.value.copy(theme = theme)
        }

        override suspend fun setLanguage(language: AppLanguage) {
            current.value = current.value.copy(language = language)
        }

        override suspend fun setHasSeenWelcome(hasSeenWelcome: Boolean) {
            current.value = current.value.copy(hasSeenWelcome = hasSeenWelcome)
        }

        override suspend fun setAutoUpdate(autoUpdate: Boolean) {
            current.value = current.value.copy(autoUpdate = autoUpdate)
        }

        override suspend fun flush() = Unit
    }

    private companion object {
        @JvmStatic
        fun checkResults() = listOf(
            arrayOf(LauncherUpdateCheckResult.UpToDate, SettingsNotice.UP_TO_DATE),
            arrayOf(LauncherUpdateCheckResult.Failed, SettingsNotice.UPDATE_CHECK_FAILED),
            arrayOf(LauncherUpdateCheckResult.Available("1.3.0"), null),
        )
    }
}
