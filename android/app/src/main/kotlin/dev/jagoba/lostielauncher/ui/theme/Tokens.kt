package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object LauncherType {
    val DisplaySize = 20.sp
    val TitleSize = 18.sp
    val HeadingSize = 16.sp
    val SubheadingSize = 15.sp
    val SectionSize = 14.sp
    val BodySize = 13.sp
    val CaptionSize = 12.sp
    val LabelSize = 11.sp
    val BadgeSize = 10.sp
    val Normal = FontWeight.Normal
    val SemiBold = FontWeight.SemiBold
    val Bold = FontWeight.Bold
    val WelcomeDescriptionLineHeight = 18.sp
    val DownloadDescriptionMaxHeight: Dp = 100.dp
}

object LauncherSpacing {
    val Hair: Dp = 2.dp
    val Micro: Dp = 3.dp
    val ExtraSmall: Dp = 4.dp
    val Snug: Dp = 5.dp
    val Small: Dp = 6.dp
    val Medium: Dp = 8.dp
    val MediumLarge: Dp = 10.dp
    val Large: Dp = 12.dp
    val ExtraLarge: Dp = 14.dp
    val Card: Dp = 16.dp
    val Screen: Dp = 20.dp
    val Section: Dp = 24.dp
    val Block: Dp = 30.dp
}

object LauncherSizes {
    val ShellInset: Dp = 15.dp
    val TitleBarHeight: Dp = 50.dp
    val NavigationRailWidth: Dp = 90.dp
    val NavigationItemSize: Dp = 60.dp
    val DialogFooterHeight: Dp = 60.dp
    val ScrollbarWidth: Dp = 8.dp
    val SpinnerSize: Dp = 13.dp
    val SpinnerStrokeWidth: Dp = 2.dp
    val TitleBarIcon: Dp = 22.dp
    val TitleBarButton: Dp = 50.dp
    val TitleBarButtonIcon: Dp = 14.dp
    val NavigationIcon: Dp = 22.dp
    val NavigationIndicator: Dp = 3.dp
    val OfflinePillHeight: Dp = 22.dp
    val OfflinePillIcon: Dp = 12.dp
    val NavigationRailBreakpoint: Dp = 600.dp
    val MinimumTouchTarget: Dp = 48.dp
}

object LauncherRadii {
    val Hair: Dp = 2.dp
    val Micro: Dp = 3.dp
    val Small: Dp = 4.dp
    val Medium: Dp = 6.dp
    val Large: Dp = 8.dp
    val ToggleThumb: Dp = 9.dp
    val ToggleTrack: Dp = 12.dp
    val ExtraLarge: Dp = 16.dp
}

object LauncherBorders {
    val Thin: Dp = 1.dp
    val Thick: Dp = 2.dp
}

object LauncherElevation {
    val PopupShadow: Dp = 8.dp
    const val POPUP_SHADOW_OPACITY = 0.3f
    val PopupShadowOffset: Dp = 2.dp
}

object LauncherOpacity {
    const val NAVIGATION_ACTION_DISABLED = 0.3f
}

object LauncherMotion {
    val Shimmer: Duration = 1_200.milliseconds
    val SpinnerTurn: Duration = 900.milliseconds
    val Pulse: Duration = 800.milliseconds
    val PulseTotal: Duration = 3_000.milliseconds
    const val PULSE_MIN_ALPHA = 0.3f
    const val PULSE_MAX_ALPHA = 1.0f
}

object FixedColors {
    val Warning = Color(0xFFFFC107)
    val WindowsClose = Color(0xFFE81123)
    val NotificationInfo = Color(0xFF4CAF50)
    val NotificationWarning = Color(0xFFFFC107)
    val NotificationExclamation = Color(0xFFF44336)
    val ShimmerEdge = Color(0x22FFFFFF)
    val ShimmerPeak = Color(0x55FFFFFF)
}
