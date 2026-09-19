package dev.jagoba.lostielauncher.core.di

import dev.jagoba.lostielauncher.core.coroutines.DefaultDispatcherProvider
import dev.jagoba.lostielauncher.util.log.AndroidLogger
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * The infrastructure smoke test, in the spirit of the desktop's
 * `InfrastructureSmokeTests`: proof that the composition root hands out the
 * production implementations rather than compiling and then failing at runtime.
 *
 * It calls the `@Provides` functions directly instead of standing up the Hilt
 * graph, because standing the graph up needs a device or Robolectric and this
 * side's tests stay headless.
 */
@DisplayName("CoreModule")
class CoreModuleTest {
    @Test
    fun `provides the production dispatcher provider`() {
        // Arrange / Act
        val provided = CoreModule.provideDispatcherProvider()

        // Assert
        provided.shouldBeInstanceOf<DefaultDispatcherProvider>()
    }

    @Test
    fun `provides the Logcat-backed logger`() {
        // Arrange / Act
        val provided = CoreModule.provideLogger()

        // Assert
        provided.shouldBeInstanceOf<AndroidLogger>()
    }
}
