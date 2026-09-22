package dev.jagoba.lostielauncher.service.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import dev.jagoba.lostielauncher.core.coroutines.TestDispatcherProvider
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.SettingsOptions
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DataStoreSettingsStore")
@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsStoreTest {
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `reads defaults when nothing has been stored`() = runTest {
        val sut = createSut(FakeDataStore(), StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings()
        }
    }

    @Test
    fun `reads every stored setting`() = runTest {
        val store = FakeDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("appearance.theme") to "Mewtwo",
                stringPreferencesKey("appearance.language") to "EUS",
                booleanPreferencesKey("onboarding.hasSeenWelcome") to true,
            ),
        )
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings(AppTheme.Mewtwo, AppLanguage.EUS, true)
        }
    }

    @Test
    fun `falls back when stored enum names are unknown`() = runTest {
        val store = FakeDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("appearance.theme") to "Pikachu",
                stringPreferencesKey("appearance.language") to "3",
            ),
        )
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings()
        }
    }

    @Test
    fun `logs and uses defaults when settings cannot be read`() = runTest {
        val sut = createSut(FailingDataStore(IOException("disk gone")), StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `logs and uses defaults for a non IO read failure`() = runTest {
        val sut = createSut(FailingDataStore(IllegalStateException("bug")), StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings()
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `publishes a change before its debounced disk write`() = runTest {
        val store = FakeDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.settings.test {
            awaitItem() shouldBe AppSettings()
            sut.setTheme(AppTheme.Astrem)
            awaitItem() shouldBe AppSettings(theme = AppTheme.Astrem)
            store.updateCount shouldBe 0
        }
    }

    @Test
    fun `waits five hundred milliseconds before writing`() = runTest {
        val store = FakeDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.setLanguage(AppLanguage.FRA)
        advanceTimeBy(499)
        runCurrent()
        store.updateCount shouldBe 0
        advanceTimeBy(1)
        runCurrent()

        store.updateCount shouldBe 1
    }

    @Test
    fun `coalesces rapid changes into one last-write-wins update`() = runTest {
        val store = FakeDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.setLanguage(AppLanguage.ENG)
        sut.setLanguage(AppLanguage.CAT)
        sut.setLanguage(AppLanguage.FRA)
        advanceUntilIdle()

        store.updateCount shouldBe 1
        store.current()[stringPreferencesKey("appearance.language")] shouldBe "FRA"
    }

    @Test
    fun `preserves concurrent changes to different settings`() = runTest {
        val store = FakeDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        listOf(
            async { sut.setTheme(AppTheme.Empoleon) },
            async { sut.setLanguage(AppLanguage.POR) },
            async { sut.setHasSeenWelcome(true) },
        ).awaitAll()
        sut.flush()

        store.updateCount shouldBe 1
        sut.settings.test {
            awaitItem() shouldBe AppSettings(AppTheme.Empoleon, AppLanguage.POR, true)
        }
    }

    @Test
    fun `flush persists pending settings immediately`() = runTest {
        val store = FakeDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.setTheme(AppTheme.Torterra)
        sut.flush()

        store.updateCount shouldBe 1
        store.current()[stringPreferencesKey("appearance.theme")] shouldBe "Torterra"
    }

    @Test
    fun `flush preserves pending settings when the deferred write has already started`() = runTest {
        val store = PausingFirstWriteDataStore()
        val sut = createSut(store, StandardTestDispatcher(testScheduler))

        sut.setTheme(AppTheme.Torterra)
        advanceTimeBy(500)
        runCurrent()
        store.firstWriteStarted.await()
        sut.flush()

        store.successfulWrites shouldBe 1
        store.current()[stringPreferencesKey("appearance.theme")] shouldBe "Torterra"
    }

    @Test
    fun `logs and restores stored state after a write failure`() = runTest {
        val sut = createSut(FailingDataStore(IOException("read-only")), StandardTestDispatcher(testScheduler))

        sut.setTheme(AppTheme.Sylveon)
        sut.flush()

        verify(atLeast = 1) { logger.error(any(), any()) }
    }

    private fun createSut(dataStore: DataStore<Preferences>, dispatcher: CoroutineDispatcher) = DataStoreSettingsStore(
        dataStore = dataStore,
        logger = logger,
        dispatchers = TestDispatcherProvider(dispatcher),
        options = SettingsOptions(
            saveDebounce = 500.milliseconds,
            startupLoadTimeout = 2_000.milliseconds,
        ),
    )

    private class FakeDataStore(initial: Preferences = mutablePreferencesOf()) : DataStore<Preferences> {
        private val gate = Mutex()
        private val state = MutableStateFlow(initial)
        var updateCount = 0
            private set

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = gate.withLock {
            transform(state.value).also {
                updateCount++
                state.value = it
            }
        }

        fun current(): Preferences = state.value
    }

    private class FailingDataStore(private val cause: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw cause }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw cause
    }

    private class PausingFirstWriteDataStore : DataStore<Preferences> {
        private val gate = Mutex()
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        private var attempts = 0
        val firstWriteStarted = CompletableDeferred<Unit>()
        var successfulWrites = 0
            private set

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = gate.withLock {
            attempts++
            if (attempts == 1) {
                firstWriteStarted.complete(Unit)
                awaitCancellation()
            }
            transform(state.value).also {
                successfulWrites++
                state.value = it
            }
        }

        fun current(): Preferences = state.value
    }
}
