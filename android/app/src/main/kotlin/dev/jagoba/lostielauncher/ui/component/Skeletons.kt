package dev.jagoba.lostielauncher.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherMotion
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

private val ShimmerStops = listOf(FixedColors.ShimmerEdge, FixedColors.ShimmerPeak, FixedColors.ShimmerEdge)

@Composable
fun GameCardSkeleton(modifier: Modifier = Modifier) {
    GameCardColumns(
        logo = {
            SkeletonBlock(
                LauncherRadii.Medium,
                Modifier.size(LauncherSizes.GameCardLogoWidth, LauncherSizes.GameCardLogoHeight),
            )
        },
        text = {
            Column {
                SkeletonBlock(
                    LauncherRadii.Micro,
                    Modifier.size(LauncherSizes.GameCardTitleWidth, LauncherSizes.SkeletonTitleHeight),
                )
                SkeletonBlock(
                    LauncherRadii.Micro,
                    Modifier
                        .padding(top = LauncherSpacing.Medium)
                        .size(LauncherSizes.SkeletonGameMetaWidth, LauncherSizes.SkeletonLineHeight),
                )
            }
        },
        actions = {
            SkeletonBlock(
                LauncherRadii.Small,
                Modifier.size(LauncherSizes.CardButtonMinWidth, LauncherSizes.CardButtonHeight),
            )
        },
        modifier = modifier
            .clearAndSetSemantics {}
            .background(LocalLauncherColors.current.secondaryBg, RoundedCornerShape(LauncherRadii.Medium))
            .padding(LauncherSpacing.Card),
    )
}

@Composable
fun NewsCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clearAndSetSemantics {}
            .background(LocalLauncherColors.current.overlaySubtle, RoundedCornerShape(LauncherRadii.Large))
            .padding(LauncherSpacing.Card),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(
                LauncherRadii.Small,
                Modifier.size(LauncherSizes.SkeletonTagWidth, LauncherSizes.SkeletonTitleHeight),
            )
            Spacer(Modifier.weight(1f))
            SkeletonBlock(
                LauncherRadii.Micro,
                Modifier.size(LauncherSizes.SkeletonTagWidth, LauncherSizes.SkeletonLineHeight),
            )
        }
        SkeletonBlock(
            LauncherRadii.Micro,
            Modifier
                .padding(top = LauncherSpacing.MediumLarge, bottom = LauncherSpacing.ExtraSmall)
                .size(LauncherSizes.SkeletonNewsTitleWidth, LauncherSizes.SkeletonTitleHeight),
        )
        SkeletonBlock(
            LauncherRadii.Micro,
            Modifier
                .fillMaxWidth()
                .height(LauncherSizes.SkeletonLineHeight),
        )
        SkeletonBlock(
            LauncherRadii.Micro,
            Modifier
                .padding(top = LauncherSpacing.ExtraSmall)
                .size(LauncherSizes.SkeletonNewsLineWidth, LauncherSizes.SkeletonLineHeight),
        )
    }
}

@Composable
fun NotificationCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics {}
            .background(LocalLauncherColors.current.overlaySubtle, RoundedCornerShape(LauncherRadii.Large))
            .padding(LauncherSpacing.ExtraLarge),
    ) {
        SkeletonBlock(
            LauncherRadii.Hair,
            Modifier
                .width(LauncherSizes.NotificationStripeWidth)
                .fillMaxHeight(),
        )
        Spacer(Modifier.width(LauncherSpacing.Large))
        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        ) {
            SkeletonBlock(
                LauncherRadii.Micro,
                Modifier.size(
                    LauncherSizes.SkeletonNotificationTitleWidth,
                    LauncherSizes.SkeletonNotificationTitleHeight,
                ),
            )
            SkeletonBlock(
                LauncherRadii.Micro,
                Modifier
                    .padding(top = LauncherSpacing.ExtraSmall)
                    .fillMaxWidth()
                    .height(LauncherSizes.SkeletonNotificationLineHeight),
            )
        }
        SkeletonBlock(
            LauncherRadii.Micro,
            Modifier
                .padding(start = LauncherSpacing.Medium, top = LauncherSpacing.Hair)
                .size(LauncherSizes.SkeletonNotificationDateWidth, LauncherSizes.SkeletonNotificationLineHeight),
        )
    }
}

@Composable
private fun SkeletonBlock(radius: Dp, modifier: Modifier) {
    val progress = rememberInfiniteTransition(label = "skeleton-shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(LauncherMotion.Shimmer.inWholeMilliseconds.toInt(), easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "skeleton-shimmer-progress",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .drawBehind {
                val width = size.width
                val sweep = progress.value * 2f
                drawRect(
                    Brush.linearGradient(
                        colors = ShimmerStops,
                        start = Offset((sweep - 1f) * width, 0f),
                        end = Offset(sweep * width, 0f),
                    ),
                )
            },
    )
}
