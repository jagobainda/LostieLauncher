package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.util.text.RichText
import kotlinx.coroutines.launch

private data class LinkRange(val start: Int, val end: Int, val text: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    highlight: String = "",
    detectLinks: Boolean = true,
) {
    val colors = LocalLauncherColors.current
    val runs = remember(text, highlight, detectLinks) { RichText.runs(text, highlight, detectLinks) }
    val links = remember(runs) { linkRanges(runs) }
    var pressedUrl by remember { mutableStateOf<String?>(null) }
    var tooltipUrl by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val openLink by rememberUpdatedState(onOpenLink)
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    val annotated = buildAnnotatedString {
        runs.forEach { run ->
            val style = when {
                run.url != null -> SpanStyle(
                    color = if (run.url == pressedUrl) colors.primaryFgHover else colors.primaryFg,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = if (run.highlighted) LauncherType.Bold else null,
                )

                run.highlighted -> SpanStyle(
                    color = colors.primaryFg,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = LauncherType.Bold,
                )

                else -> null
            }
            if (style == null) append(run.text) else withStyle(style) { append(run.text) }
        }
    }

    fun urlAt(position: Offset): String? {
        val result = layout ?: return null
        val line = result.getLineForVerticalPosition(position.y)
        if (position.x < result.getLineLeft(line) || position.x > result.getLineRight(line)) return null
        val offset = result.getOffsetForPosition(position)
        return links.firstOrNull { offset >= it.start && offset < it.end }?.url
    }

    val interaction = if (links.isEmpty()) {
        Modifier
    } else {
        Modifier
            .pointerInput(links) {
                detectTapGestures(
                    onPress = { position ->
                        pressedUrl = urlAt(position) ?: return@detectTapGestures
                        tryAwaitRelease()
                        pressedUrl = null
                    },
                    onTap = { position -> urlAt(position)?.let(openLink) },
                    onLongPress = { position ->
                        urlAt(position)?.let {
                            tooltipUrl = it
                            scope.launch { tooltipState.show() }
                        }
                    },
                )
            }
            .semantics {
                customActions = links.map { link ->
                    CustomAccessibilityAction(link.text) {
                        openLink(link.url)
                        true
                    }
                }
            }
    }

    Box(modifier) {
        LauncherTooltipBox(text = tooltipUrl, state = tooltipState, enableUserInput = false) {
            Text(
                text = annotated,
                color = color,
                fontSize = fontSize,
                fontWeight = fontWeight,
                onTextLayout = { layout = it },
                modifier = interaction,
            )
        }
    }
}

private fun linkRanges(runs: List<RichText.Run>): List<LinkRange> {
    val ranges = mutableListOf<LinkRange>()
    var offset = 0
    runs.forEach { run ->
        val url = run.url
        if (url != null) ranges += LinkRange(offset, offset + run.text.length, run.text, url)
        offset += run.text.length
    }
    return ranges
}
