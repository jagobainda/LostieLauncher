package dev.jagoba.lostielauncher.ui.dialog

import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.widget.doAfterTextChanged
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
import dev.jagoba.lostielauncher.ui.theme.inputSurface

internal enum class DialogButtonStyle { ACCENT, SECONDARY }

internal fun useScrollingDialogTitle(height: Dp, keyboardVisible: Boolean): Boolean =
    keyboardVisible && height < LauncherSizes.TitleBarHeight + LauncherSizes.DialogFooterHeight +
        LauncherSizes.MinimumTouchTarget + LauncherSizes.DialogScreenGutter * 2

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
    val preferredHeight = Modifier.heightIn(min = minHeight, max = maxHeight)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = LauncherSizes.DialogScreenGutter),
            contentAlignment = Alignment.Center,
        ) {
            val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            val compact = useScrollingDialogTitle(this.maxHeight, keyboardVisible)
            Column(
                Modifier
                    .padding(vertical = if (compact) 0.dp else LauncherSizes.DialogScreenGutter)
                    .widthIn(max = width)
                    .fillMaxWidth()
                    .then(preferredHeight)
                    .background(colors.primaryBg)
                    .border(LauncherBorders.Thin, colors.primaryFg),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!compact) DialogTitleBar(icon, title, onDismiss)
                Column(Modifier.weight(1f, fill = !centerBody).verticalScroll(rememberScrollState())) {
                    if (compact) DialogTitleBar(icon, title, onDismiss)
                    body()
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(if (compact) LauncherSizes.MinimumTouchTarget else LauncherSizes.DialogFooterHeight)
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
    val closeLabel = LocalStrings.current.btnClose
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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = LauncherSpacing.Medium)
                .weight(1f),
        )
        Box(
            Modifier
                .size(LauncherSizes.TitleBarButton)
                .background(if (hovered || pressed) FixedColors.WindowsClose else Color.Transparent)
                .semantics { contentDescription = closeLabel }
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
            .padding(start = LauncherSpacing.Screen, end = LauncherSpacing.Screen, top = LauncherSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium, Alignment.End),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

@Composable
internal fun DialogButton(
    label: String,
    style: DialogButtonStyle,
    onClick: () -> Unit,
    width: Dp = Dp.Unspecified,
    horizontalPadding: Dp = 0.dp,
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
            )
            .padding(horizontal = horizontalPadding),
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
internal fun DialogKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    error: String? = null,
) {
    val colors = LocalLauncherColors.current
    val change by rememberUpdatedState(onValueChange)
    val confirm by rememberUpdatedState(onDone)
    val requester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val shape = RoundedCornerShape(LauncherRadii.Small)
    LaunchedEffect(focused, imeBottom, error) {
        if ((focused && imeBottom > 0) || error != null) requester.bringIntoView()
    }
    Column(Modifier.bringIntoViewRequester(requester)) {
        DialogFieldLabel(R.drawable.ic_dialog_key, label)
        AndroidView(
            factory = { context ->
                EditText(context, null, 0, R.style.Widget_Launcher_KeyField).apply {
                    inputType = InputType.TYPE_CLASS_TEXT
                    setSingleLine()
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    background = null
                    includeFontPadding = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, LauncherType.CaptionSize.value)
                    setOnFocusChangeListener { _, hasFocus -> focused = hasFocus }
                    doAfterTextChanged { change(it.toString()) }
                    setOnEditorActionListener { _, action, event ->
                        when {
                            event?.keyCode == KeyEvent.KEYCODE_ENTER -> {
                                if (event.action == KeyEvent.ACTION_UP) confirm()
                                true
                            }

                            action == EditorInfo.IME_ACTION_DONE -> {
                                confirm()
                                true
                            }

                            else -> false
                        }
                    }
                }
            },
            update = { field ->
                field.contentDescription = label
                field.setTextColor(colors.secondaryFg.toArgb())
                with(density) {
                    val horizontal = LauncherSpacing.MediumLarge.roundToPx()
                    val vertical = LauncherSpacing.Medium.roundToPx()
                    field.setPadding(horizontal, vertical, horizontal, vertical)
                }
                if (field.text.toString() != value) {
                    field.setText(value)
                    field.setSelection(value.length)
                }
            },
            modifier = Modifier
                .padding(top = LauncherSpacing.ExtraSmall)
                .fillMaxWidth()
                .inputSurface(colors, shape)
                .then(
                    if (error != null) {
                        Modifier.border(LauncherBorders.Thin, FixedColors.NotificationExclamation, shape)
                    } else {
                        Modifier
                    },
                ),
        )
        if (error != null) {
            Row(Modifier.padding(top = LauncherSpacing.Small), verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(R.drawable.ic_dialog_alert),
                    contentDescription = null,
                    tint = FixedColors.NotificationExclamation,
                    modifier = Modifier
                        .padding(top = LauncherSpacing.Hair)
                        .size(LauncherSizes.DialogFieldIcon),
                )
                Text(
                    text = error,
                    color = colors.secondaryFg,
                    fontSize = LauncherType.LabelSize,
                    modifier = Modifier.padding(start = LauncherSpacing.Small),
                )
            }
        }
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
