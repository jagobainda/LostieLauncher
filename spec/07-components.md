# 07 · Components — anatomy and every state

Extracted from `desktop/LostieLauncher/Views/Components/` and
`desktop/LostieLauncher/Styles/`.

Four content cards, three loading skeletons, and the shared button and input
styles they are built from. Every measurement here is from the XAML; the tokens
they refer to are in section 06.

---

## Shared button styles

Seven button shapes exist across the application. They are listed once here
because the cards and dialogs reuse them.

| Style | Height | Background | Hover | Pressed | Text |
| --- | --- | --- | --- | --- | --- |
| Accent (dialogs) | 34 | `PrimaryFgBrush` | `PrimaryFgHoverBrush` | `PrimaryFgPressedBrush` | 13 SemiBold |
| Accent (cards) | 32, min-width 110, padding `14,0` | `PrimaryFgBrush` | `PrimaryFgHoverBrush` | `PrimaryFgPressedBrush` | 12 SemiBold |
| Success (cards) | as accent | `SuccessBrush` | `SuccessBrush` at 0.85 opacity | `SuccessBrush` at 0.7 opacity | 12 SemiBold |
| Secondary (dialogs) | 34 | `OverlayMediumBrush` | `OverlayStrongBrush` | `OverlayMutedBrush` | 13 |
| Secondary (cards) | 32, min-width 32, padding `10,0` | `OverlayMutedBrush` | `OverlayMediumBrush` | `OverlayLightBrush` | 12 |
| Settings | 32, padding `12,0` | `OverlayMutedBrush` | `OverlayMediumBrush` | `OverlayLightBrush` | 12 |
| Link | auto | transparent | foreground → `PrimaryFgHoverBrush` | — | 12, accent, underlined |

All of them are radius 4, borderless, and show a hand cursor.

Disabled states are not uniform, and the difference is deliberate:

- Card accent and success buttons drop to **0.5 opacity** when disabled.
- The "Downloaded" button is a card secondary button that is permanently
  disabled at **0.6 opacity** — it is a status chip shaped like a button, never
  clickable.
- Navigation rail action buttons drop to **0.3 opacity**.
- Secondary buttons have no disabled visual of their own; they only ever appear
  disabled through the `IsUpdating` guard, which uses the base style.

Note that the foreground of an accent or success button is
`SecondaryFgBrush` — the same brush as body text. See the contrast list in
section 06.

---

## Game card

The largest component by far, and the only one with two modes and seven states.
`GameCardComponent.xaml` plus **31 dependency properties** — 30 settable, and
one read-only `DownloadArgs` that the component recomputes itself whenever the
game id, the version or the relative path changes, so the download and resume
buttons always pass a consistent argument set.

### Outer geometry

A container with a **10 px bottom margin**, holding a `SecondaryBgBrush` surface
at radius 6 with **16 px padding**, and — on top of it, not inside it — a
transparent 2 px accent border used only for the download pulse.

Three columns: logo (auto), text (star), actions (auto).

### Logo well

**110 × 80**, radius 6, `TertiaryBgBrush`, clipped, 5 px padding, vertically
centred. It holds two mutually exclusive children: a **Pokéball icon at 32 × 32
in `OverlayMediumBrush`** when there is no logo, and the image scaled uniformly
with high-quality bitmap scaling when there is.

The logo is resolved from an HTTPS URL. A non-HTTPS or unparseable URL yields no
image, so the placeholder shows. There is no loading state and no error state
for the image itself — it appears when it has loaded.

### Text column

Inset 16 px on both sides, vertically centred.

**Title** — 16 px Bold in `SecondaryFgBrush`, ellipsised.

**Metadata row, Library mode only** — 6 px below the title, a horizontal run of
icon + text pairs, every text 12 px in `SecondaryFgDimBrush`, every icon 12 × 12
in the same brush, 4 px between an icon and its text:

| Icon | Text | Shown |
| --- | --- | --- |
| Harddisk | the formatted size, e.g. `588 MB` | always, 14 px to the next pair |
| TagOutline | the catalogue version | always |
| ClockOutline | the playtime text | only when non-empty, 14 px to its left |

**Metadata block, Games mode only** — 6 px below the title, two stacked rows.

Row one: TagOutline plus the installed version at 12 px dim; then, 8 px right,
a **special-version badge** when `tipo` is non-empty — an accent-filled pill at
radius 3 with `6,2` padding whose label is 10 px Bold in
**`SecondaryBgBrush`** (the card's own background colour, so the badge reads as
a cut-out); then, when an update exists, an 8 px gap, a 10 × 10 accent
ArrowRight, another 8 px, and the update version at 12 px SemiBold **in the
accent colour**.

Row two, 4 px below: ClockOutline plus the playtime text, only when non-empty.

**Progress block** — 8 px below, visible in exactly four states:
`Downloading`, `Paused`, `Extracting`, `VerifyingIntegrity`. It is:

- a **4 px tall** progress bar, track `OverlayLightBrush` at radius 2, fill
  `PrimaryFgBrush` at radius 2, left-aligned, 0–100;
- 3 px below, a row with three possible left-aligned labels and one
  right-aligned one, all 11 px in `SecondaryFgDimBrush`:

| Label | Shown when |
| --- | --- |
| `{percent:0}%` | not extracting and not verifying — so it stays visible while paused |
| the remaining-time text, e.g. `· 5m 3s` | downloading only |
| `StatusExtracting` | extracting |
| `StatusVerifying` | verifying |
| the speed text, right-aligned | downloading only |

The percentage is therefore visible while paused, showing where the transfer
stopped, while speed and remaining time disappear.

### Action column — Library mode

Exactly one of five layouts, chosen by the download status.

| Status | Layout |
| --- | --- |
| `Available` | one accent button: Download icon 13 px + `BtnDownload` |
| `Downloading` | accent `BtnPause` with a Pause icon, 6 px gap, then a secondary icon-only button with a Close icon, tooltip `BtnCancel` |
| `Paused` | accent `BtnResume` with a Play icon, 6 px gap, then the same cancel button |
| `Downloaded` | a permanently disabled secondary button: a 13 px Check icon in **`SuccessBrush`** + `BtnDownloaded` |
| `UpdateAvailable` | one success button: Update icon + `BtnUpdate` |
| `Extracting`, `VerifyingIntegrity` | none of the above match, so the action area is empty — the progress block carries the state |

The download and resume buttons pass the card's download arguments; pause and
cancel pass the game id.

### Action column — Games mode

Two mutually exclusive rows.

**Normal row**, right-aligned, buttons 6 px apart:

| Button | Style | Content | Visible when | Disabled when |
| --- | --- | --- | --- | --- |
| Play | card accent | Play icon + `BtnPlay` | always | updating, or (has update **and** not a special version) |
| Update | card success | Update icon + `BtnUpdate` | `HasUpdate` | — |
| Help | card secondary, icon only | HelpCircleOutline 14 px | `HasHelpFolder` | — |
| Special version | card secondary, icon only | KeyVariant 14 px | always | updating |
| Open folder | card secondary, icon only | FolderOpen 14 px | always | updating |
| Uninstall | card secondary, icon only | TrashCanOutline 14 px | always | — |

Icon-only buttons carry tooltips: `TooltipOpenHelp`,
`TooltipSwitchSpecialVersion`, `TooltipOpenFolder`, `TooltipUninstall`. All six
pass the game **title** as their parameter, not the id.

The uninstall button is deliberately never disabled while updating — the user
can always reach it.

**Uninstalling row**, shown instead of the whole normal row while
`IsUninstalling`: the 13 px dashed spinner described in section 06, 6 px gap,
then `StatusUninstalling` at 11 px in the dim foreground.

### Download pulse

When the status becomes `Downloading`, the transparent 2 px accent border over
the card fades 0.3 → 1 → 0.3 on a sine ease over 0.8 s, auto-reversing, for
**three seconds total**, then stops and returns to transparent. It is a
one-shot attention cue at the start of a transfer, not a persistent indicator.
It never intercepts input.

### The seven states, end to end

| State | Progress block | Action column (Library) | Card border |
| --- | --- | --- | --- |
| `Available` | hidden | Download | — |
| `Downloading` | bar + percent + remaining + speed | Pause, Cancel | pulses for 3 s on entry |
| `Paused` | bar + percent | Resume, Cancel | — |
| `VerifyingIntegrity` | bar at 100 + `StatusVerifying` | empty | — |
| `Extracting` | bar at 100 + `StatusExtracting` | empty | — |
| `Downloaded` | hidden | disabled "Downloaded" | — |
| `UpdateAvailable` | hidden | Update | — |

---

## News card

`OverlaySubtleBrush`, radius 8, **16 px padding**, 10 px bottom margin. Three
stacked rows.

1. **Header** — an accent pill at radius 4 with `8,2` padding holding the tag at
   11 px SemiBold in `SecondaryFgBrush`, docked left; the date right-aligned at
   11 px in the dim foreground, formatted `dd MMM yyyy`.
2. **Title** — 15 px SemiBold in `SecondaryFgBrush`, wrapped, margin `0,10,0,4`.
3. **Description** — 13 px in the dim foreground, wrapped, **rendered as rich
   text**: the string is scanned for links and every match becomes an accent,
   underlined hyperlink that hovers to `PrimaryFgHoverBrush` and carries the
   resolved URL as its tooltip. Clicking opens it, and only an absolute HTTPS
   URI is ever opened.

The link scanner is a pure function; its exact grammar is in section 09.

## Notification card

`OverlaySubtleBrush`, radius 8, **14 px padding**, 10 px bottom margin. Three
columns.

1. **Severity stripe** — 4 px wide, radius 2, stretched to the card's full
   height, 12 px to its right. Its colour is a literal keyed off the type:
   `Info` → `#4CAF50`, `Warning` → `#FFC107`, `Exclamation` → `#F44336`. An
   unrecognised value falls back to `Info`.
2. **Text** — vertically centred: title at 13 px SemiBold in
   `SecondaryFgBrush`, wrapped; message 4 px below at 12 px in the dim
   foreground, wrapped.
3. **Date** — 11 px dim, **top-aligned**, margin `8,2,0,0`, formatted
   `dd MMM` — shorter than the news card's format.

Unlike the news card, the notification message is **plain text**: links in a
notification are not made clickable.

## FAQ card

`OverlaySubtleBrush`, radius 8, 10 px bottom margin, no outer padding — the
header and body carry their own.

**Header** — a transparent, hand-cursored strip with 16 px padding; clicking
anywhere on it toggles expansion. Three columns: a 16 px CircleQuestionMark in
the accent with 10 px to its right; the question at 14 px SemiBold in
`SecondaryFgBrush`, wrapped, vertically centred; a 16 px ChevronDown in the dim
foreground with 10 px to its left, rotated **180°** when expanded, with no
transition.

**Body**, only when expanded — a 1 px `OverlayLightBrush` divider inset 16 px
horizontally, then the answer at 13 px in the dim foreground, wrapped, margin
`16,12,16,16`.

Both question and answer are rich text, and both do two things at once:

- **Search highlighting.** Every match of the current search term is rendered
  **Bold**; in plain text it is additionally underlined and recoloured to the
  accent. Inside a hyperlink it is only bolded, because the link is already
  accent-coloured and underlined.
- **Link detection**, in the answer only, identical to the news card.

Matching ignores case and diacritics, so a search for `anil` highlights `Añil`.
Highlighting is re-rendered whenever the question, the answer or the search term
changes.

Expansion is bound two-way to the underlying item, which is what lets the FAQ
screen force every result open while searching.

---

## Skeletons

Three, one per card type. Each **mirrors the outer geometry of the real card
exactly**, so the layout does not shift when content arrives. That is the rule
worth carrying over: a skeleton is the real card's box with its content replaced
by blocks.

Every placeholder block shares one style: not hit-testable, pixel-snapped, and
filled with a horizontal linear gradient of `#22FFFFFF` → `#55FFFFFF` →
`#22FFFFFF` animated as described in section 06. The semi-transparent white
works over both dark and light card surfaces.

**Game card skeleton** — the same container margin, surface, radius and padding
as the real card. A 110 × 80 block at radius 6 for the logo; in the text column
a 180 × 18 block at radius 3 and, 8 px below, a 120 × 12 block; a 110 × 32 block
at radius 4 for the button.

**News card skeleton** — same surface, radius 8, 16 px padding. Row one: a
60 × 18 block at radius 4 left (the tag pill) and a 60 × 12 block right (the
date). Row two: a 200 × 18 block with margin `0,10,0,4` (the title). Row three:
a full-width 12 px block and, 4 px below, a 220 × 12 block — two lines of
description, the second short.

**Notification card skeleton** — same surface, radius 8, 14 px padding. A 4 px
wide, full-height block at radius 2 (the stripe); a 130 × 14 block and, 4 px
below, a full-width 11 px block; a 30 × 11 top-aligned block for the date.

How many are shown, per screen:

| Screen | Count |
| --- | --- |
| Home, news column | 4 |
| Home, notifications column | 4 |
| My Games | 5 |
| Library | 6 |

---

## Inputs

**Toggle switch** (Settings) — a 44 × 24 track at radius 12 with an 18 × 18
thumb at radius 9. Off: track `OverlayMutedBrush`, thumb `SecondaryFgBrush`
left-aligned with a 3 px inset. On: track `PrimaryFgBrush`, thumb right-aligned
with a 3 px inset. No transition — it snaps.

**Combo box** (Settings, welcome dialog) — minimum width 160, height 34, 12 px
text, hand cursor. The closed face is `TertiaryBgBrush` at radius 4 with the
selected value inset `10,0,30,0` and a small 8 × 4 chevron in the dim foreground
centred in a 30 px column on the right. The popup is `SecondaryBgBrush` at
radius 4 with 4 px padding, 2 px below the face, at least as wide as the face,
carrying the only drop shadow in the application, and animates in with the
platform slide. Items are radius 3 with `8,6` padding: hover
`OverlayLightBrush`, selected `OverlayMutedBrush`.

**Text inputs** — three variants, all borderless with the caret in
`SecondaryFgBrush`:

| Variant | Background | Radius | Padding | Size |
| --- | --- | --- | --- | --- |
| Key box (dialogs) | `TertiaryBgBrush` | 4 | `10,8` | 12 |
| Settings path box | `TertiaryBgBrush` | 4 | `10,7` | 12, read-only |
| FAQ search box | transparent | — | — | 13, inside the search bar |

The FAQ search box is the only one with a placeholder, and it shows only when
the box is both empty and unfocused.
