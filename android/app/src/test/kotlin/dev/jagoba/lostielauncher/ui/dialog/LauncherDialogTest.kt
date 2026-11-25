package dev.jagoba.lostielauncher.ui.dialog

import android.text.InputType
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("Dialog layout, key input and overlay touch filtering")
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

    @Test
    fun `key field disables suggestions and personalized learning in both masked and visible modes`() {
        val masked = keyFieldInputType(visible = false)
        val visible = keyFieldInputType(visible = true)

        (masked and InputType.TYPE_MASK_CLASS) shouldBe InputType.TYPE_CLASS_TEXT
        (masked and InputType.TYPE_MASK_VARIATION) shouldBe InputType.TYPE_TEXT_VARIATION_PASSWORD
        (masked and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) shouldBe InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        (visible and InputType.TYPE_MASK_CLASS) shouldBe InputType.TYPE_CLASS_TEXT
        (visible and InputType.TYPE_MASK_VARIATION) shouldBe InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        (visible and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) shouldBe InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        (KEY_FIELD_IME_OPTIONS and EditorInfo.IME_MASK_ACTION) shouldBe EditorInfo.IME_ACTION_DONE
        (KEY_FIELD_IME_OPTIONS and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) shouldBe
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
    }

    @Test
    fun `obscured touch detection respects the active flag mask`() {
        val fullOnly = MotionEvent.FLAG_WINDOW_IS_OBSCURED
        val fullAndPartial = MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED

        isTouchObscured(0, fullAndPartial) shouldBe false
        isTouchObscured(MotionEvent.FLAG_WINDOW_IS_OBSCURED, fullOnly) shouldBe true
        isTouchObscured(MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED, fullOnly) shouldBe false
        isTouchObscured(MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED, fullAndPartial) shouldBe true
    }

    @Test
    fun `touch filter allows clean gestures and drops gestures started while obscured`() {
        val filter = ObscuredTouchFilter()

        filter.evaluate(MotionEvent.ACTION_DOWN, obscured = false) shouldBe TouchDecision.ALLOW
        filter.evaluate(MotionEvent.ACTION_MOVE, obscured = false) shouldBe TouchDecision.ALLOW
        filter.evaluate(MotionEvent.ACTION_UP, obscured = false) shouldBe TouchDecision.ALLOW

        filter.evaluate(MotionEvent.ACTION_DOWN, obscured = true) shouldBe TouchDecision.DROP
        filter.evaluate(MotionEvent.ACTION_MOVE, obscured = false) shouldBe TouchDecision.DROP
        filter.evaluate(MotionEvent.ACTION_UP, obscured = false) shouldBe TouchDecision.DROP

        filter.evaluate(MotionEvent.ACTION_DOWN, obscured = false) shouldBe TouchDecision.ALLOW
        filter.evaluate(MotionEvent.ACTION_UP, obscured = false) shouldBe TouchDecision.ALLOW
    }

    @Test
    fun `touch filter cancels an active gesture when an overlay appears mid-gesture or on release`() {
        val filter = ObscuredTouchFilter()

        filter.evaluate(MotionEvent.ACTION_DOWN, obscured = false) shouldBe TouchDecision.ALLOW
        filter.evaluate(MotionEvent.ACTION_MOVE, obscured = true) shouldBe TouchDecision.CANCEL_AND_DROP
        filter.evaluate(MotionEvent.ACTION_MOVE, obscured = false) shouldBe TouchDecision.DROP
        filter.evaluate(MotionEvent.ACTION_UP, obscured = false) shouldBe TouchDecision.DROP

        filter.evaluate(MotionEvent.ACTION_DOWN, obscured = false) shouldBe TouchDecision.ALLOW
        filter.evaluate(MotionEvent.ACTION_UP, obscured = true) shouldBe TouchDecision.CANCEL_AND_DROP

        filter.evaluate(MotionEvent.ACTION_HOVER_MOVE, obscured = true) shouldBe TouchDecision.DROP
        filter.evaluate(MotionEvent.ACTION_HOVER_MOVE, obscured = false) shouldBe TouchDecision.ALLOW
    }
}
