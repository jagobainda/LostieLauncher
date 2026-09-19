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
 * The ten palettes that fill it live in `Palettes.kt`; adding a role here is a
 * compile error until every one of them supplies it, which is the whole reason
 * this is a `data class` and not a map.
 *
 * Extracted from `desktop/LostieLauncher/Themes/`, described in
 * `spec/06-design-tokens.md`.
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
