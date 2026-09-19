package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Everything in the visual language that is not a colour.
 *
 * Read `spec/06-design-tokens.md` before touching any of this, and read this
 * paragraph before assuming a name here means something on the desktop: colour
 * is the only axis the desktop tokenizes. Type sizes, spacings, radii, border
 * widths and durations are written inline at each use site in
 * `desktop/LostieLauncher/Views/` and `Styles/`, so **the values below are the
 * desktop's and the names are this port's**. Naming them is allowed; changing
 * one is not, and a value that appears here but nowhere in the XAML is a bug.
 *
 * They are plain objects rather than a theme-scoped record because none of them
 * varies by theme — a theme swaps fourteen colours and nothing else. There is
 * therefore nothing to provide through a composition local and nothing to
 * recompose when the theme changes.
 *
 * Sizes are `.dp`, type sizes are `.sp`. WPF's device-independent pixel is
 * 1/96 inch, which is exactly what a `dp` is, so the numbers carry over
 * unchanged. Font sizes are `sp` rather than `dp` on purpose: that is the one
 * place the port deliberately diverges, because Android scales text by the
 * user's accessibility setting and a launcher that ignores it is broken in a
 * way the desktop cannot be.
 */
object LauncherType {
    /** The launcher name in the welcome dialog. */
    val DisplaySize = 20.sp

    /** The Home column headers. */
    val TitleSize = 18.sp

    /** A game card title; the game title in the download dialog. */
    val HeadingSize = 16.sp

    /** A news card title. */
    val SubheadingSize = 15.sp

    /** A FAQ question; a banner title; a settings section header. */
    val SectionSize = 14.sp

    /** The window title; card titles; message-box and settings body text. */
    val BodySize = 13.sp

    /** Card metadata; dialog bodies; combo boxes; link buttons. */
    val CaptionSize = 12.sp

    /** The news card tag; progress readouts; dates. */
    val LabelSize = 11.sp

    /** The special-version badge on a game card. */
    val BadgeSize = 10.sp

    /**
     * The only three weights in the application. The desktop sets no font
     * family anywhere and inherits Segoe UI from Windows; the port inherits the
     * platform default the same way and ships no face of its own.
     */
    val Normal = FontWeight.Normal
    val SemiBold = FontWeight.SemiBold
    val Bold = FontWeight.Bold

    /**
     * The welcome dialog's description, at [CaptionSize], is the one place a
     * line height is set explicitly. Nothing else overrides it.
     */
    val WelcomeDescriptionLineHeight = 18.sp

    /** The download dialog's description stops growing here and scrolls. */
    val DownloadDescriptionMaxHeight: Dp = 100.dp
}

/**
 * The spacing scale. There is no generative rule behind it — it is not a
 * 4-point grid — so it is a list of observed values rather than a formula.
 *
 * It is the list `spec/06-design-tokens.md` records, and that list is
 * **incomplete**: the desktop's XAML also uses three values it does not
 * mention, found by reading the views directly.
 *
 * | Value | Where | Belongs to |
 * | --- | --- | --- |
 * | 5 | `GameCardComponent.xaml:132`, `DownloadConfirmDialog.xaml:64` (padding), `MainWindow.xaml:144` (margin) | steps 12, 13, 11 |
 * | 7 | `SettingsView.xaml:71`, as the vertical half of `10,7` | step 14 |
 * | 1 | `ScrollViewerStyle.xaml:13`, the scrollbar thumb's margin `1,2` | step 12 |
 *
 * They are deliberately not tokens yet: none has a use site on this side until
 * the step that ports its component arrives, and a token nothing reads is a
 * value nobody can check. **The step that ports one of those three adds its
 * token here** rather than inlining the number, and the gap in `spec/06` is
 * worth fixing in its own change — the specification is documentation, and a
 * mismatch with the desktop is a bug in it.
 */
object LauncherSpacing {
    /** Progress-bar internals. */
    val Hair: Dp = 2.dp

    /** The scrollbar thumb's inset. */
    val Micro: Dp = 3.dp

    /** Icon to label in card metadata. */
    val ExtraSmall: Dp = 4.dp

    /** Between adjacent card buttons; icon to label inside a button. */
    val Small: Dp = 6.dp

    /** Between a header and a card; between a path box and its browse button. */
    val Medium: Dp = 8.dp

    /** Settings row icon to label; between stacked cards. */
    val MediumLarge: Dp = 10.dp

    /** Icon to text in a banner; icon to label in the search bar. */
    val Large: Dp = 12.dp

    /** Notification card padding; banner padding; settings row vertical padding. */
    val ExtraLarge: Dp = 14.dp

    /** Game, news and FAQ card padding; settings row horizontal padding. */
    val Card: Dp = 16.dp

    /** Screen content padding; dialog horizontal padding; the Home column gap. */
    val Screen: Dp = 20.dp

    /** Between the two settings sections; the welcome dialog's side padding. */
    val Section: Dp = 24.dp

    /** Above the key section in the download dialog. */
    val Block: Dp = 30.dp
}

/**
 * Fixed structural sizes. Each is a chrome dimension the desktop states once;
 * several describe a window, and `spec/10-windows-only.md` is where the port
 * decides which of those still mean anything on a phone.
 */
object LauncherSizes {
    /** The shell's inset inside the window border. */
    val ShellInset: Dp = 15.dp

    /** The title bar, and the dialog title bar, which matches it. */
    val TitleBarHeight: Dp = 50.dp

    /** The navigation rail. */
    val NavigationRailWidth: Dp = 90.dp

    /** One navigation item, square. */
    val NavigationItemSize: Dp = 60.dp

    /** A dialog's footer. */
    val DialogFooterHeight: Dp = 60.dp

    /** The scrollbar, and its thumb. */
    val ScrollbarWidth: Dp = 8.dp

    /** The uninstalling spinner's circle. */
    val SpinnerSize: Dp = 13.dp

    /** The uninstalling spinner's stroke. */
    val SpinnerStrokeWidth: Dp = 2.dp
}

/**
 * Corner radii. Like the spacing scale these are observed rather than derived,
 * and the eight of them are the complete set.
 */
object LauncherRadii {
    /** The progress bar's track and fill; the notification stripe. */
    val Hair: Dp = 2.dp

    /** Combo box items; skeleton text blocks; the scrollbar thumb. */
    val Micro: Dp = 3.dp

    /** Buttons; tags; text inputs; badges; pills; the scrollbar trough. */
    val Small: Dp = 4.dp

    /** Game cards; the logo well; settings groups; the search bar; banners. */
    val Medium: Dp = 6.dp

    /** News, notification and FAQ cards. */
    val Large: Dp = 8.dp

    /** The toggle switch thumb. */
    val ToggleThumb: Dp = 9.dp

    /** The toggle switch track. */
    val ToggleTrack: Dp = 12.dp

    /** The welcome dialog's icon tile. */
    val ExtraLarge: Dp = 16.dp
}

/**
 * Border widths. There are three borders in the whole application, and the list
 * below is all of them; a divider is not a border but a 1 dp rectangle filled
 * with [LauncherColors.overlayLight].
 */
object LauncherBorders {
    /** The window and dialog border, in the accent; the amber pill and banners. */
    val Thin: Dp = 1.dp

    /** The download pulse overlay. */
    val Thick: Dp = 2.dp
}

/**
 * Elevation. The combo box popup's shadow is the only one in the application,
 * which is why this holds a single value and no scale.
 */
object LauncherElevation {
    val PopupShadow: Dp = 8.dp
    const val POPUP_SHADOW_OPACITY = 0.3f
    val PopupShadowOffset: Dp = 2.dp
}

/**
 * The four animations, and that is the complete list — everything else in the
 * launcher, including changing screen and expanding a FAQ, is instantaneous.
 * A transition added on Android that the desktop does not have is a divergence,
 * not a polish.
 */
object LauncherMotion {
    /** The skeleton shimmer sweep, repeating forever, linear. */
    val Shimmer: Duration = 1_200.milliseconds

    /** One turn of the uninstalling spinner, repeating while visible, linear. */
    val SpinnerTurn: Duration = 900.milliseconds

    /** One leg of the download pulse; it auto-reverses. */
    val Pulse: Duration = 800.milliseconds

    /**
     * How long the pulse runs in total when a card enters the downloading
     * state. It stops there; it does not run for the whole download, and its
     * border returns to fully transparent.
     */
    val PulseTotal: Duration = 3_000.milliseconds

    /** The pulse border's opacity, from and to. */
    const val PULSE_MIN_ALPHA = 0.3f
    const val PULSE_MAX_ALPHA = 1.0f
}

/**
 * The colours that deliberately do not follow the theme.
 *
 * Four literals, each for its own reason, listed in `spec/06-design-tokens.md`.
 * Naming them is fine; re-tinting them per theme is not — the whole point of
 * all four is that they stay put across all ten palettes.
 */
object FixedColors {
    /** The offline pill and both home banners: border and icon. */
    val Warning = Color(0xFFFFC107)

    /**
     * The Windows system close-button hover, and the only colour here that may
     * not survive the port: it goes with the title bar if Android has no close
     * button, which is a `spec/10-windows-only.md` question. It is *not* the
     * notification stripe's red — that is [NotificationExclamation], a
     * different value — so removing this one leaves the others alone.
     */
    val WindowsClose = Color(0xFFE81123)

    /** A notification card's left stripe, by severity. Semantic, not themed. */
    val NotificationInfo = Color(0xFF4CAF50)
    val NotificationWarning = Color(0xFFFFC107)
    val NotificationExclamation = Color(0xFFF44336)

    /**
     * The skeleton shimmer gradient, semi-transparent white at every stop so it
     * layers acceptably over both dark and light card backgrounds.
     */
    val ShimmerEdge = Color(0x22FFFFFF)
    val ShimmerPeak = Color(0x55FFFFFF)
}
