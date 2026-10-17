package dev.jagoba.lostielauncher.core.coroutines

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DefaultDispatcherProvider")
class DispatcherProviderTest {
    private fun createSut() = DefaultDispatcherProvider()

    @Test
    fun `maps each role to the dispatcher it names`() {
        val sut = createSut()

        sut.io shouldBe Dispatchers.IO
        sut.default shouldBe Dispatchers.Default
        sut.main shouldBe Dispatchers.Main
    }
}
