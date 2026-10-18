package dev.jagoba.lostielauncher.core.di

import android.os.Environment
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("KirinModule")
class KirinModuleTest {
    @ParameterizedTest
    @ValueSource(ints = [0, 10, 11])
    fun `uses the current user's shared storage for games, the local registry and the Kirin library`(userId: Int) {
        val storage = File("/storage/emulated/$userId")
        mockkStatic(Environment::class) {
            every { Environment.getExternalStorageDirectory() } returns storage

            val options = KirinModule.provideKirinOptions()

            options.gamesRoot shouldBe File(storage, "LostieLauncher")
            options.libraryFile shouldBe File(storage, "Kirin/game-library.json")
            KirinModule.provideLocalLibraryOptions(options).registryFile shouldBe
                File(storage, "LostieLauncher/local_games.json")
        }
    }
}
