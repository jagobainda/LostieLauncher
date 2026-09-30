# Localization and themes

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before adding any user-visible text or
touching the theme.

Everything here is subordinate to the desktop code that holds the actual
values: `Content/Strings.cs` and `Content/Faqs.cs` for text, `Themes/` for
colour, and the XAML under `Views/` and `Styles/` for everything else.

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
| The 95 keys | `content/Strings.kt` — the `Strings` interface |
| The eight implementations | `content/strings/<Lang>Strings.kt`, one `object` each |
| Resolving one | `stringsFor(language)`, exhaustive over `AppLanguage` |
| The 6 FAQ entries × 8 | `content/Faqs.kt`, resolved with `faqsFor(language)` |
| Placeholder substitution | `String.withArgs(vararg)` in `content/WithArgs.kt` |
| Reading text in a composable | `LocalStrings.current` |

What binds:

- **No user-visible literal outside the catalogue**, in a composable or a
  ViewModel. The desktop has exactly three deliberate exceptions — the product
  name, the `Saved Games` tooltip and the pre-startup fatal-error box — and the
  only one that survives the port is the product name `"Lostie Launcher"`.
  The title bar's social tooltips are brand names too and live on
  `ExternalLink.brandName`, not in a composable.
- **A new string means the key and all eight translations.** English left in
  the other seven is a review rejection. It is also a compile error: `Strings`
  is an interface with 95 abstract properties and eight `object`s implementing
  it, which is exactly the desktop's guarantee and the reason the catalogue is
  900 lines of `override val` rather than a map.
- **Placeholders keep the same count and order in all eight languages.** Five
  keys carry one (the uninstall messages), they use the desktop's `{0}` syntax, and the arguments are
  positional — a translation that swapped `{0}` and `{1}` would put a filesystem
  path where a game name belongs, in one language only. `StringsTest` asserts
  no drift across all 95 × 8.
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

### Thirty keys the desktop has and this side does not

`SettingsStartWithWindows`, `SettingsStartMinimized`, `TrayOpen` and `TrayExit`
are gone from all eight languages. Android has no user-controlled autostart, no
tray, and no concept of an application running with no UI at the user's
request. Dropping a
setting means dropping its Settings row, its key in **all eight** languages and
its persisted field — half-removing one is the failure mode to avoid, and
`StringsTest` asserts none of the four came back.

The other 26 went at the end of the port, with the dialogs and Settings rows
that only exist for Windows reasons (see
[architecture.md](architecture.md#dialogs)): launcher self-update
(`UpToDate*`, `UpdateCheckFailed*`, `UpdateCheckBusy*`, `UpdateAvailable*`,
`SettingsCheckForUpdates`), the download folder (`ChangeDownloadDir*`,
`DownloadDirNotUsable*`, the three `DownloadDirStep*`, `SettingsDownloadDir`,
`BtnBrowse`, `SettingsGamesStoredIn`), OneDrive (`OneDriveWarning*`,
`SettingsOneDriveWarning`) and the four `ExitWarning*`. `StringsTest` asserts
all thirty stay gone; `FaqsTest` keeps the desktop's download-directory labels
as test data, to prove no FAQ answer names that setting.

### Seven keys this side has and the desktop does not

`StatusNotSupportedYet` ("Not available on Android yet") is the text the game
card shows for anything that goes through the unimplemented install and launch
seam (`docs/game-runtime-options.md`): the Library card's
`INSTALLATION_UNSUPPORTED` line and the tooltip of every marked My Games
action. `NotSupportedYetMessage` is the body of the message box that answers a
tap on one of those actions, with `StatusNotSupportedYet` as its title, and
the pair is also My Games' state while installed games cannot be known. It
names every game action, opening a game's folders included. The
desktop has no such state, so it has no such keys. Both go when the seam is
implemented, in all eight languages at once.

`LocationNoHandlerTitle` and `LocationNoHandlerMessage` answer
`OpenGameLocationResult.NoHandler`: Android can have no app that opens a
folder, which Explorer never lacks.

`BtnClose` and `FaqsClearSearch` name the two icon-only controls the desktop
leaves unnamed (the dialog ✕ and the FAQ clear button), for TalkBack.
`StatusWaitingForConnection` is the Library card's line while a transfer waits
for the network, a state the desktop does not have because it fails the
download instead. `StringsTest` asserts all seven by name.

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
  four in `FixedColors` are the deliberate exceptions — the amber warning, the
  close button's hover red, the notification severity stripes and the skeleton
  shimmer — each fixed so it reads the same in every theme.
- Adding a colour role means adding it to **all ten** palettes. Unlike the
  desktop — where a key missing from one dictionary is a runtime binding failure
  nobody sees until that theme is selected — here it is a compile error, because
  `LauncherColors` is a data class. Keep it that way.
- **`Tokens.kt` holds the desktop's values under this port's names.** Colour is
  the only axis the desktop tokenizes; type sizes, spacings, radii, border
  widths and durations are inline at each use site in its XAML. Naming them is
  allowed. Changing one is not, and a value that appears there but nowhere in
  the XAML is a bug.
- **Two desktop spacing values have no token**, because nothing on this side
  uses them: **7** (`SettingsView.xaml:71`, the vertical half of `10,7`) and
  **1** (`ScrollViewerStyle.xaml:13`, the scrollbar thumb's `1,2` margin). The
  7 belongs only to the download-directory path box, which step 14 dropped with
  its row, and the 1 only to the scrollbar, which step 12 did not port (see
  [architecture.md](architecture.md#components)). A step that brings either
  back adds the token to `LauncherSpacing` rather than inlining the number.
- Sizes are `dp` — WPF's device-independent pixel is 1/96 inch, the same as a
  `dp` — and **font sizes are `sp`**, which is the one deliberate divergence:
  Android scales text by the user's accessibility setting and a launcher that
  ignores it is broken in a way the desktop cannot be.
- `PalettesTest` is the parity guard: it compares all 140 values against
  `DesktopPalettes`, a table of the desktop's own hex strings, which it parses
  itself so a bug in the production conversion cannot hide behind it.

## Switching either one

Both settings live in `SettingsStore` (`service/settings/`) and reach the UI
through its narrow `AppearanceStore` parent and `SettingsViewModel`, which
resolves the palette's theme and the catalogue and exposes them as one state.
`MainActivity` provides them and does
**not** react to a change: both are ordinary state, so a change recomposes and
the activity is never recreated and never loses screen state. That is the
behaviour requirement, and it is why neither uses a resource qualifier.

There is one source of truth and it is the store. A selection is written, comes
back out of `AppearanceStore.appearance`, and only then reaches the screen —
nothing holds a second copy, and **nothing caches a resolved string in a field.**
That last rule is what the hot switch rests on, on the desktop and identically
here.

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
build type — debug renders the shell with a debug-tools action that opens the
token catalogue, the component catalogue and the download harness, release
renders the shell alone — and `MainActivity` calls it without knowing which. A file added to one build type's
source set must be added to the other, or the release build stops compiling.

## Contrast review list

Measured across all ten palettes, over the surfaces each colour actually paints,
compositing the alpha ones first. **No palette value has been changed**, and
none may be changed on this side alone. The end-of-port review decided each
row; the decisions are in [the parity report](../docs/parity-report.md#polish-done-in-this-step)
and summed up below the table. They are listed because the desktop values are
kept as they are, and this is what that costs on a phone, where the screen is
smaller, the viewing distance shorter and the ambient light unpredictable.

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

What was decided:

- **The light themes' inputs** are the one row fixed on Android, without
  touching a value: `Modifier.inputSurface` (`ui/theme/InputSurface.kt`) draws a
  1 dp `OverlayStrong` outline (4.3:1 against the page) wherever
  `TertiaryBg` equals `PrimaryBg`. Every input uses it — the FAQ search bar,
  the key fields, the download path box and the combo box.
- **Hover and pressed states** are accepted: touch shows hover only for a
  pointer, and a pressed colour lasts as long as the press.
- **The accent as text, text on an accent button, the dim foreground and
  `SuccessColor`** are the palettes themselves and are left for a design change
  made on both sides at once.
