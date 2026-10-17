package dev.jagoba.lostielauncher.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.NotificationType
import dev.jagoba.lostielauncher.ui.LocalAppLanguage
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.util.format.CardDateFormatter
import java.time.LocalDateTime

private const val EXPANDED_ROTATION = 180f

@Composable
fun NewsCard(
    tag: String,
    date: LocalDateTime,
    title: String,
    description: String,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val language = LocalAppLanguage.current
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.overlaySubtle, RoundedCornerShape(LauncherRadii.Large))
            .padding(LauncherSpacing.Card),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tag,
                color = colors.secondaryFg,
                fontSize = LauncherType.LabelSize,
                fontWeight = LauncherType.SemiBold,
                modifier = Modifier
                    .background(colors.primaryFg, RoundedCornerShape(LauncherRadii.Small))
                    .padding(horizontal = LauncherSpacing.Medium, vertical = LauncherSpacing.Hair),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = CardDateFormatter.news(date, language),
                color = colors.secondaryFgDim,
                fontSize = LauncherType.LabelSize,
            )
        }
        Text(
            text = title,
            color = colors.secondaryFg,
            fontSize = LauncherType.SubheadingSize,
            fontWeight = LauncherType.SemiBold,
            modifier = Modifier.padding(top = LauncherSpacing.MediumLarge, bottom = LauncherSpacing.ExtraSmall),
        )
        LinkText(
            text = description,
            color = colors.secondaryFgDim,
            fontSize = LauncherType.BodySize,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
fun NotificationCard(
    title: String,
    message: String,
    type: NotificationType,
    date: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val language = LocalAppLanguage.current
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(colors.overlaySubtle, RoundedCornerShape(LauncherRadii.Large))
            .padding(LauncherSpacing.ExtraLarge),
    ) {
        Box(
            Modifier
                .width(LauncherSizes.NotificationStripeWidth)
                .fillMaxHeight()
                .background(type.stripeColor(), RoundedCornerShape(LauncherRadii.Hair)),
        )
        Spacer(Modifier.width(LauncherSpacing.Large))
        Column(
            Modifier
                .weight(1f)
                .align(Alignment.CenterVertically),
        ) {
            Text(
                text = title,
                color = colors.secondaryFg,
                fontSize = LauncherType.BodySize,
                fontWeight = LauncherType.SemiBold,
            )
            Text(
                text = message,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier.padding(top = LauncherSpacing.ExtraSmall),
            )
        }
        Text(
            text = CardDateFormatter.notification(date, language),
            color = colors.secondaryFgDim,
            fontSize = LauncherType.LabelSize,
            modifier = Modifier.padding(start = LauncherSpacing.Medium, top = LauncherSpacing.Hair),
        )
    }
}

@Composable
fun FaqCard(
    question: String,
    answer: String,
    highlight: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    val interactions = remember { MutableInteractionSource() }
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.overlaySubtle, RoundedCornerShape(LauncherRadii.Large)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onToggle)
                .padding(LauncherSpacing.Card),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_faqs),
                contentDescription = null,
                tint = colors.primaryFg,
                modifier = Modifier.size(LauncherSizes.CardHeaderIcon),
            )
            Spacer(Modifier.width(LauncherSpacing.MediumLarge))
            LinkText(
                text = question,
                color = colors.secondaryFg,
                fontSize = LauncherType.SectionSize,
                fontWeight = LauncherType.SemiBold,
                highlight = highlight,
                detectLinks = false,
                onOpenLink = onOpenLink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(LauncherSpacing.MediumLarge))
            Icon(
                painter = painterResource(R.drawable.ic_chevron_down),
                contentDescription = null,
                tint = colors.secondaryFgDim,
                modifier = Modifier
                    .size(LauncherSizes.CardHeaderIcon)
                    .graphicsLayer { rotationZ = if (expanded) EXPANDED_ROTATION else 0f },
            )
        }
        if (expanded) {
            Box(
                Modifier
                    .padding(horizontal = LauncherSpacing.Card)
                    .fillMaxWidth()
                    .height(LauncherBorders.Thin)
                    .background(colors.overlayLight),
            )
            LinkText(
                text = answer,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.BodySize,
                highlight = highlight,
                onOpenLink = onOpenLink,
                modifier = Modifier.padding(
                    start = LauncherSpacing.Card,
                    top = LauncherSpacing.Large,
                    end = LauncherSpacing.Card,
                    bottom = LauncherSpacing.Card,
                ),
            )
        }
    }
}

private fun NotificationType.stripeColor() = when (this) {
    NotificationType.INFO -> FixedColors.NotificationInfo
    NotificationType.WARNING -> FixedColors.NotificationWarning
    NotificationType.EXCLAMATION -> FixedColors.NotificationExclamation
}
