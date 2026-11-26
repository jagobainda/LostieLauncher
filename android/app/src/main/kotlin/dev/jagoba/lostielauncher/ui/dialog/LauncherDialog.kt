package dev.jagoba.lostielauncher.ui.dialog

import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.Window
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
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
import dev.jagoba.lostielauncher.ui.theme.accentButton
import dev.jagoba.lostielauncher.ui.theme.inputSurface

internal enum class DialogButtonStyle { ACCENT, SECONDARY }

internal enum class TouchDecision { ALLOW, DROP, CANCEL_AND_DROP }

internal const val KEY_FIELD_IME_OPTIONS: Int =
    EditorInfo.IME_ACTION_DONE or EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING

internal fun keyFieldInputType(visible: Boolean): Int = InputType.TYPE_CLASS_TEXT or
    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
    if (visible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD

internal fun isTouchObscured(flags: Int, obscuredMask: Int): Boolean = (flags and obscuredMask) != 0

internal class ObscuredTouchFilter {
    private var gestureActive = false
    private var gestureBlocked = false

    fun evaluate(actionMasked: Int, obscured: Boolean): TouchDecision = when (actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            if (obscured) {
                gestureActive = false
                gestureBlocked = true
                TouchDecision.DROP
            } else {
                gestureActive = true
                gestureBlocked = false
                TouchDecision.ALLOW
            }
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            val wasActive = gestureActive
            val wasBlocked = gestureBlocked
            gestureActive = false
            gestureBlocked = false
            when {
                wasBlocked -> TouchDecision.DROP
                obscured && wasActive -> TouchDecision.CANCEL_AND_DROP
                obscured -> TouchDecision.DROP
                else -> TouchDecision.ALLOW
            }
        }

        else -> when {
            gestureBlocked -> TouchDecision.DROP

            obscured && gestureActive -> {
                gestureActive = false
                gestureBlocked = true
                TouchDecision.CANCEL_AND_DROP
            }

            obscured -> TouchDecision.DROP

            else -> TouchDecision.ALLOW
        }
    }
}

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
    secure: Boolean = false,
    protectFromOverlays: Boolean = false,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalLauncherColors.current
    val preferredHeight = Modifier.heightIn(min = minHeight, max = maxHeight)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            securePolicy = if (secure) SecureFlagPolicy.SecureOn else SecureFlagPolicy.Inherit,
        ),
    ) {
        if (protectFromOverlays) DialogOverlayProtection()
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
                        .heightIn(
                            min = if (compact) LauncherSizes.MinimumTouchTarget else LauncherSizes.DialogFooterHeight,
                        )
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
            .heightIn(min = LauncherSizes.TitleBarHeight),
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
            modifier = Modifier
                .padding(start = LauncherSpacing.Medium, top = LauncherSpacing.Small, bottom = LauncherSpacing.Small)
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
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LauncherSpacing.Screen, vertical = LauncherSpacing.Small),
        horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Medium),
        content = content,
    )
}

@Composable
internal fun DialogButton(
    label: String,
    style: DialogButtonStyle,
    onClick: () -> Unit,
    width: Dp = Dp.Unspecified,
    horizontalPadding: Dp = LauncherSpacing.MediumLarge,
    enabled: Boolean = true,
) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val background = when (style) {
        DialogButtonStyle.ACCENT -> colors.accentButton(enabled, pressed, hovered)

        DialogButtonStyle.SECONDARY -> when {
            enabled && pressed -> colors.overlayMuted
            enabled && hovered -> colors.overlayStrong
            else -> colors.overlayMedium
        }
    }
    Box(
        Modifier
            .graphicsLayer { alpha = if (enabled) 1f else LauncherOpacity.CARD_BUTTON_DISABLED }
            .widthIn(min = width)
            .heightIn(min = LauncherSizes.DialogButtonHeight)
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
            textAlign = TextAlign.Center,
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
private fun DialogOverlayProtection() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window
    DisposableEffect(view, window) {
        val previousFilter = view.filterTouchesWhenObscured
        view.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window?.setHideOverlayWindows(true)
        }
        val wrapped = window?.callback?.let { original ->
            ObscuredTouchWindowCallback(original).also { window.callback = it }
        }
        onDispose {
            view.filterTouchesWhenObscured = previousFilter
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window?.setHideOverlayWindows(false)
            }
            if (wrapped != null && window.callback === wrapped) {
                window.callback = wrapped.delegate
            }
        }
    }
}

private class ObscuredTouchWindowCallback(val delegate: Window.Callback) : Window.Callback by delegate {
    private val filter = ObscuredTouchFilter()
    private val obscuredMask: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MotionEvent.FLAG_WINDOW_IS_OBSCURED or MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED
    } else {
        MotionEvent.FLAG_WINDOW_IS_OBSCURED
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return delegate.dispatchTouchEvent(null)
        val obscured = isTouchObscured(event.flags, obscuredMask)
        return when (filter.evaluate(event.actionMasked, obscured)) {
            TouchDecision.ALLOW -> delegate.dispatchTouchEvent(event)

            TouchDecision.DROP -> true

            TouchDecision.CANCEL_AND_DROP -> {
                val cancel = MotionEvent.obtain(event)
                try {
                    cancel.action = MotionEvent.ACTION_CANCEL
                    delegate.dispatchTouchEvent(cancel)
                } finally {
                    cancel.recycle()
                }
                true
            }
        }
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
    var keyVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val shape = RoundedCornerShape(LauncherRadii.Small)
    LaunchedEffect(focused, imeBottom, error) {
        if ((focused && imeBottom > 0) || error != null) requester.bringIntoView()
    }
    Column(Modifier.bringIntoViewRequester(requester)) {
        DialogFieldLabel(R.drawable.ic_dialog_key, label)
        Row(
            Modifier
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndroidView(
                factory = { context ->
                    EditText(context, null, 0, R.style.Widget_Launcher_KeyField).apply {
                        val defaultTypeface = typeface
                        setSingleLine()
                        inputType = keyFieldInputType(visible = false)
                        typeface = defaultTypeface
                        imeOptions = KEY_FIELD_IME_OPTIONS
                        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
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
                        val start = LauncherSpacing.MediumLarge.roundToPx()
                        val vertical = LauncherSpacing.Medium.roundToPx()
                        val end = LauncherSpacing.ExtraSmall.roundToPx()
                        field.setPaddingRelative(start, vertical, end, vertical)
                    }
                    val desiredInputType = keyFieldInputType(keyVisible)
                    if (field.inputType != desiredInputType) {
                        val start = field.selectionStart.coerceIn(0, field.length())
                        val end = field.selectionEnd.coerceIn(0, field.length())
                        val currentTypeface = field.typeface
                        field.inputType = desiredInputType
                        field.typeface = currentTypeface
                        field.setSelection(start, end)
                    }
                    if (field.text.toString() != value) {
                        field.setText(value)
                        field.setSelection(value.length)
                    }
                },
                modifier = Modifier.weight(1f),
            )
            KeyVisibilityToggle(
                visible = keyVisible,
                onToggle = { keyVisible = !keyVisible },
            )
        }
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
private fun KeyVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    val colors = LocalLauncherColors.current
    val strings = LocalStrings.current
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val pressed by interactions.collectIsPressedAsState()
    val active = hovered || pressed
    val label = if (visible) strings.hideKey else strings.showKey
    Box(
        Modifier
            .padding(end = LauncherSpacing.ExtraSmall)
            .size(LauncherSizes.DialogKeyToggleButton)
            .then(
                if (active) {
                    Modifier.background(colors.overlayLight, RoundedCornerShape(LauncherRadii.Small))
                } else {
                    Modifier
                },
            )
            .semantics { contentDescription = label }
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (visible) R.drawable.ic_dialog_eye_off else R.drawable.ic_dialog_eye),
            contentDescription = null,
            tint = if (active) colors.secondaryFg else colors.secondaryFgDim,
            modifier = Modifier.size(LauncherSizes.DialogKeyToggleIcon),
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
    FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
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
        }
        if (trailing != null) {
            Text(
                text = trailing,
                color = colors.secondaryFg,
                fontSize = LauncherType.CaptionSize,
                fontWeight = LauncherType.SemiBold,
                softWrap = false,
                modifier = Modifier.padding(start = LauncherSpacing.ExtraSmall),
            )
        }
    }
}
