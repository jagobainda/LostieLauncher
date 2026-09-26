package dev.jagoba.lostielauncher.ui.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherOpacity
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

internal enum class DialogButtonStyle { ACCENT, SECONDARY }

@Composable
internal fun LauncherDialog(
    @DrawableRes icon: Int,
    title: String,
    width: Dp,
    minHeight: Dp,
    maxHeight: Dp,
    onDismiss: () -> Unit,
    footer: @Composable RowScope.() -> Unit,
    centerBody: Boolean = false,
    footerBackground: Color = Color.Transparent,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalLauncherColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(LauncherSizes.DialogScreenGutter),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = width)
                    .fillMaxWidth()
                    .heightIn(min = minHeight, max = maxHeight)
                    .background(colors.primaryBg)
                    .border(LauncherBorders.Thin, colors.primaryFg),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                DialogTitleBar(icon, title, onDismiss)
                Column(Modifier.weight(1f, fill = !centerBody), content = body)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(LauncherSizes.DialogFooterHeight)
                        .background(footerBackground),
                    horizontalArrangement = Arrangement.End,
                    content = footer,
                )
            }
        }
    }
}

@Composable
private fun DialogTitleBar(@DrawableRes icon: Int, title: String, onClose: () -> Unit) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(LauncherSizes.TitleBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(LauncherSpacing.Card))
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.primaryFg,
            modifier = Modifier.size(LauncherSizes.DialogTitleIcon),
        )
        Text(
            text = title,
            color = colors.secondaryFg,
            fontSize = LauncherType.BodySize,
            fontWeight = LauncherType.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .padding(start = LauncherSpacing.Medium)
                .weight(1f),
        )
        Box(
            Modifier
                .size(LauncherSizes.TitleBarButton)
                .background(if (hovered || pressed) FixedColors.WindowsClose else Color.Transparent)
                .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dialog_close),
                contentDescription = null,
                tint = colors.secondaryFg,
                modifier = Modifier.size(LauncherSizes.DialogCloseIcon),
            )
        }
    }
}

@Composable
internal fun DialogFooterButtons(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxSize()
            .padding(start = LauncherSpacing.Screen, end = LauncherSpacing.Screen, bottom = LauncherSpacing.Screen),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium, Alignment.End),
        verticalAlignment = Alignment.Bottom,
        content = content,
    )
}

@Composable
internal fun DialogButton(
    label: String,
    style: DialogButtonStyle,
    width: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val background = when (style) {
        DialogButtonStyle.ACCENT -> when {
            enabled && pressed -> colors.primaryFgPressed
            enabled && hovered -> colors.primaryFgHover
            else -> colors.primaryFg
        }

        DialogButtonStyle.SECONDARY -> when {
            enabled && pressed -> colors.overlayMuted
            enabled && hovered -> colors.overlayStrong
            else -> colors.overlayMedium
        }
    }
    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else LauncherOpacity.CARD_BUTTON_DISABLED }
            .width(width)
            .height(LauncherSizes.DialogButtonHeight)
            .background(background, RoundedCornerShape(LauncherRadii.Small))
            .clickable(
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.secondaryFg,
            fontSize = LauncherType.BodySize,
            fontWeight = if (style == DialogButtonStyle.ACCENT) LauncherType.SemiBold else LauncherType.Normal,
            maxLines = 1,
        )
    }
}

@Composable
internal fun DialogCancelButton(onClick: () -> Unit) {
    DialogButton(
        label = LocalStrings.current.btnCancel,
        style = DialogButtonStyle.SECONDARY,
        width = LauncherSizes.DialogCancelButtonWidth,
        onClick = onClick,
    )
}

@Composable
internal fun DialogLinkButton(
    @DrawableRes icon: Int,
    iconSize: Dp,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val color = if (hovered || pressed) colors.primaryFgHover else colors.primaryFg
    Row(
        modifier.clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = text,
            color = color,
            fontSize = LauncherType.CaptionSize,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.padding(start = LauncherSpacing.ExtraSmall),
        )
    }
}

@Composable
internal fun DialogKeyField(label: String, value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    val colors = LocalLauncherColors.current
    Column {
        DialogFieldLabel(R.drawable.ic_dialog_key, label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.secondaryFg, fontSize = LauncherType.CaptionSize),
            cursorBrush = SolidColor(colors.secondaryFg),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .padding(top = LauncherSpacing.ExtraSmall)
                .fillMaxWidth()
                .semantics { contentDescription = label }
                .background(colors.tertiaryBg, RoundedCornerShape(LauncherRadii.Small))
                .padding(horizontal = LauncherSpacing.MediumLarge, vertical = LauncherSpacing.Medium),
        )
    }
}

@Composable
internal fun DialogFieldLabel(
    @DrawableRes icon: Int,
    text: String,
    iconSize: Dp = LauncherSizes.DialogFieldIcon,
    trailing: String? = null,
) {
    val colors = LocalLauncherColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.secondaryFgDim,
            modifier = Modifier.size(iconSize),
        )
        Text(
            text = text,
            color = colors.secondaryFgDim,
            fontSize = LauncherType.CaptionSize,
            modifier = Modifier.padding(start = LauncherSpacing.Small),
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = colors.secondaryFg,
                fontSize = LauncherType.CaptionSize,
                fontWeight = LauncherType.SemiBold,
                modifier = Modifier.padding(start = LauncherSpacing.ExtraSmall),
            )
        }
    }
}
