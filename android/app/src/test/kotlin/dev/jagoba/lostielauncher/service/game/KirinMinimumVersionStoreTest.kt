package dev.jagoba.lostielauncher.service.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.nio.file.Path
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@DisplayName("KirinMinimumVersionStore")
class KirinMinimumVersionStoreTest {
    @TempDir
    lateinit var directory: Path

    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `persists and reads back the last-known minimum Kirin version`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { directory.resolve("kirin.preferences_pb").toFile() },
        )
        val sut = DataStoreKirinMinimumVersionStore(dataStore, logger)

        sut.read().shouldBeNull()

        sut.write("0.5.0")

        sut.read() shouldBe "0.5.0"
    }

    @Test
    fun `returns null and logs when reading from DataStore fails`() = runTest {
        val dataStore = mockk<DataStore<Preferences>> {
            every { data } returns flow { throw IOException("disk full") }
        }
        val sut = DataStoreKirinMinimumVersionStore(dataStore, logger)

        sut.read().shouldBeNull()

        verify(exactly = 1) { logger.error(any(), any<IOException>()) }
    }

    @Test
    fun `logs and absorbs an IO failure when writing to DataStore`() = runTest {
        val dataStore = mockk<DataStore<Preferences>> {
            coEvery { updateData(any()) } throws IOException("disk full")
        }
        val sut = DataStoreKirinMinimumVersionStore(dataStore, logger)

        sut.write("0.5.0")

        verify(exactly = 1) { logger.error(any(), any<IOException>()) }
    }
}
