package dev.jagoba.lostielauncher.ui.component

import dev.jagoba.lostielauncher.model.LibraryCardStatus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GameCardTakeoverTest {
    @Test
    fun `starting a download or an update takes the card over`() {
        libraryCardTakeover(LibraryCardStatus.AVAILABLE, LibraryCardStatus.DOWNLOADING) shouldBe
            CardTakeover.INSTALL_STARTED
        libraryCardTakeover(LibraryCardStatus.UPDATE_AVAILABLE, LibraryCardStatus.DOWNLOADING) shouldBe
            CardTakeover.INSTALL_STARTED
    }

    @Test
    fun `resuming a paused download is not a start`() {
        libraryCardTakeover(LibraryCardStatus.PAUSED, LibraryCardStatus.DOWNLOADING).shouldBeNull()
    }

    @Test
    fun `an installation that ends installed takes the card over`() {
        libraryCardTakeover(LibraryCardStatus.EXTRACTING, LibraryCardStatus.DOWNLOADED) shouldBe
            CardTakeover.INSTALL_FINISHED
    }

    @Test
    fun `a card first shown in a state, a cancel or a failure is not taken over`() {
        LibraryCardStatus.entries.forEach { libraryCardTakeover(it, it).shouldBeNull() }
        libraryCardTakeover(LibraryCardStatus.DOWNLOADING, LibraryCardStatus.AVAILABLE).shouldBeNull()
        libraryCardTakeover(LibraryCardStatus.EXTRACTING, LibraryCardStatus.INSTALLATION_FAILED).shouldBeNull()
    }
}
