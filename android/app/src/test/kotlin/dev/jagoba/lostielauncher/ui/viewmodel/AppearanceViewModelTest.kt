package dev.jagoba.lostielauncher.ui.viewmodel

import app.cash.turbine.test
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.service.settings.AppearanceStore
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AppearanceViewModel")
class AppearanceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeAppearanceStore()

    // `viewModelScope` is hard-wired to Dispatchers.Main; this is the one place
    // the codebase is allowed to name a dispatcher outside DispatcherProvider,
    // because the ViewModel does not choose it.
    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun createSut() = AppearanceViewModel(store)

    @Test
    fun `reports nothing until the stored settings have been read`() = runTest(dispatcher) {
        // Arrange — the first frame after a cold start. Rendering the defaults
        // while the real values are still being read would flash Volcarona at
        // everyone who chose something else.
        val sut = createSut()

        // Act / Assert
        sut.state.value shouldBe null
    }

    @Test
    fun `exposes the stored theme and the catalogue for the stored language`() = runTest(dispatcher) {
        // Arrange
        store.emit(Appearance(AppTheme.Empoleon, AppLanguage.GAL))
        val sut = createSut()

        // Act / Assert
        sut.state.test {
            awaitItem() shouldBe null
            val state = awaitItem()
            state?.theme shouldBe AppTheme.Empoleon
            state?.language shouldBe AppLanguage.GAL
            state?.strings shouldBe stringsFor(AppLanguage.GAL)
        }
    }

    @Test
    fun `a new theme reaches the UI without anything restarting`() = runTest(dispatcher) {
        // Arrange — this is the desktop behaviour the port has to keep: the
        // selection repaints the running UI. Here that is one state emission,
        // which is why no part of the shell reacts to a theme change explicitly.
        val sut = createSut()

        // Act / Assert
        sut.state.test {
            awaitItem() shouldBe null
            awaitItem()?.theme shouldBe AppTheme.Volcarona

            sut.selectTheme(AppTheme.Zoroark)

            awaitItem()?.theme shouldBe AppTheme.Zoroark
        }
    }

    @Test
    fun `a new language swaps the whole catalogue`() = runTest(dispatcher) {
        // Arrange — the language change is a single reference swap, so nothing
        // has to re-read a resource table and nothing may cache a resolved
        // string in a field. spec/05-localization.md: that rule is what makes
        // the hot switch work at all.
        val sut = createSut()

        // Act / Assert
        sut.state.test {
            awaitItem() shouldBe null
            awaitItem()?.strings shouldBe stringsFor(AppLanguage.ESP)

            sut.selectLanguage(AppLanguage.FRA)

            val state = awaitItem()
            state?.language shouldBe AppLanguage.FRA
            state?.strings shouldBe stringsFor(AppLanguage.FRA)
            state?.strings?.titleHome shouldBe "Accueil"
        }
    }

    @Test
    fun `a selection is written through to the store`() = runTest(dispatcher) {
        // Arrange — persistence is the half of the requirement a recomposition
        // cannot satisfy on its own.
        val sut = createSut()

        // Act
        sut.selectTheme(AppTheme.Auretoskos)
        sut.selectLanguage(AppLanguage.CAT)
        testScheduler.advanceUntilIdle()

        // Assert
        store.current.value shouldBe Appearance(AppTheme.Auretoskos, AppLanguage.CAT)
    }

    @Test
    fun `keeps no copy of its own, so a change made elsewhere still arrives`() = runTest(dispatcher) {
        // Arrange — there is one source of truth and it is the store. A
        // ViewModel holding its own copy would be the bug this asserts
        // against: the Settings screen step 14 writes is not the only thing
        // that can change these.
        val sut = createSut()

        // Act / Assert
        sut.state.test {
            awaitItem() shouldBe null
            awaitItem()?.theme shouldBe AppTheme.Volcarona

            store.emit(Appearance(AppTheme.Torterra, AppLanguage.POR))

            val state = awaitItem()
            state?.theme shouldBe AppTheme.Torterra
            state?.language shouldBe AppLanguage.POR
        }
    }

    /** An [AppearanceStore] with the storage replaced by a `StateFlow`. */
    private class FakeAppearanceStore : AppearanceStore {
        val current = MutableStateFlow(Appearance())

        override val appearance: Flow<Appearance> = current

        override suspend fun setTheme(theme: AppTheme) {
            current.value = current.value.copy(theme = theme)
        }

        override suspend fun setLanguage(language: AppLanguage) {
            current.value = current.value.copy(language = language)
        }

        fun emit(appearance: Appearance) {
            current.value = appearance
        }
    }
}
