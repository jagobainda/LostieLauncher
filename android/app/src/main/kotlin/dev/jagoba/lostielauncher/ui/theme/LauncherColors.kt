package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The colour roles a theme has to fill.
 *
 * One property per colour key the desktop themes define — fourteen of them, with
 * the same meaning and the same names. The desktop also declares a brush per
 * colour; Compose has no separate brush concept for a flat fill, so the brushes
 * have no counterpart here.
 *
 * Extracted from `spec/06-design-tokens.md`, which in turn comes from
 * `desktop/LostieLauncher/Themes/`.
 */
@Immutable
data class LauncherColors(
    /** The window background. */
    val primaryBg: Color,
    /** The navigation rail, cards, settings groups, the welcome footer. */
    val secondaryBg: Color,
    /** Inset surfaces: logo wells, text inputs, the search bar, combo box faces. */
    val tertiaryBg: Color,
    /** The accent: active navigation, primary buttons, progress fill, tags, links. */
    val primaryFg: Color,
    /** Accent, hovered. */
    val primaryFgHover: Color,
    /** Accent, pressed. */
    val primaryFgPressed: Color,
    /** Primary text — and the foreground *on* an accent button. */
    val secondaryFg: Color,
    /** Secondary text, inactive navigation items, icons, metadata. */
    val secondaryFgDim: Color,
    /** The update button and the "downloaded" check. */
    val success: Color,
    /** Card and banner backgrounds, the active navigation item, the scrollbar trough. */
    val overlaySubtle: Color,
    /** Navigation hover, dividers, the progress track, secondary button pressed. */
    val overlayLight: Color,
    /** Card secondary buttons, the toggle track when off, settings buttons. */
    val overlayMuted: Color,
    /** Title-bar button hover, secondary button base, the logo placeholder icon. */
    val overlayMedium: Color,
    /** The scrollbar thumb, secondary button hover. */
    val overlayStrong: Color,
)

/**
 * Volcarona — the desktop's default theme, and the one it falls back to when a
 * selected theme fails to load.
 *
 * It is the only palette the port has so far, on purpose: port plan step 05
 * brings the other nine and the rest of the visual token set. The values are
 * the desktop's exactly; `LauncherColorsTest` is what keeps them that way.
 * Overlay colours carry real alpha and are meant to composite over whatever is
 * beneath them.
 */
val VolcaronaColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF4D4949),
    secondaryBg = Color(0xFF3A3737),
    tertiaryBg = Color(0xFF2E2C2C),
    primaryFg = Color(0xFFF08058),
    primaryFgHover = Color(0xFFD06038),
    primaryFgPressed = Color(0xFFB04020),
    secondaryFg = Color(0xFFF3F7FA),
    secondaryFgDim = Color(0x88F3F7FA),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)
