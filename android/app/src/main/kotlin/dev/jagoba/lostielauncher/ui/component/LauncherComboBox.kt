package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.jagoba.lostielauncher.ui.theme.LauncherElevation
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
fun <T> LauncherComboBox(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val shape = RoundedCornerShape(LauncherRadii.Small)
    var expanded by rememberSaveable { mutableStateOf(false) }
    var faceWidth by remember { mutableIntStateOf(0) }
    var faceHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(modifier) {
        Box(
            Modifier
                .widthIn(min = LauncherSizes.ComboBoxMinWidth)
                .height(LauncherSizes.ComboBoxHeight)
                .onSizeChanged {
                    faceWidth = it.width
                    faceHeight = it.height
                }
                .background(colors.tertiaryBg, shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.DropdownList,
                    onClick = { expanded = !expanded },
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label(selected),
                color = colors.secondaryFg,
                fontSize = LauncherType.CaptionSize,
                maxLines = 1,
                modifier = Modifier.padding(
                    start = LauncherSpacing.MediumLarge,
                    end = LauncherSizes.ComboBoxChevronColumn,
                ),
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(LauncherSizes.ComboBoxChevronColumn),
                contentAlignment = Alignment.Center,
            ) {
                ComboBoxChevron(colors.secondaryFgDim)
            }
        }
        if (expanded) {
            val gap = with(density) { LauncherSpacing.Hair.roundToPx() }
            Popup(
                offset = IntOffset(0, faceHeight + gap),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = with(density) { faceWidth.toDp() })
                        .shadow(
                            LauncherElevation.PopupShadow,
                            shape,
                            ambientColor = Color.Black.copy(alpha = LauncherElevation.POPUP_SHADOW_OPACITY),
                            spotColor = Color.Black.copy(alpha = LauncherElevation.POPUP_SHADOW_OPACITY),
                        )
                        .background(colors.secondaryBg, shape)
                        .padding(LauncherSpacing.ExtraSmall),
                ) {
                    options.forEach { option ->
                        ComboBoxItem(label(option), option == selected) {
                            expanded = false
                            onSelect(option)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComboBoxChevron(color: Color) {
    Canvas(Modifier.size(LauncherSizes.ComboBoxChevronWidth, LauncherSizes.ComboBoxChevronHeight)) {
        drawPath(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width / 2f, size.height)
                lineTo(size.width, 0f)
                close()
            },
            color,
        )
    }
}

@Composable
private fun ComboBoxItem(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val background = when {
        selected -> colors.overlayMuted
        hovered || pressed -> colors.overlayLight
        else -> Color.Transparent
    }
    Text(
        text = text,
        color = colors.secondaryFg,
        fontSize = LauncherType.CaptionSize,
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(LauncherRadii.Micro))
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = LauncherSpacing.Medium, vertical = LauncherSpacing.Small),
    )
}
