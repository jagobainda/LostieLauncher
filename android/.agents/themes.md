# Android themes

Apply the [shared theme rules](../../.agents/localization-and-themes.md).

- `ui/theme/LauncherColors.kt` defines the color roles; `Palettes.kt` supplies
  every palette and `paletteFor(theme)` resolves one.
- `LostieLauncherTheme` provides colors through `LocalLauncherColors.current`.
  Theme changes recompose the existing activity.
- Add a role to every palette. A complete data-class constructor is the compile
  guarantee; `PalettesTest` compares palette values against `DesktopPalettes`.
- Use `Tokens.kt` for spacing, size, radii, borders, elevation, opacity and motion.
  Reuse desktop values where appropriate and name intentional mobile adaptations.
  WPF DIPs and Android dp are different units; preserving a numeric value is a
  design choice, not physical-size equivalence.
- `FixedColors` in `Tokens.kt` holds deliberate fixed colors such as warning,
  close-button, live indicator, notification and shimmer colors. Reuse those
  names instead of copying literals into components.
- Splash colors are repeated in `res/values/colors.xml` and theme-specific
  splash styles; `SplashThemesTest` checks palette agreement. A new theme needs
  those resources, its `AppTheme` entry and `splashThemeFor` mapping.
- `Modifier.inputSurface` outlines inputs when a light palette otherwise makes
  them disappear into the background. Preserve that distinction.
- Existing contrast findings and decisions are recorded in
  [palette-review.md](../docs/palette-review.md). A documentation move is not
  a new accessibility audit.
