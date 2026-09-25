package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.AppVersion
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import io.kotest.matchers.shouldBe
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeSettingsStore()

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

    private fun createSut() = SettingsViewModel(store, AppVersion("1.2.3"))

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
}
