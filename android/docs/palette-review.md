# Palette contrast review

Historical measurements and design decisions retained from the Android port
review. These ratios were recorded over the desktop palette values, with alpha
colors composited over their painted surfaces. Moving this document does not
constitute a new accessibility audit.

| Pairing | ≥ 4.5:1 | ≥ 3:1 | Worst |
| --- | --- | --- | --- |
| Primary text on any of the three backgrounds | 10 | 10 | 6.05 (Cefireon, on a card) |
| Dim text on the window background | 4 | 9 | 2.97 (Cefireon) |
| Dim text on a card | 6 | 8 | 2.41 (Cefireon) |
| The accent on the window background | 2 | 6 | 1.68 (Cefireon) |
| The accent on a card | 2 | 7 | 1.08 (Cefireon) |
| Primary text **on** an accent button | 3 | 5 | 1.65 (Auretoskos) |
| Primary text **on** an accent button, hovered | 4 | 7 | 1.00 (Cefireon) |
| Primary text **on** an accent button, pressed | 3 | 4 | 1.19 (Infernape) |
| Primary text **on** the success button | 4 | 10 | 3.79 (Torterra) |

## Retained findings and decisions

- Primary text was readable across the recorded backgrounds; accent links and
  labels, text on accent buttons, dim metadata and success-button text had the
  weaker combinations shown above.
- Cefireon's hover foreground matches its button text, producing the recorded
  1.00:1 combination. Changing a shared palette requires a separate design decision.
- The three light themes give inputs the same background as their page.
  `Modifier.inputSurface` adds an `OverlayStrong` outline in that case without
  changing the palette.
- The port review accepted hover/pressed feedback and left palette changes for
  a deliberate design change across both applications.
- `FixedColors` are explicit exceptions for semantic indicators and effects,
  not a reason to introduce arbitrary literal colors in components.

## Sources and scope

Current colors are in the desktop theme dictionaries and Android
`ui/theme/Palettes.kt`. `PalettesTest` compares against the independent
`DesktopPalettes` fixture and checks the matching light-theme backgrounds.
`SplashThemesTest` checks startup resources.

Those tests guard values; they do not prove legibility, touch-target adequacy or
accessibility compliance. Recompute affected ratios and run visual acceptance
when changing palettes, foreground use or compositing.
