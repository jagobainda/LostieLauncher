package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.LauncherSection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NavigationStoreTest {
    @Test
    fun `library target survives until matching card consumes it`() {
        val store = NavigationStore()
        store.navigate(LauncherSection.LIBRARY, "target", LibraryNavigationAction.UPDATE)
        store.consumeLibraryGame("other")
        store.state.value.pendingLibraryGameId shouldBe "target"
        store.consumeLibraryAction("target")
        store.state.value.pendingLibraryGameId shouldBe "target"
        store.state.value.libraryAction shouldBe null
        store.consumeLibraryGame("target")
        store.state.value.pendingLibraryGameId shouldBe null
        store.state.value.libraryAction shouldBe null
    }
}
