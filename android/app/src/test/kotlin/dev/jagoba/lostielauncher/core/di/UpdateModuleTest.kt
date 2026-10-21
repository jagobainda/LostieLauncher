package dev.jagoba.lostielauncher.core.di

import android.content.Context
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UpdateModule")
class UpdateModuleTest {
    @Test
    fun `keeps the launcher's installer in its own folder under the shared APK cache`() {
        val context = mockk<Context> {
            every { packageName } returns "dev.jagoba.lostielauncher"
            every { cacheDir } returns File("/cache")
        }

        val options = UpdateModule.provideLauncherUpdateOptions(context)

        options.packageName shouldBe "dev.jagoba.lostielauncher"
        options.feedUrl shouldBe "https://ericlostie-launcher.jagoba.dev/public/android/latest.json"
        options.installerFile shouldBe File("/cache", "apk/launcher/LostieLauncher.apk")
    }
}
