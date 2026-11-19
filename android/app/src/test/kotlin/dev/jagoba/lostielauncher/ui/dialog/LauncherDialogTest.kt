package dev.jagoba.lostielauncher.ui.dialog

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Dialog layout with a soft keyboard")
class LauncherDialogTest {
    @ParameterizedTest
    @CsvSource("122, true, true", "122, false, false", "340, true, false")
    fun `the title scrolls only when the keyboard leaves too little room to edit`(
        height: Int,
        keyboardVisible: Boolean,
        scrollTitle: Boolean,
    ) {
        useScrollingDialogTitle(height.dp, keyboardVisible) shouldBe scrollTitle
    }
}
