package dev.jagoba.lostielauncher.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

@Composable
internal fun ContentStatusBanner(isOffline: Boolean, isOutOfDate: Boolean, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    when {
        isOffline -> StatusBannerCard(
            icon = R.drawable.ic_offline,
            title = strings.offlineModeLabel,
            message = strings.serverMaintenanceNotificationMessage,
            modifier = modifier,
        )

        isOutOfDate -> StatusBannerCard(
            icon = R.drawable.ic_home_cloud_alert,
            title = strings.contentOutOfDateLabel,
            message = strings.contentOutOfDateMessage,
            modifier = modifier,
        )
    }
}

@Composable
private fun StatusBannerCard(@DrawableRes icon: Int, title: String, message: String, modifier: Modifier = Modifier) {
    val colors = LocalLauncherColors.current
    val shape = RoundedCornerShape(LauncherRadii.Medium)
    Row(
        modifier
            .padding(bottom = LauncherSpacing.Card)
            .fillMaxWidth()
            .background(colors.overlaySubtle, shape)
            .border(LauncherBorders.Thin, FixedColors.Warning, shape)
            .padding(LauncherSpacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = FixedColors.Warning,
            modifier = Modifier.size(LauncherSizes.HomeBannerIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.Large))
        Column {
            Text(
                text = title,
                color = colors.secondaryFg,
                fontSize = LauncherType.SectionSize,
                fontWeight = LauncherType.SemiBold,
            )
            Text(
                text = message,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier.padding(top = LauncherSpacing.ExtraSmall),
            )
        }
    }
}
