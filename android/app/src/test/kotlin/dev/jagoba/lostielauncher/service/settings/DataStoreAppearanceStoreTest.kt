package dev.jagoba.lostielauncher.service.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DataStoreAppearanceStore")
class DataStoreAppearanceStoreTest {
    private val logger = mockk<Logger>(relaxed = true)

    private fun createSut(dataStore: DataStore<Preferences>) = DataStoreAppearanceStore(dataStore, logger)

    // ---- reading ----

    @Test
    fun `reads the defaults when nothing has been stored`() = runTest {
        // Arrange — a fresh install. spec/04-data-model.md: Volcarona and Esp.
        val sut = createSut(FakeDataStore())

        // Act / Assert
        sut.appearance.test {
            awaitItem() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
        }
    }

    @Test
    fun `reads back what was stored`() = runTest {
        // Arrange
        val store = FakeDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("appearance.theme") to "Mewtwo",
                stringPreferencesKey("appearance.language") to "EUS",
            ),
        )
        val sut = createSut(store)

        // Act / Assert
        sut.appearance.test {
            awaitItem() shouldBe Appearance(AppTheme.Mewtwo, AppLanguage.EUS)
        }
    }

    @Test
    fun `falls back to the defaults when a stored value names nothing`() = runTest {
        // Arrange — a settings file written by a future version that had a
        // theme this one does not, or an older one that wrote an ordinal. The
        // desktop logs and falls back rather than refusing to start, and so
        // does this.
        val store = FakeDataStore(
            mutablePreferencesOf(
                stringPreferencesKey("appearance.theme") to "Pikachu",
                stringPreferencesKey("appearance.language") to "3",
            ),
        )
        val sut = createSut(store)

        // Act / Assert
        sut.appearance.test {
            awaitItem() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
        }
    }

    @Test
    fun `logs and degrades to the defaults when the settings file cannot be read`() = runTest {
        // Arrange — a corrupt or unreadable preferences file must not be able
        // to stop the launcher starting, the same rule the content layer
        // follows when the CDN is down.
        val sut = createSut(FailingDataStore(IOException("disk gone")))

        // Act / Assert
        sut.appearance.test {
            awaitItem() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
            awaitComplete()
        }
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    @Test
    fun `rethrows anything that is not an IO failure`() = runTest {
        // Arrange — an IllegalStateException out of DataStore is a programming
        // error, not a storage one. Swallowing it would turn a bug into a
        // launcher that silently forgets the user's theme.
        val sut = createSut(FailingDataStore(IllegalStateException("bug")))

        // Act / Assert
        sut.appearance.test {
            awaitError()::class shouldBe IllegalStateException::class
        }
    }

    // ---- writing ----

    @Test
    fun `a selected theme comes back out of the flow`() = runTest {
        // Arrange — the flow is the only source of truth: a write lands in
        // storage and reaches the UI on the way back out, so there is no
        // in-memory copy to disagree with the file.
        val store = FakeDataStore()
        val sut = createSut(store)

        // Act / Assert
        sut.appearance.test {
            awaitItem() shouldBe Appearance(AppTheme.Volcarona, AppLanguage.ESP)
            sut.setTheme(AppTheme.Astrem)
            awaitItem() shouldBe Appearance(AppTheme.Astrem, AppLanguage.ESP)
            sut.setLanguage(AppLanguage.FRA)
            awaitItem() shouldBe Appearance(AppTheme.Astrem, AppLanguage.FRA)
        }
    }

    @Test
    fun `logs and carries on when a selection cannot be written`() = runTest {
        // Arrange — losing the write means the choice will not survive a
        // restart. That is not worth a dialog, and it is certainly not worth
        // propagating out of a click handler.
        val sut = createSut(FailingDataStore(IOException("read-only")))

        // Act
        sut.setTheme(AppTheme.Sylveon)

        // Assert
        verify(exactly = 1) { logger.error(any(), any()) }
    }

    /**
     * Preferences DataStore without a file.
     *
     * The real one is an Android artifact writing to a real path; what is under
     * test here is the mapping and the failure handling around it, and neither
     * needs a filesystem. `updateData` applies the transform and re-emits, which
     * is the whole contract this store uses.
     */
    private class FakeDataStore(initial: Preferences = mutablePreferencesOf()) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    /** A DataStore that is broken in both directions, with the given cause. */
    private class FailingDataStore(private val cause: Throwable) : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw cause }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw cause
    }
}
