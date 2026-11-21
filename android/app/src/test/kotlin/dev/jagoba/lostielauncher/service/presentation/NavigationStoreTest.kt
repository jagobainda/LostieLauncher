package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.LauncherSection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NavigationStoreTest {
    @Test
    fun `library target and action are consumed independently`() {
        val store = NavigationStore()
        store.navigate(LauncherSection.LIBRARY, "target", LibraryNavigationAction.UPDATE)
        store.consumeLibraryGame("other")
        store.state.value.pendingLibraryGameId shouldBe "target"
        store.consumeLibraryAction("target")
        store.state.value.pendingLibraryGameId shouldBe "target"
        store.state.value.pendingLibraryAction shouldBe null
        store.consumeLibraryGame("target")
        store.state.value.pendingLibraryGameId shouldBe null
        store.state.value.pendingLibraryAction shouldBe null
    }

    @Test
    fun `consuming the scroll target preserves the pending library action`() {
        val store = NavigationStore()
        store.navigate(LauncherSection.LIBRARY, "target", LibraryNavigationAction.SPECIAL_VERSION)
        store.consumeLibraryGame("target")
        store.state.value.pendingLibraryGameId shouldBe null
        store.state.value.pendingLibraryAction shouldBe
            PendingLibraryAction("target", LibraryNavigationAction.SPECIAL_VERSION)
        store.consumeLibraryAction("target") shouldBe true
        store.consumeLibraryAction("target") shouldBe false
        store.state.value.pendingLibraryAction shouldBe null
    }
}
