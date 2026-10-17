package dev.jagoba.lostielauncher.core.di

import dev.jagoba.lostielauncher.core.coroutines.DefaultDispatcherProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CoreModule")
class CoreModuleTest {
    @Test
    fun `provides the production dispatcher provider`() {
        CoreModule.provideDispatcherProvider().shouldBeInstanceOf<DefaultDispatcherProvider>()
    }

    @Test
    fun `provides a UTC clock`() {
        CoreModule.provideClock().zone.id shouldBe "Z"
    }
}
