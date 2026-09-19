package dev.jagoba.lostielauncher.ui.theme

import androidx.compose.ui.graphics.Color
import dev.jagoba.lostielauncher.model.AppTheme

/**
 * The ten palettes, one per [AppTheme] member.
 *
 * Generated from the ten resource dictionaries in
 * `desktop/LostieLauncher/Themes/` and identical to them, value for value. WPF reads a six-digit `#rrggbb` as fully opaque and an
 * eight-digit `#aarrggbb` in that order, which is Compose's `0xAARRGGBB`, so
 * the conversion only ever fills in a missing `FF`. `PalettesTest` holds the
 * desktop's own values in a table and compares all 140 of them.
 *
 * Do not adjust a colour here. `spec/06-design-tokens.md` carries a contrast
 * review list — Cefireon's hover colour equalling its own text colour is the
 * conspicuous one — and resolving those is port plan step 15's, not a
 * correction to make in passing.
 */
fun paletteFor(theme: AppTheme): LauncherColors = when (theme) {
    AppTheme.Volcarona -> VolcaronaColors
    AppTheme.Zoroark -> ZoroarkColors
    AppTheme.Infernape -> InfernapeColors
    AppTheme.Torterra -> TorterraColors
    AppTheme.Empoleon -> EmpoleonColors
    AppTheme.Mewtwo -> MewtwoColors
    AppTheme.Cefireon -> CefireonColors
    AppTheme.Sylveon -> SylveonColors
    AppTheme.Astrem -> AstremColors
    AppTheme.Auretoskos -> AuretoskosColors
}

/**
 * Whether this palette composites its overlays over a dark surface.
 *
 * The desktop stores this nowhere — it is implied by whether the five overlay
 * colours are white or black. The port needs it explicitly, for the system bar
 * icons and for anything that has to pick a light or dark asset, so it is
 * derived from the one thing that actually distinguishes the two: the overlay
 * hue. Reading it rather than listing it means a new palette cannot disagree
 * with itself.
 */
fun LauncherColors.isDark(): Boolean = overlaySubtle.red > 0.5f

/** Volcarona — dark. */
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

/** Zoroark — dark. */
val ZoroarkColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF2C2933),
    secondaryBg = Color(0xFF211E2A),
    tertiaryBg = Color(0xFF181620),
    primaryFg = Color(0xFFC42D3F),
    primaryFgHover = Color(0xFFA82435),
    primaryFgPressed = Color(0xFF8C1C2A),
    secondaryFg = Color(0xFFF5EDE0),
    secondaryFgDim = Color(0x88F5EDE0),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)

/** Infernape — dark. */
val InfernapeColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF4A3535),
    secondaryBg = Color(0xFF3A2828),
    tertiaryBg = Color(0xFF2E1F1F),
    primaryFg = Color(0xFFCF4F51),
    primaryFgHover = Color(0xFFB83A3C),
    primaryFgPressed = Color(0xFFF2D848),
    secondaryFg = Color(0xFFF5E8D8),
    secondaryFgDim = Color(0x99F5E8D8),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)

/** Torterra — dark. */
val TorterraColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF3A3020),
    secondaryBg = Color(0xFF2C2418),
    tertiaryBg = Color(0xFF221C12),
    primaryFg = Color(0xFF5A9E42),
    primaryFgHover = Color(0xFF447A30),
    primaryFgPressed = Color(0xFF8BC34A),
    secondaryFg = Color(0xFFE8DCC8),
    secondaryFgDim = Color(0x99E8DCC8),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)

/** Empoleon — dark. */
val EmpoleonColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF252E38),
    secondaryBg = Color(0xFF1C242D),
    tertiaryBg = Color(0xFF141B22),
    primaryFg = Color(0xFF3A82C4),
    primaryFgHover = Color(0xFF2A68A0),
    primaryFgPressed = Color(0xFF6AB0E8),
    secondaryFg = Color(0xFFDCE8F0),
    secondaryFgDim = Color(0x99DCE8F0),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)

/** Mewtwo — dark. */
val MewtwoColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF342E4A),
    secondaryBg = Color(0xFF272238),
    tertiaryBg = Color(0xFF1E1A2C),
    primaryFg = Color(0xFFC060F0),
    primaryFgHover = Color(0xFF9040CC),
    primaryFgPressed = Color(0xFF40E0D0),
    secondaryFg = Color(0xFFEDE8FF),
    secondaryFgDim = Color(0x99EDE8FF),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)

/** Cefireon — light. */
val CefireonColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFFFFEFD3),
    secondaryBg = Color(0xFFE8A772),
    tertiaryBg = Color(0xFFFFEFD3),
    primaryFg = Color(0xFF3ECFCF),
    primaryFgHover = Color(0xFF343434),
    primaryFgPressed = Color(0xFFF5E653),
    secondaryFg = Color(0xFF343434),
    secondaryFgDim = Color(0x88343434),
    success = Color(0xFF78C878),
    overlaySubtle = Color(0x1A000000),
    overlayLight = Color(0x22000000),
    overlayMuted = Color(0x33000000),
    overlayMedium = Color(0x55000000),
    overlayStrong = Color(0x88000000),
)

/** Sylveon — light. */
val SylveonColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFFFDE8F0),
    secondaryBg = Color(0xFFF0B0CC),
    tertiaryBg = Color(0xFFFDE8F0),
    primaryFg = Color(0xFF5AAAD8),
    primaryFgHover = Color(0xFF3888B8),
    primaryFgPressed = Color(0xFF1868A0),
    secondaryFg = Color(0xFF3A2030),
    secondaryFgDim = Color(0x883A2030),
    success = Color(0xFF3AAA6A),
    overlaySubtle = Color(0x1A000000),
    overlayLight = Color(0x22000000),
    overlayMuted = Color(0x33000000),
    overlayMedium = Color(0x55000000),
    overlayStrong = Color(0x88000000),
)

/** Astrem — light. */
val AstremColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFFEEF3FF),
    secondaryBg = Color(0xFFAEC4EC),
    tertiaryBg = Color(0xFFEEF3FF),
    primaryFg = Color(0xFF2255CC),
    primaryFgHover = Color(0xFF1844AA),
    primaryFgPressed = Color(0xFF0F3388),
    secondaryFg = Color(0xFF141820),
    secondaryFgDim = Color(0x88141820),
    success = Color(0xFF3AAA6A),
    overlaySubtle = Color(0x1A000000),
    overlayLight = Color(0x22000000),
    overlayMuted = Color(0x33000000),
    overlayMedium = Color(0x55000000),
    overlayStrong = Color(0x88000000),
)

/** Auretoskos — dark. */
val AuretoskosColors: LauncherColors = LauncherColors(
    primaryBg = Color(0xFF3B3838),
    secondaryBg = Color(0xFF2B2929),
    tertiaryBg = Color(0xFF1E1C1C),
    primaryFg = Color(0xFFF0B020),
    primaryFgHover = Color(0xFFCC9010),
    primaryFgPressed = Color(0xFFA87008),
    secondaryFg = Color(0xFFF0EDED),
    secondaryFgDim = Color(0x88F0EDED),
    success = Color(0xFF2E7D32),
    overlaySubtle = Color(0x1AFFFFFF),
    overlayLight = Color(0x22FFFFFF),
    overlayMuted = Color(0x33FFFFFF),
    overlayMedium = Color(0x55FFFFFF),
    overlayStrong = Color(0x88FFFFFF),
)
