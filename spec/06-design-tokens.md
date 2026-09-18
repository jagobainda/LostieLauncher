# 06 · Design tokens — colour, type, space, shape and motion

Extracted from `desktop/LostieLauncher/Themes/` (10 files),
`desktop/LostieLauncher/Styles/` (4 files), `App.xaml`, and every view and
component under `desktop/LostieLauncher/Views/`.

Colour is fully tokenized. Everything else — type, spacing, radii, borders,
durations — is **not** tokenized on desktop: the values are written inline at
each use site. They are collected here as a token set because the Android port
needs one, but be aware that the desktop has no names for them, so the names
below are introduced by this specification.

## Colour

### The 28 keys

Each theme is one resource dictionary defining **14 colours and 14 brushes**,
one brush per colour, same name with `Color` replaced by `Brush`. Every theme
defines the identical key set; a key missing from one theme is not a compile
error on desktop, only a runtime binding failure when that theme is selected,
which is why the sets are checked rather than trusted.

| Colour key | Brush key | What it paints |
| --- | --- | --- |
| `PrimaryBgColor` | `PrimaryBgBrush` | the window background |
| `SecondaryBgColor` | `SecondaryBgBrush` | the navigation rail, cards, settings groups, the welcome footer |
| `TertiaryBgColor` | `TertiaryBgBrush` | inset surfaces: logo wells, text inputs, the search bar, combo box faces |
| `PrimaryFgColor` | `PrimaryFgBrush` | the accent: active nav item and its bar, primary buttons, progress fill, tags, links, section headers, the window border |
| `PrimaryFgHoverColor` | `PrimaryFgHoverBrush` | accent hover |
| `PrimaryFgPressedColor` | `PrimaryFgPressedBrush` | accent pressed |
| `SecondaryFgColor` | `SecondaryFgBrush` | primary text, and the foreground **on** accent buttons |
| `SecondaryFgDimColor` | `SecondaryFgDimBrush` | secondary text, inactive nav items, icons, metadata |
| `SuccessColor` | `SuccessBrush` | the update button and the "downloaded" check |
| `OverlaySubtleColor` | `OverlaySubtleBrush` | news, notification and FAQ card backgrounds; the active nav item's background; banner backgrounds; the scrollbar trough |
| `OverlayLightColor` | `OverlayLightBrush` | nav hover, dividers, progress track, secondary-button pressed, the OneDrive warning box |
| `OverlayMutedColor` | `OverlayMutedBrush` | card secondary buttons, the toggle track when off, settings buttons, combo item selected |
| `OverlayMediumColor` | `OverlayMediumBrush` | title-bar button hover, secondary-button base, the logo placeholder icon |
| `OverlayStrongColor` | `OverlayStrongBrush` | the scrollbar thumb, secondary-button hover |

Two conventions worth stating because they are load-bearing:

- **`SecondaryFgBrush` is the foreground on an accent button.** Not a dedicated
  "on-accent" colour. In a light theme with a light accent this is exactly where
  contrast can fail; see the contrast review list below.
- **Overlays are alpha, not opaque.** The first two hex digits are the alpha
  channel and the rest is either white or black depending on whether the theme
  is dark or light. They are meant to composite over whatever is beneath them.

### Overlay ramp

The five overlay steps are the same alpha ramp in every theme:

| Key | Alpha | Dark themes | Light themes |
| --- | --- | --- | --- |
| `OverlaySubtle` | `1A` (10 %) | `#1AFFFFFF` | `#1A000000` |
| `OverlayLight` | `22` (13 %) | `#22FFFFFF` | `#22000000` |
| `OverlayMuted` | `33` (20 %) | `#33FFFFFF` | `#33000000` |
| `OverlayMedium` | `55` (33 %) | `#55FFFFFF` | `#55000000` |
| `OverlayStrong` | `88` (53 %) | `#88FFFFFF` | `#88000000` |

`SecondaryFgDimColor` follows the same idea: it is `SecondaryFgColor` with an
alpha prefix, `88` (53 %) in six themes and `99` (60 %) in four — Empoleon,
Infernape, Torterra and Mewtwo use `99`. It is **not** a separate hue.

### Light and dark

Seven themes are dark and three are light. The distinction is not stored
anywhere; it is implied by whether the overlays composite white or black.

| | Themes |
| --- | --- |
| Dark | Volcarona, Zoroark, Infernape, Torterra, Empoleon, Mewtwo, Auretoskos |
| Light | Cefireon, Sylveon, Astrem |

In the three light themes `PrimaryBgColor` and `TertiaryBgColor` are the **same
value**, so the inset surfaces read flat against the window and only
`SecondaryBgColor` provides separation.

### Applying a theme

On desktop the active dictionary is swapped in the merged dictionary set at
runtime: the new one is loaded from `Themes/<AppTheme>.xaml`, the old one
removed, the new one added. Every consumer binds dynamically, so the whole UI
repaints without a reload. The file name and the enum member name must stay in
sync because the URI is built from the enum.

A theme that fails to load falls back to `Volcarona` and logs. If that fails
too, the previous theme stays.

The Android requirement is the behaviour, not the mechanism: **switching theme
must repaint the running UI without recreating it or losing screen state, and
must persist across runs.**

### Per-theme palettes

#### Volcarona  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#4d4949` |
|  | `SecondaryBgColor` | `#3a3737` |
|  | `TertiaryBgColor` | `#2e2c2c` |
| Accent | `PrimaryFgColor` | `#f08058` |
|  | `PrimaryFgHoverColor` | `#d06038` |
|  | `PrimaryFgPressedColor` | `#b04020` |
| Text | `SecondaryFgColor` | `#f3f7fa` |
|  | `SecondaryFgDimColor` | `#88f3f7fa` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Zoroark  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#2c2933` |
|  | `SecondaryBgColor` | `#211e2a` |
|  | `TertiaryBgColor` | `#181620` |
| Accent | `PrimaryFgColor` | `#c42d3f` |
|  | `PrimaryFgHoverColor` | `#a82435` |
|  | `PrimaryFgPressedColor` | `#8c1c2a` |
| Text | `SecondaryFgColor` | `#f5ede0` |
|  | `SecondaryFgDimColor` | `#88f5ede0` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Infernape  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#4a3535` |
|  | `SecondaryBgColor` | `#3a2828` |
|  | `TertiaryBgColor` | `#2e1f1f` |
| Accent | `PrimaryFgColor` | `#cf4f51` |
|  | `PrimaryFgHoverColor` | `#b83a3c` |
|  | `PrimaryFgPressedColor` | `#f2d848` |
| Text | `SecondaryFgColor` | `#f5e8d8` |
|  | `SecondaryFgDimColor` | `#99f5e8d8` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Torterra  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#3a3020` |
|  | `SecondaryBgColor` | `#2c2418` |
|  | `TertiaryBgColor` | `#221c12` |
| Accent | `PrimaryFgColor` | `#5a9e42` |
|  | `PrimaryFgHoverColor` | `#447a30` |
|  | `PrimaryFgPressedColor` | `#8bc34a` |
| Text | `SecondaryFgColor` | `#e8dcc8` |
|  | `SecondaryFgDimColor` | `#99e8dcc8` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Empoleon  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#252e38` |
|  | `SecondaryBgColor` | `#1c242d` |
|  | `TertiaryBgColor` | `#141b22` |
| Accent | `PrimaryFgColor` | `#3a82c4` |
|  | `PrimaryFgHoverColor` | `#2a68a0` |
|  | `PrimaryFgPressedColor` | `#6ab0e8` |
| Text | `SecondaryFgColor` | `#dce8f0` |
|  | `SecondaryFgDimColor` | `#99dce8f0` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Mewtwo  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#342e4a` |
|  | `SecondaryBgColor` | `#272238` |
|  | `TertiaryBgColor` | `#1e1a2c` |
| Accent | `PrimaryFgColor` | `#c060f0` |
|  | `PrimaryFgHoverColor` | `#9040cc` |
|  | `PrimaryFgPressedColor` | `#40e0d0` |
| Text | `SecondaryFgColor` | `#ede8ff` |
|  | `SecondaryFgDimColor` | `#99ede8ff` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

#### Cefireon  (light)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#ffefd3` |
|  | `SecondaryBgColor` | `#e8a772` |
|  | `TertiaryBgColor` | `#ffefd3` |
| Accent | `PrimaryFgColor` | `#3ecfcf` |
|  | `PrimaryFgHoverColor` | `#343434` |
|  | `PrimaryFgPressedColor` | `#f5e653` |
| Text | `SecondaryFgColor` | `#343434` |
|  | `SecondaryFgDimColor` | `#88343434` |
| Semantic | `SuccessColor` | `#78c878` |
| Overlays | `OverlaySubtleColor` | `#1A000000` |
|  | `OverlayLightColor` | `#22000000` |
|  | `OverlayMutedColor` | `#33000000` |
|  | `OverlayMediumColor` | `#55000000` |
|  | `OverlayStrongColor` | `#88000000` |

#### Sylveon  (light)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#fde8f0` |
|  | `SecondaryBgColor` | `#f0b0cc` |
|  | `TertiaryBgColor` | `#fde8f0` |
| Accent | `PrimaryFgColor` | `#5aaad8` |
|  | `PrimaryFgHoverColor` | `#3888b8` |
|  | `PrimaryFgPressedColor` | `#1868a0` |
| Text | `SecondaryFgColor` | `#3a2030` |
|  | `SecondaryFgDimColor` | `#883a2030` |
| Semantic | `SuccessColor` | `#3aaa6a` |
| Overlays | `OverlaySubtleColor` | `#1A000000` |
|  | `OverlayLightColor` | `#22000000` |
|  | `OverlayMutedColor` | `#33000000` |
|  | `OverlayMediumColor` | `#55000000` |
|  | `OverlayStrongColor` | `#88000000` |

#### Astrem  (light)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#eef3ff` |
|  | `SecondaryBgColor` | `#aec4ec` |
|  | `TertiaryBgColor` | `#eef3ff` |
| Accent | `PrimaryFgColor` | `#2255cc` |
|  | `PrimaryFgHoverColor` | `#1844aa` |
|  | `PrimaryFgPressedColor` | `#0f3388` |
| Text | `SecondaryFgColor` | `#141820` |
|  | `SecondaryFgDimColor` | `#88141820` |
| Semantic | `SuccessColor` | `#3aaa6a` |
| Overlays | `OverlaySubtleColor` | `#1A000000` |
|  | `OverlayLightColor` | `#22000000` |
|  | `OverlayMutedColor` | `#33000000` |
|  | `OverlayMediumColor` | `#55000000` |
|  | `OverlayStrongColor` | `#88000000` |

#### Auretoskos  (dark)

| Group | Colour key | Value |
| --- | --- | --- |
| Surfaces | `PrimaryBgColor` | `#3b3838` |
|  | `SecondaryBgColor` | `#2b2929` |
|  | `TertiaryBgColor` | `#1e1c1c` |
| Accent | `PrimaryFgColor` | `#f0b020` |
|  | `PrimaryFgHoverColor` | `#cc9010` |
|  | `PrimaryFgPressedColor` | `#a87008` |
| Text | `SecondaryFgColor` | `#f0eded` |
|  | `SecondaryFgDimColor` | `#88f0eded` |
| Semantic | `SuccessColor` | `#2e7d32` |
| Overlays | `OverlaySubtleColor` | `#1AFFFFFF` |
|  | `OverlayLightColor` | `#22FFFFFF` |
|  | `OverlayMutedColor` | `#33FFFFFF` |
|  | `OverlayMediumColor` | `#55FFFFFF` |
|  | `OverlayStrongColor` | `#88FFFFFF` |

### The same data as one table

| Colour key | Volcarona | Zoroark | Infernape | Torterra | Empoleon | Mewtwo | Cefireon | Sylveon | Astrem | Auretoskos |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `PrimaryBgColor` | `#4d4949` | `#2c2933` | `#4a3535` | `#3a3020` | `#252e38` | `#342e4a` | `#ffefd3` | `#fde8f0` | `#eef3ff` | `#3b3838` |
| `SecondaryBgColor` | `#3a3737` | `#211e2a` | `#3a2828` | `#2c2418` | `#1c242d` | `#272238` | `#e8a772` | `#f0b0cc` | `#aec4ec` | `#2b2929` |
| `TertiaryBgColor` | `#2e2c2c` | `#181620` | `#2e1f1f` | `#221c12` | `#141b22` | `#1e1a2c` | `#ffefd3` | `#fde8f0` | `#eef3ff` | `#1e1c1c` |
| `PrimaryFgColor` | `#f08058` | `#c42d3f` | `#cf4f51` | `#5a9e42` | `#3a82c4` | `#c060f0` | `#3ecfcf` | `#5aaad8` | `#2255cc` | `#f0b020` |
| `PrimaryFgHoverColor` | `#d06038` | `#a82435` | `#b83a3c` | `#447a30` | `#2a68a0` | `#9040cc` | `#343434` | `#3888b8` | `#1844aa` | `#cc9010` |
| `PrimaryFgPressedColor` | `#b04020` | `#8c1c2a` | `#f2d848` | `#8bc34a` | `#6ab0e8` | `#40e0d0` | `#f5e653` | `#1868a0` | `#0f3388` | `#a87008` |
| `SecondaryFgColor` | `#f3f7fa` | `#f5ede0` | `#f5e8d8` | `#e8dcc8` | `#dce8f0` | `#ede8ff` | `#343434` | `#3a2030` | `#141820` | `#f0eded` |
| `SecondaryFgDimColor` | `#88f3f7fa` | `#88f5ede0` | `#99f5e8d8` | `#99e8dcc8` | `#99dce8f0` | `#99ede8ff` | `#88343434` | `#883a2030` | `#88141820` | `#88f0eded` |
| `SuccessColor` | `#2e7d32` | `#2e7d32` | `#2e7d32` | `#2e7d32` | `#2e7d32` | `#2e7d32` | `#78c878` | `#3aaa6a` | `#3aaa6a` | `#2e7d32` |
| `OverlaySubtleColor` | `#1AFFFFFF` | `#1AFFFFFF` | `#1AFFFFFF` | `#1AFFFFFF` | `#1AFFFFFF` | `#1AFFFFFF` | `#1A000000` | `#1A000000` | `#1A000000` | `#1AFFFFFF` |
| `OverlayLightColor` | `#22FFFFFF` | `#22FFFFFF` | `#22FFFFFF` | `#22FFFFFF` | `#22FFFFFF` | `#22FFFFFF` | `#22000000` | `#22000000` | `#22000000` | `#22FFFFFF` |
| `OverlayMutedColor` | `#33FFFFFF` | `#33FFFFFF` | `#33FFFFFF` | `#33FFFFFF` | `#33FFFFFF` | `#33FFFFFF` | `#33000000` | `#33000000` | `#33000000` | `#33FFFFFF` |
| `OverlayMediumColor` | `#55FFFFFF` | `#55FFFFFF` | `#55FFFFFF` | `#55FFFFFF` | `#55FFFFFF` | `#55FFFFFF` | `#55000000` | `#55000000` | `#55000000` | `#55FFFFFF` |
| `OverlayStrongColor` | `#88FFFFFF` | `#88FFFFFF` | `#88FFFFFF` | `#88FFFFFF` | `#88FFFFFF` | `#88FFFFFF` | `#88000000` | `#88000000` | `#88000000` | `#88FFFFFF` |


### Colours that are not theme keys

Four literals appear in the UI and do not follow the theme. They are recorded
here so the port reproduces them deliberately rather than by accident.

| Literal | Where | Why |
| --- | --- | --- |
| `#FFFFC107` (amber) | the offline pill's border and icon; both home banners' borders and icons | a warning colour that must stay recognisable in all ten themes |
| `#E81123` (red) | the close button's hover background | the Windows system close-button colour |
| `#4CAF50` / `#FFC107` / `#F44336` | the notification card's left stripe, for `Info` / `Warning` / `Exclamation` | semantic severity, deliberately theme-independent |
| `#22FFFFFF` → `#55FFFFFF` → `#22FFFFFF` | the skeleton shimmer gradient | semi-transparent white, chosen to layer acceptably over both dark and light card backgrounds |

Adding these as tokens on Android is fine. Re-tinting them per theme is not:
the whole point of all four is that they do not move.

### Contrast review list

Step 05 is instructed not to "improve" any palette, and to collect
contrast concerns for a review at the end instead. The following are the ones
visible from the values alone; none has been changed.

- **Cefireon** pairs `PrimaryFgColor` `#3ecfcf` (a light cyan) with
  `SecondaryFgColor` `#343434` as the on-accent foreground — the only theme
  whose accent is lighter than its own primary text. Its
  `PrimaryFgHoverColor` `#343434` is also the same value as its text colour, so
  hovering a primary button turns it into a dark slab with dark text on it.
- **Cefireon**'s `PrimaryFgPressedColor` `#f5e653` (pale yellow) against
  `SecondaryFgColor` `#343434` is the same concern in the pressed state.
- **Astrem** and **Sylveon**, being light themes, use `SecondaryFgColor` as the
  label on an accent button: `#141820` on `#2255cc`, and `#3a2030` on `#5aaad8`.
  The second is the thinner of the two.
- All three light themes define `TertiaryBgColor` equal to `PrimaryBgColor`, so
  text inputs and the search bar have **no** visual separation from the page
  behind them. That is a legibility question rather than a contrast one.
- The dim foreground at 53 % alpha over a mid-tone surface is close to the
  smallest usable contrast for 11 px and 12 px metadata text, which is where it
  is mostly used.

## Typography

One family throughout: **the platform default**. No font is ever set, in any
view, style or theme — the desktop inherits Segoe UI from the system. The
Android port should use its own platform default rather than shipping a face.

The type scale, by observed size and weight:

| Size | Weight | Used for |
| --- | --- | --- |
| 20 | Bold | the launcher name in the welcome dialog |
| 18 | SemiBold | the Home column headers |
| 16 | Bold | a game card title; the game title in the download dialog |
| 15 | SemiBold | a news card title |
| 14 | SemiBold | a FAQ question; a banner title; a settings section header |
| 14 | normal | screen-level empty-state messages |
| 13 | SemiBold | the window title; a notification card title; the message-box title |
| 13 | normal | settings row labels; the search box and its placeholder; message-box body; news card description; FAQ answer; per-column empty-state messages |
| 12 | SemiBold | a game card's update version; the offline pill body is 11 SemiBold |
| 12 | normal | card metadata; settings secondary text; dialog bodies; combo boxes; the key input; link buttons |
| 11 | SemiBold | the news card tag; the offline pill label |
| 11 | normal | progress percentage, speed, remaining time, dates, the uninstalling label |
| 10 | Bold | the special-version badge on a game card |

Weights are only ever `normal`, `SemiBold` and `Bold`. The welcome dialog's
description sets an explicit **line height of 18** at 12 px; nothing else
overrides line height.

Text truncation: game titles, the settings games-root path and the download
dialog's path ellipsise at the end. Descriptions, answers, banner bodies and
message-box bodies wrap. The download dialog's description also caps at a
maximum height of 100.

## Spacing

The scale in actual use, in device-independent pixels:

`2 · 3 · 4 · 6 · 8 · 10 · 12 · 14 · 16 · 20 · 24 · 30`

| Value | Typical use |
| --- | --- |
| 4 | gap between an icon and its label in card metadata |
| 6 | gap between adjacent card buttons; icon-to-label inside a button |
| 8 | gap between a header and a card; between the path box and its browse button |
| 10 | gap between a settings row icon and its label; between stacked cards (as a bottom margin) |
| 12 | icon-to-text in a banner; icon-to-label in the search bar |
| 14 | notification card padding; banner padding; settings row vertical padding |
| 16 | game card padding; news and FAQ card padding; settings row horizontal padding; the title bar's left inset |
| 20 | screen content padding; dialog horizontal padding; the gap between the two Home columns |
| 24 | gap between the two settings sections; the welcome dialog's horizontal padding |
| 30 | the gap above the key section in the download dialog |

Structural sizes: the shell inset is **15**, the title bar is **50** tall, the
navigation rail is **90** wide with **60 × 60** items, dialog footers are **60**
tall, and the dialog title bar matches the window's at **50**.

## Shape

| Radius | Applied to |
| --- | --- |
| 2 | the progress bar's track and fill; the notification stripe |
| 3 | combo box items; skeleton text blocks |
| 4 | buttons; the tag pill; text inputs; the special-version badge; combo box faces; the offline pill; the OneDrive warning box; the scrollbar trough |
| 6 | game cards; the logo well; settings group cards; the search bar; home banners |
| 8 | news, notification and FAQ cards |
| 9 | the toggle switch thumb |
| 12 | the toggle switch track |
| 16 | the welcome dialog's icon tile |

Borders are almost absent. Only three exist: the **1 px** window and dialog
border in the accent colour, the **1 px** amber border on the offline pill and
the two home banners, and the **2 px** accent border of the download pulse
overlay. Dividers are not borders — they are 1 px `OverlayLightBrush` rectangles.

Elevation is almost absent too. The only shadow in the application is on the
combo box popup: blur radius 8, opacity 0.3, depth 2.

## Motion

Four animations, and that is the complete list.

| Animation | Duration | Repeat | Easing |
| --- | --- | --- | --- |
| Skeleton shimmer | 1.2 s | forever | linear |
| Uninstalling spinner | 0.9 s per turn | forever while visible | linear |
| Download pulse border | 0.8 s | auto-reversing, for 3 s total | sine, ease-in-out |
| Combo box popup | the platform slide | once | platform |

Details:

- **Shimmer** animates a linear gradient's start point from `-1,0` to `1,0` and
  its end point from `0,0` to `2,0`, so a highlight band sweeps left to right
  across the placeholder.
- **Spinner** rotates an ellipse 0 → 360°, drawn as a 13 px circle with a 2 px
  dashed stroke (`13 7`, round caps) in the dim foreground, so it reads as an
  arc chasing its tail. It starts when it becomes visible and stops when it
  hides.
- **Pulse** fades a 2 px accent border overlay from 0.3 to 1 and back, repeating
  for three seconds when a card enters the downloading state and then stopping.
  It does not run for the whole download. Its fill behaviour is `Stop`, so the
  border returns to fully transparent afterwards.
- The FAQ chevron rotation between 0° and 180° is a **state change, not an
  animation** — there is no transition on it. Everything else in the UI
  (screen changes, expanding a FAQ, showing a banner) is instantaneous.

## Scrollbars

Custom-templated and used everywhere: **8 px wide**, trough
`OverlaySubtleBrush` at radius 4, thumb `OverlayStrongBrush` at radius 3 with a
`1,2` margin, hovering to the accent and turning to accent-hover while dragging.
The page and line buttons exist but are fully transparent and do nothing
visually. Only a vertical bar is templated; there is no horizontal scrolling
anywhere in the application.

