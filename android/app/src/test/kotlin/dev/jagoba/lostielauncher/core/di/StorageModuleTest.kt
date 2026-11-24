package dev.jagoba.lostielauncher.core.di

import android.content.Context
import android.os.Environment
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("StorageModule")
class StorageModuleTest {
    private val noBackup = File("/data/user/0/app/no_backup")
    private val files = File("/data/user/0/app/files")
    private val external = File("/storage/emulated/0/Android/data/app/files")

    @Test
    fun `places downloads and logs in internal noBackupFilesDir even when external storage is mounted`() {
        val context = mockk<Context> {
            every { noBackupFilesDir } returns noBackup
            every { filesDir } returns files
            every { getExternalFilesDir(null) } returns external
        }
        mockkStatic(Environment::class) {
            every { Environment.getExternalStorageState(external) } returns Environment.MEDIA_MOUNTED

            val locations = StorageModule.provideStorageLocations(context)

            locations.downloadsRoot shouldBe File(noBackup, "games")
            locations.logsDirectory shouldBe File(noBackup, "logs")
            locations.legacyDownloadsRoots shouldBe listOf(File(external, "games"), File(files, "games"))
        }
    }

    @Test
    fun `omits the external legacy root when external storage is unavailable`() {
        val context = mockk<Context> {
            every { noBackupFilesDir } returns noBackup
            every { filesDir } returns files
            every { getExternalFilesDir(null) } returns external
        }
        mockkStatic(Environment::class) {
            every { Environment.getExternalStorageState(external) } returns Environment.MEDIA_REMOVED

            val locations = StorageModule.provideStorageLocations(context)

            locations.downloadsRoot shouldBe File(noBackup, "games")
            locations.legacyDownloadsRoots shouldBe listOf(File(files, "games"))
        }
    }
}
