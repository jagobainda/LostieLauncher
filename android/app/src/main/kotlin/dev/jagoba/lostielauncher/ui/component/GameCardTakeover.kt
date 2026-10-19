package dev.jagoba.lostielauncher.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.LauncherMotion
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.delay

internal enum class CardTakeover { INSTALL_STARTED, INSTALL_FINISHED, UNINSTALLING }

internal fun libraryCardTakeover(previous: LibraryCardStatus, current: LibraryCardStatus): CardTakeover? = when {
    previous in PROGRESS_STATUSES -> CardTakeover.INSTALL_FINISHED.takeIf { current == LibraryCardStatus.DOWNLOADED }
    current == LibraryCardStatus.DOWNLOADING -> CardTakeover.INSTALL_STARTED
    else -> null
}

@Composable
internal fun rememberLibraryCardTakeover(status: LibraryCardStatus): CardTakeover? {
    var previous by remember { mutableStateOf(status) }
    var takeover by remember { mutableStateOf<CardTakeover?>(null) }
    LaunchedEffect(status) {
        val next = libraryCardTakeover(previous, status)
        previous = status
        takeover = next
        if (next != null) {
            delay(LauncherMotion.TakeoverHold)
            takeover = null
        }
    }
    return takeover
}

@Composable
internal fun CardTakeoverGlyph(takeover: CardTakeover, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val motion = remember(takeover) { Animatable(0f) }
    LaunchedEffect(takeover) {
        val spec = if (takeover == CardTakeover.UNINSTALLING) {
            infiniteRepeatable<Float>(
                tween(LauncherMotion.EjectLoop.inWholeMilliseconds.toInt()),
                RepeatMode.Restart,
            )
        } else {
            spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)
        }
        motion.animateTo(1f, spec)
    }
    val (icon, tint) = when (takeover) {
        CardTakeover.INSTALL_STARTED -> R.drawable.ic_card_download to colors.primaryFg
        CardTakeover.INSTALL_FINISHED -> R.drawable.ic_card_check to colors.success
        CardTakeover.UNINSTALLING -> R.drawable.ic_card_eject to colors.secondaryFgDim
    }
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(LauncherSizes.CardTakeoverGlyph)
                .graphicsLayer {
                    val travel = LauncherSizes.CardTakeoverTravel.toPx()
                    when (takeover) {
                        CardTakeover.INSTALL_STARTED -> {
                            translationY = (motion.value - 1f) * travel
                            alpha = motion.value.coerceIn(0f, 1f)
                        }

                        CardTakeover.INSTALL_FINISHED -> {
                            scaleX = motion.value
                            scaleY = motion.value
                        }

                        CardTakeover.UNINSTALLING -> {
                            translationY = -motion.value * travel
                            alpha = 1f - motion.value
                        }
                    }
                },
        )
        if (takeover == CardTakeover.UNINSTALLING) {
            Text(
                LocalStrings.current.statusUninstalling,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.LabelSize,
            )
        }
    }
}
