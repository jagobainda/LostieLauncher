package dev.jagoba.lostielauncher.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherMotion
import dev.jagoba.lostielauncher.ui.theme.LauncherOpacity
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlinx.coroutines.withTimeoutOrNull

@Immutable
data class LibraryGameCardState(
    val title: String,
    val logoUrl: String?,
    val size: String,
    val version: String,
    val playtime: String,
    val status: LibraryCardStatus,
    val progress: Float = 0f,
    val remainingTime: String? = null,
    val speed: String? = null,
    val canStart: Boolean = true,
    val canPause: Boolean = true,
    val canCancel: Boolean = true,
    val canUpdate: Boolean = true,
)

@Immutable
data class InstalledGameCardState(
    val title: String,
    val logoUrl: String?,
    val installedVersion: String,
    val specialType: String?,
    val updateVersion: String?,
    val playtime: String,
    val showHelp: Boolean,
    val canPlay: Boolean = true,
    val canUpdate: Boolean = true,
    val canSwitchSpecialVersion: Boolean = true,
    val canOpenFolder: Boolean = true,
    val canUninstall: Boolean = true,
    val isUninstalling: Boolean = false,
    val runtimeSupported: Boolean = true,
)

private enum class CardButtonStyle { ACCENT, SUCCESS, SECONDARY }

private val PROGRESS_STATUSES = setOf(
    LibraryCardStatus.DOWNLOADING,
    LibraryCardStatus.PAUSED,
    LibraryCardStatus.VERIFYING_INTEGRITY,
    LibraryCardStatus.EXTRACTING,
    LibraryCardStatus.INSTALLATION_PENDING,
)

private val FINISHING_STATUSES = setOf(
    LibraryCardStatus.VERIFYING_INTEGRITY,
    LibraryCardStatus.EXTRACTING,
    LibraryCardStatus.INSTALLATION_PENDING,
)

private const val FULL_PROGRESS = 100f

private val SineInOut = Easing { fraction -> -(cos(PI * fraction).toFloat() - 1f) / 2f }

@Composable
fun LibraryGameCard(
    state: LibraryGameCardState,
    onDownload: () -> Unit,
    onPause: () -> Unit,
    onCancel: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    GameCardFrame(
        logoUrl = state.logoUrl,
        pulsing = state.status == LibraryCardStatus.DOWNLOADING,
        modifier = modifier,
        text = {
            CardTitle(state.title)
            FlowRow(
                modifier = Modifier.padding(top = LauncherSpacing.Small),
                horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.ExtraLarge),
            ) {
                MetaItem(R.drawable.ic_card_harddisk, state.size)
                MetaItem(R.drawable.ic_card_tag, state.version)
                if (state.playtime.isNotEmpty()) MetaItem(R.drawable.ic_card_clock, state.playtime)
            }
            if (state.status in PROGRESS_STATUSES) DownloadProgress(state)
            when (state.status) {
                LibraryCardStatus.INSTALLATION_UNSUPPORTED -> StatusNotice(strings.statusNotSupportedYet)
                LibraryCardStatus.INSTALLATION_FAILED -> StatusNotice(strings.downloadErrorTitle)
                else -> Unit
            }
        },
        actions = {
            when (state.status) {
                LibraryCardStatus.AVAILABLE -> CardButton(
                    style = CardButtonStyle.ACCENT,
                    icon = R.drawable.ic_card_download,
                    label = strings.btnDownload,
                    enabled = state.canStart,
                    onClick = onDownload,
                )

                LibraryCardStatus.DOWNLOADING -> Row {
                    CardButton(
                        style = CardButtonStyle.ACCENT,
                        icon = R.drawable.ic_card_pause,
                        label = strings.btnPause,
                        enabled = state.canPause,
                        onClick = onPause,
                    )
                    Spacer(Modifier.width(LauncherSpacing.Small))
                    CancelButton(state.canCancel, onCancel)
                }

                LibraryCardStatus.PAUSED -> Row {
                    CardButton(
                        style = CardButtonStyle.ACCENT,
                        icon = R.drawable.ic_card_play,
                        label = strings.btnResume,
                        enabled = state.canStart,
                        onClick = onDownload,
                    )
                    Spacer(Modifier.width(LauncherSpacing.Small))
                    CancelButton(state.canCancel, onCancel)
                }

                LibraryCardStatus.DOWNLOADED, LibraryCardStatus.INSTALLATION_UNSUPPORTED -> CardButton(
                    style = CardButtonStyle.SECONDARY,
                    icon = R.drawable.ic_card_check,
                    label = strings.btnDownloaded,
                    enabled = false,
                    disabledAlpha = LauncherOpacity.CARD_STATUS_CHIP,
                    iconTint = LocalLauncherColors.current.success,
                    onClick = {},
                )

                LibraryCardStatus.UPDATE_AVAILABLE -> CardButton(
                    style = CardButtonStyle.SUCCESS,
                    icon = R.drawable.ic_card_update,
                    label = strings.btnUpdate,
                    enabled = state.canUpdate,
                    onClick = onUpdate,
                )

                else -> Unit
            }
        },
    )
}

@Composable
fun InstalledGameCard(
    state: InstalledGameCardState,
    onPlay: () -> Unit,
    onUpdate: () -> Unit,
    onOpenHelp: () -> Unit,
    onSwitchSpecialVersion: () -> Unit,
    onOpenFolder: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val colors = LocalLauncherColors.current
    val unsupported = !state.runtimeSupported
    GameCardFrame(
        logoUrl = state.logoUrl,
        pulsing = false,
        modifier = modifier,
        text = {
            CardTitle(state.title)
            FlowRow(
                modifier = Modifier.padding(top = LauncherSpacing.Small),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                MetaItem(R.drawable.ic_card_tag, state.installedVersion)
                if (!state.specialType.isNullOrEmpty()) {
                    Text(
                        text = state.specialType,
                        color = colors.secondaryBg,
                        fontSize = LauncherType.BadgeSize,
                        fontWeight = LauncherType.Bold,
                        modifier = Modifier
                            .padding(start = LauncherSpacing.Medium)
                            .background(colors.primaryFg, RoundedCornerShape(LauncherRadii.Micro))
                            .padding(horizontal = LauncherSpacing.Small, vertical = LauncherSpacing.Hair),
                    )
                }
                if (state.updateVersion != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_card_arrow_right),
                            contentDescription = null,
                            tint = colors.primaryFg,
                            modifier = Modifier
                                .padding(horizontal = LauncherSpacing.Medium)
                                .size(LauncherSizes.CardUpdateArrow),
                        )
                        Text(
                            text = state.updateVersion,
                            color = colors.primaryFg,
                            fontSize = LauncherType.CaptionSize,
                            fontWeight = LauncherType.SemiBold,
                        )
                    }
                }
            }
            if (state.playtime.isNotEmpty()) {
                MetaItem(
                    R.drawable.ic_card_clock,
                    state.playtime,
                    Modifier.padding(top = LauncherSpacing.ExtraSmall),
                )
            }
        },
        actions = {
            if (state.isUninstalling) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UninstallSpinner()
                    Spacer(Modifier.width(LauncherSpacing.Small))
                    Text(strings.statusUninstalling, color = colors.secondaryFgDim, fontSize = LauncherType.LabelSize)
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Small, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
                ) {
                    CardButton(
                        style = CardButtonStyle.ACCENT,
                        icon = R.drawable.ic_card_play,
                        label = strings.btnPlay,
                        enabled = state.canPlay,
                        unsupported = unsupported,
                        onClick = onPlay,
                    )
                    if (state.updateVersion != null) {
                        CardButton(
                            style = CardButtonStyle.SUCCESS,
                            icon = R.drawable.ic_card_update,
                            label = strings.btnUpdate,
                            enabled = state.canUpdate,
                            onClick = onUpdate,
                        )
                    }
                    if (state.showHelp) {
                        GameIconButton(R.drawable.ic_card_help, strings.tooltipOpenHelp, true, unsupported, onOpenHelp)
                    }
                    GameIconButton(
                        R.drawable.ic_card_key,
                        strings.tooltipSwitchSpecialVersion,
                        state.canSwitchSpecialVersion,
                        false,
                        onSwitchSpecialVersion,
                    )
                    GameIconButton(
                        R.drawable.ic_card_folder_open,
                        strings.tooltipOpenFolder,
                        state.canOpenFolder,
                        unsupported,
                        onOpenFolder,
                    )
                    GameIconButton(
                        R.drawable.ic_card_trash,
                        strings.tooltipUninstall,
                        state.canUninstall,
                        unsupported,
                        onUninstall,
                    )
                }
            }
        },
    )
}

@Composable
private fun GameCardFrame(
    logoUrl: String?,
    pulsing: Boolean,
    modifier: Modifier,
    text: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
) {
    val colors = LocalLauncherColors.current
    val shape = RoundedCornerShape(LauncherRadii.Medium)
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(pulsing) {
        if (!pulsing) {
            pulse.snapTo(0f)
            return@LaunchedEffect
        }
        pulse.snapTo(LauncherMotion.PULSE_MIN_ALPHA)
        val half = tween<Float>(LauncherMotion.Pulse.inWholeMilliseconds.toInt(), easing = SineInOut)
        withTimeoutOrNull(LauncherMotion.PulseTotal) {
            while (true) {
                pulse.animateTo(LauncherMotion.PULSE_MAX_ALPHA, half)
                pulse.animateTo(LauncherMotion.PULSE_MIN_ALPHA, half)
            }
        }
        pulse.snapTo(0f)
    }
    Box(modifier.fillMaxWidth()) {
        GameCardColumns(
            logo = { GameLogo(logoUrl) },
            text = { Column(content = text) },
            actions = actions,
            modifier = Modifier
                .background(colors.secondaryBg, shape)
                .padding(LauncherSpacing.Card),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = pulse.value }
                .border(LauncherBorders.Thick, colors.primaryFg, shape),
        )
    }
}

@Composable
internal fun GameCardColumns(
    logo: @Composable () -> Unit,
    text: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf({ Box { logo() } }, { Box { text() } }, { Box { actions() } }),
        modifier = modifier.fillMaxWidth(),
    ) { (logoSlot, textSlot, actionsSlot), constraints ->
        val width = constraints.maxWidth
        val inset = LauncherSpacing.Card.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val logoPlaceable = logoSlot.first().measure(loose)
        val actionsWidth = actionsSlot.first().maxIntrinsicWidth(constraints.maxHeight)
        val inlineTextWidth = width - logoPlaceable.width - actionsWidth - 2 * inset
        if (inlineTextWidth >= LauncherSizes.GameCardTitleWidth.roundToPx()) {
            val actionsPlaceable = actionsSlot.first().measure(loose)
            val textPlaceable = textSlot.first().measure(
                loose.copy(minWidth = inlineTextWidth, maxWidth = inlineTextWidth),
            )
            val height = maxOf(logoPlaceable.height, textPlaceable.height, actionsPlaceable.height)
            layout(width, height) {
                logoPlaceable.place(0, (height - logoPlaceable.height) / 2)
                textPlaceable.place(logoPlaceable.width + inset, (height - textPlaceable.height) / 2)
                actionsPlaceable.place(width - actionsPlaceable.width, (height - actionsPlaceable.height) / 2)
            }
        } else {
            val textWidth = (width - logoPlaceable.width - inset).coerceAtLeast(0)
            val textPlaceable = textSlot.first().measure(loose.copy(minWidth = textWidth, maxWidth = textWidth))
            val actionsPlaceable = actionsSlot.first().measure(loose)
            val top = maxOf(logoPlaceable.height, textPlaceable.height)
            val gap = if (actionsPlaceable.height > 0) LauncherSpacing.Large.roundToPx() else 0
            layout(width, top + gap + actionsPlaceable.height) {
                logoPlaceable.place(0, (top - logoPlaceable.height) / 2)
                textPlaceable.place(logoPlaceable.width + inset, (top - textPlaceable.height) / 2)
                actionsPlaceable.place(width - actionsPlaceable.width, top + gap)
            }
        }
    }
}

@Composable
internal fun GameLogo(
    url: String?,
    width: Dp = LauncherSizes.GameCardLogoWidth,
    height: Dp = LauncherSizes.GameCardLogoHeight,
    placeholderSize: Dp = LauncherSizes.GameCardLogoPlaceholder,
) {
    val colors = LocalLauncherColors.current
    val model = remember(url) { HttpsUrls.parseOrNull(url)?.toString() }
    var loaded by remember(model) { mutableStateOf(false) }
    Box(
        Modifier
            .size(width, height)
            .clip(RoundedCornerShape(LauncherRadii.Medium))
            .background(colors.tertiaryBg)
            .padding(LauncherSpacing.Snug),
        contentAlignment = Alignment.Center,
    ) {
        if (!loaded) {
            Icon(
                painter = painterResource(R.drawable.ic_card_pokeball),
                contentDescription = null,
                tint = colors.overlayMedium,
                modifier = Modifier.size(placeholderSize),
            )
        }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.High,
                onState = { loaded = it is AsyncImagePainter.State.Success },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CardTitle(title: String) {
    Text(
        text = title,
        color = LocalLauncherColors.current.secondaryFg,
        fontSize = LauncherType.HeadingSize,
        fontWeight = LauncherType.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MetaItem(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier) {
    val color = LocalLauncherColors.current.secondaryFgDim
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(LauncherSizes.CardMetaIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.ExtraSmall))
        Text(text, color = color, fontSize = LauncherType.CaptionSize)
    }
}

@Composable
private fun DownloadProgress(state: LibraryGameCardState) {
    val colors = LocalLauncherColors.current
    val strings = LocalStrings.current
    val finishing = state.status in FINISHING_STATUSES
    val value = if (finishing) FULL_PROGRESS else state.progress.coerceIn(0f, FULL_PROGRESS)
    val downloading = state.status == LibraryCardStatus.DOWNLOADING
    val barShape = RoundedCornerShape(LauncherRadii.Hair)
    Column(Modifier.padding(top = LauncherSpacing.Medium)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(LauncherSizes.ProgressBarHeight)
                .background(colors.overlayLight, barShape),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / FULL_PROGRESS)
                    .height(LauncherSizes.ProgressBarHeight)
                    .background(colors.primaryFg, barShape),
            )
        }
        Row(Modifier.padding(top = LauncherSpacing.Micro)) {
            Row(Modifier.weight(1f)) {
                when (state.status) {
                    LibraryCardStatus.EXTRACTING -> ProgressLabel(strings.statusExtracting)
                    LibraryCardStatus.VERIFYING_INTEGRITY -> ProgressLabel(strings.statusVerifying)
                    else -> ProgressLabel("${value.roundToInt()}%")
                }
                val remaining = state.remainingTime
                if (downloading && !remaining.isNullOrEmpty()) {
                    ProgressLabel("· $remaining", Modifier.padding(start = LauncherSpacing.ExtraSmall))
                }
            }
            val speed = state.speed
            if (downloading && speed != null) ProgressLabel(speed)
        }
    }
}

@Composable
private fun ProgressLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LocalLauncherColors.current.secondaryFgDim,
        fontSize = LauncherType.LabelSize,
        modifier = modifier,
    )
}

@Composable
internal fun StatusNotice(text: String) {
    Row(Modifier.padding(top = LauncherSpacing.Medium), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_card_not_supported),
            contentDescription = null,
            tint = FixedColors.Warning,
            modifier = Modifier.size(LauncherSizes.CardMetaIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.ExtraSmall))
        ProgressLabel(text)
    }
}

@Composable
private fun CancelButton(enabled: Boolean, onClick: () -> Unit) {
    CardButton(
        style = CardButtonStyle.SECONDARY,
        icon = R.drawable.ic_card_close,
        tooltip = LocalStrings.current.btnCancel,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun GameIconButton(
    @DrawableRes icon: Int,
    tooltip: String,
    enabled: Boolean,
    unsupported: Boolean,
    onClick: () -> Unit,
) {
    CardButton(
        style = CardButtonStyle.SECONDARY,
        icon = icon,
        tooltip = tooltip,
        enabled = enabled,
        unsupported = unsupported,
        iconSize = LauncherSizes.CardIconButtonIcon,
        onClick = onClick,
    )
}

@Composable
private fun CardButton(
    style: CardButtonStyle,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    label: String? = null,
    tooltip: String? = null,
    enabled: Boolean = true,
    unsupported: Boolean = false,
    disabledAlpha: Float = LauncherOpacity.CARD_BUTTON_DISABLED,
    iconTint: Color = LocalLauncherColors.current.secondaryFg,
    iconSize: Dp = LauncherSizes.CardButtonIcon,
) {
    val colors = LocalLauncherColors.current
    val notSupported = LocalStrings.current.statusNotSupportedYet
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val active = enabled && (pressed || hovered)
    val background = when (style) {
        CardButtonStyle.ACCENT -> when {
            enabled && pressed -> colors.primaryFgPressed
            active -> colors.primaryFgHover
            else -> colors.primaryFg
        }

        CardButtonStyle.SUCCESS -> colors.success

        CardButtonStyle.SECONDARY -> when {
            enabled && pressed -> colors.overlayLight
            active -> colors.overlayMedium
            else -> colors.overlayMuted
        }
    }
    val alpha = when {
        !enabled -> disabledAlpha
        style == CardButtonStyle.SUCCESS && pressed -> LauncherOpacity.SUCCESS_PRESSED
        style == CardButtonStyle.SUCCESS && hovered -> LauncherOpacity.SUCCESS_HOVER
        else -> 1f
    }
    val labelled = label != null
    val description = label ?: tooltip
    val tooltipText = if (unsupported) listOfNotNull(tooltip ?: label, notSupported).joinToString("\n") else tooltip
    val secondary = style == CardButtonStyle.SECONDARY
    val minWidth = if (secondary) LauncherSizes.CardIconButtonMinWidth else LauncherSizes.CardButtonMinWidth
    val fontWeight = if (secondary) LauncherType.Normal else LauncherType.SemiBold
    val button: @Composable () -> Unit = {
        Box(
            Modifier
                .graphicsLayer { this.alpha = alpha }
                .height(LauncherSizes.CardButtonHeight)
                .defaultMinSize(minWidth = minWidth)
                .background(background, RoundedCornerShape(LauncherRadii.Small))
                .semantics {
                    description?.let { contentDescription = it }
                    if (unsupported) stateDescription = notSupported
                }
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .then(if (secondary) Modifier.padding(horizontal = LauncherSpacing.MediumLarge) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize),
                )
                if (labelled) {
                    Spacer(Modifier.width(LauncherSpacing.Small))
                    Text(
                        text = label.orEmpty(),
                        color = colors.secondaryFg,
                        fontSize = LauncherType.CaptionSize,
                        fontWeight = fontWeight,
                        maxLines = 1,
                    )
                }
            }
            if (unsupported) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = LauncherSpacing.Micro)
                        .size(LauncherSizes.NotSupportedMarker)
                        .background(FixedColors.Warning, CircleShape),
                )
            }
        }
    }
    if (tooltipText != null) LauncherTooltip(text = tooltipText, content = button) else button()
}

@Composable
private fun UninstallSpinner() {
    val color = LocalLauncherColors.current.secondaryFgDim
    val angle = rememberInfiniteTransition(label = "uninstall-spinner").animateFloat(
        initialValue = 0f,
        targetValue = FULL_TURN,
        animationSpec = infiniteRepeatable(
            tween(LauncherMotion.SpinnerTurn.inWholeMilliseconds.toInt(), easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "uninstall-spinner-angle",
    )
    Canvas(Modifier.size(LauncherSizes.SpinnerSize).graphicsLayer { rotationZ = angle.value }) {
        val stroke = LauncherSizes.SpinnerStrokeWidth.toPx()
        drawCircle(
            color = color,
            radius = (size.minDimension - stroke) / 2f,
            style = Stroke(
                width = stroke,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(LauncherSizes.SPINNER_DASH * stroke, LauncherSizes.SPINNER_GAP * stroke),
                ),
            ),
        )
    }
}

private const val FULL_TURN = 360f
