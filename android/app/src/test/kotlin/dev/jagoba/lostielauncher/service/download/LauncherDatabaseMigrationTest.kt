package dev.jagoba.lostielauncher.service.download

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LauncherDatabase migration 1 to 2")
class LauncherDatabaseMigrationTest {
    private val database = mockk<SupportSQLiteDatabase>(relaxed = true)

    @Test
    fun `adds nullable enqueue metadata without replacing existing rows`() {
        MIGRATION_1_2.migrate(database)

        verifyOrder {
            database.execSQL("ALTER TABLE downloads ADD COLUMN expected_sha256 TEXT")
            database.execSQL("ALTER TABLE downloads ADD COLUMN variant TEXT")
        }
    }
}
