# Localization and themes

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before adding any user-visible text or
touching the theme.

Everything here is subordinate to
[`spec/05-localization.md`](../../spec/05-localization.md) and
[`spec/06-design-tokens.md`](../../spec/06-design-tokens.md), which carry the
actual values, and both are subordinate to the desktop code they were extracted
from.

Both areas work the same way on the desktop and here: a fixed set of parallel
implementations that has to stay complete, switchable at runtime, with the
choice persisted. Half-done work here is the most common reason a PR gets sent
back on the other side.

## Localization — 8 languages, no exceptions

The catalogue is in **`content/`, in Kotlin** — not in `res/values-xx/`. The
launcher switches language in-place, without restarting, and Android's resource
qualifiers follow the system locale rather than an in-app setting.
`app/src/main/res/values/strings.xml` therefore holds only what the platform
itself reads before any Kotlin runs, and says so.

| Thing | Where |
| --- | --- |
| The 114 keys | `content/Strings.kt` — the `Strings` interface |
| The eight implementations | `content/strings/<Lang>Strings.kt`, one `object` each |
| Resolving one | `stringsFor(language)`, exhaustive over `AppLanguage` |
| The 6 FAQ entries × 8 | `content/Faqs.kt`, resolved with `faqsFor(language)` |
| Placeholder substitution | `String.withArgs(vararg)` in `content/WithArgs.kt` |
| Reading text in a composable | `LocalStrings.current` |

What binds:

- **No user-visible literal outside the catalogue**, in a composable or a
  ViewModel. The desktop has exactly three deliberate exceptions and
  [`spec/10-windows-only.md`](../../spec/10-windows-only.md) §15 lists them; the
  only one that survives the port is the product name `"Lostie Launcher"`.
- **A new string means the key and all eight translations.** English left in
  the other seven is a review rejection. It is also a compile error: `Strings`
  is an interface with 114 abstract properties and eight `object`s implementing
  it, which is exactly the desktop's guarantee and the reason the catalogue is
  900 lines of `override val` rather than a map.
- **Placeholders keep the same count and order in all eight languages.** Eight
  keys carry one, they use the desktop's `{0}` syntax, and the arguments are
  positional — a translation that swapped `{0}` and `{1}` would put a filesystem
  path where a game name belongs, in one language only. `StringsTest` asserts
  no drift across all 114 × 8.
- **Substitution is `String.withArgs`, and the name is load-bearing.**
  `kotlin.text` declares `String.format(vararg Any?)` and is default-imported,
  so an extension called `format` here would not shadow it — it would *lose* to
  it in any file that forgot the explicit import, silently returning the
  template with `{0}` still in it. Neither of the obvious alternatives works
  either: `java.lang.String.format` needs the catalogue rewritten to `%1$s`, and
  `MessageFormat` reads `{0}` but also assigns meaning to a single quote, of
  which the catalogue is full (`s'han`, `l'aplicació`). `WithArgsResolutionTest`
  lives outside `content/` for exactly this reason.
- **The language display names are not localized and are not in the catalogue.**
  They are the endonyms on `AppLanguage.displayName`, from the desktop's
  `[Description]` attributes. The Settings picker shows all eight at once, so a
  reader has to find their own in a language they cannot read.
- **The wire codes are not the display names and not the member names.** Spanish
  is `es`, Valencian is `val`. They resolve the remote home content and nothing
  else.

### Four keys the desktop has and this side does not

`SettingsStartWithWindows`, `SettingsStartMinimized`, `TrayOpen` and `TrayExit`
are gone from all eight languages. `spec/10-windows-only.md` entries 2 and 3 and
its closing table: Android has no user-controlled autostart, no tray, and no
concept of an application running with no UI at the user's request. Dropping a
setting means dropping its Settings row, its key in **all eight** languages and
its persisted field — half-removing one is the failure mode to avoid, and
`StringsTest` asserts none of the four came back.

That is the whole list. Every other key was ported, including ones whose screen
does not exist yet.

## Themes — 10 palettes, identical key sets

| Thing | Where |
| --- | --- |
| The 14 colour roles | `ui/theme/LauncherColors.kt` |
| The ten palettes | `ui/theme/Palettes.kt`, resolved with `paletteFor(theme)` |
| Light or dark | `LauncherColors.isDark()`, derived from the overlay hue |
| Everything that is not colour | `ui/theme/Tokens.kt` |
| Providing them | `LostieLauncherTheme(theme) { … }` |
| Reading a colour | `LocalLauncherColors.current` |

What binds:

- **All ten palettes, exact values, no unification and no "improvements."** If a
  colour fails contrast on Android it goes on the list below; it does not get
  changed on the way past.
- **Never inline a colour.** A literal will not follow the active theme. The
  four in `FixedColors` are the deliberate exceptions and
  `spec/06-design-tokens.md` says why each one does not move.
- Adding a colour role means adding it to **all ten** palettes. Unlike the
  desktop — where a key missing from one dictionary is a runtime binding failure
  nobody sees until that theme is selected — here it is a compile error, because
  `LauncherColors` is a data class. Keep it that way.
- **`Tokens.kt` holds the desktop's values under this port's names.** Colour is
  the only axis the desktop tokenizes; type sizes, spacings, radii, border
  widths and durations are inline at each use site in its XAML. Naming them is
  allowed. Changing one is not, and a value that appears there but nowhere in
  the XAML is a bug.
- Sizes are `dp` — WPF's device-independent pixel is 1/96 inch, the same as a
  `dp` — and **font sizes are `sp`**, which is the one deliberate divergence:
  Android scales text by the user's accessibility setting and a launcher that
  ignores it is broken in a way the desktop cannot be.
- `PalettesTest` is the parity guard: it compares all 140 values against
  `DesktopPalettes`, a table of the desktop's own hex strings, which it parses
  itself so a bug in the production conversion cannot hide behind it.

## Switching either one

Both settings live in `SettingsStore` (`service/settings/`) and reach the UI
through its narrow `AppearanceStore` parent and `AppearanceViewModel`, which
resolves the palette's theme and the catalogue and exposes them as one state. `MainActivity` provides them and does
**not** react to a change: both are ordinary state, so a change recomposes and
the activity is never recreated and never loses screen state. That is the
behaviour requirement, and it is why neither uses a resource qualifier.

There is one source of truth and it is the store. A selection is written, comes
back out of `AppearanceStore.appearance`, and only then reaches the screen —
nothing holds a second copy, and **nothing caches a resolved string in a field.**
That last rule is what the hot switch rests on; `spec/05-localization.md` states
it for the desktop and it holds identically here.

Persistence is a Preferences DataStore holding the **enum member name**, not the
desktop's ordinal. A name survives a member being inserted into the middle of
the enum; an ordinal does not. An unrecognised value falls back to Volcarona and
Spanish, which is the desktop's own behaviour.

`SettingsStore` also owns welcome state and debounces persistence writes for
500 ms. Appearance changes remain immediately observable while rapid changes
are coalesced into one DataStore transaction.

## The token catalogue

`ui/screen/TokenCatalogScreen.kt` shows every colour in the active palette with
its name and hex, the type scale, the spacing scale, the radii, the borders, the
four durations and a sample of the text catalogue, with live theme and language
pickers. It is the fastest way to see whether a change to any of this did what
you meant.

It is in **`src/debug/`**, not behind a `BuildConfig.DEBUG` branch, so it is not
compiled into a release APK at all. `StartSurface` has one implementation per
build type — debug shows the catalogue, release shows `EmptyScreen` — and
`MainActivity` calls it without knowing which. A file added to one build type's
source set must be added to the other, or the release build stops compiling.

## Contrast review list

Measured across all ten palettes, over the surfaces each colour actually paints,
compositing the alpha ones first. **None of these has been changed and none of
them may be changed in a port step** — `spec/06-design-tokens.md` says to
collect them and port plan step 15 owns resolving them. They are listed here
rather than in the specification because the specification records what the
desktop is, and this is what that costs on a phone, where the screen is smaller,
the viewing distance shorter and the ambient light unpredictable.

WCAG 2.1 AA wants 4.5:1 for body text and 3:1 for large text and UI boundaries.
The counts below are out of ten palettes.

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

Reading it:

- **Body text is fine everywhere.** `SecondaryFgColor` clears 4.5:1 against all
  three backgrounds in all ten palettes. Nothing below is about ordinary copy.
- **The accent is not a text colour.** It paints links, section headers, tags
  and the active navigation item, and it reaches 4.5:1 in two palettes. This is
  the widest of the problems, because it affects every theme rather than a few.
- **`SecondaryFgColor` on an accent button** is the other systematic one: the
  desktop has no dedicated "on accent" colour and reuses primary text there.
  Cefireon's hover is the extreme — `PrimaryFgHoverColor` **equals** its own
  text colour, so hovering a primary button gives a dark slab with invisible
  text on it at exactly 1.00:1. The pressed states of Infernape, Mewtwo,
  Torterra, Astrem and Empoleon are all under 2:1.
- **The dim foreground**, at 53 % or 60 % alpha, is under 4.5:1 over the window
  background in six palettes. It is used for 11 px and 12 px metadata, which is
  where it matters most.
- **`SuccessColor`** clears 3:1 everywhere and 4.5:1 in four — consistently
  close and consistently short.
- **The three light themes** define `TertiaryBgColor` equal to `PrimaryBgColor`,
  so text inputs and the search bar have no visual separation from the page
  behind them. That is legibility rather than contrast, and it is on the same
  list.

Cefireon appears in almost every row and is the one palette worth looking at as
a whole rather than value by value.

`PalettesTest` pins the last of those as a test, so a later step "tidying" one of
them has to do it deliberately rather than by accident.
