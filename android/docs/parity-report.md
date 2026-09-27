# Android parity report

Where the Android port stands against the desktop launcher, written at the close
of the port (plan step 15, 2026-09-26, on top of `d51cd05`). It is meant to be
read months from now without the port's history: what works, what is partial,
what waits on a decision, and what was deliberately left behind, with the reason
for each.

The desktop code under [`../../desktop/`](../../desktop/) is the authority on
behavior, [`../../spec/`](../../spec/) is its written contract, and the
per-area deviations live in the three tables of
[`../.agents/architecture.md`](../.agents/architecture.md) (cards, dialogs,
screens). This document summarises and does not repeat them.

## At a glance

| State | What it covers |
| --- | --- |
| **Ported and working** | Catalogue, home news and notifications, resumable downloads, special-version keys, maintenance flag and offline behavior, FAQs with search, settings, first-run welcome, social links, ten themes, eight languages, file logging, every dialog and notice |
| **Ported partially** | My Games, games auto-update and the Library card after a download: all built against the install seam and rendered as "not available on Android yet" |
| **Pending by design** | Installing, uninstalling, launching, running detection, playtime, opening game and help folders: the eleven `TODO-ANDROID-GAME-RUNTIME` questions below |
| **Not ported** | Everything that exists only because the desktop is a Windows program: tray, autostart, self-update, window chrome, single instance, keyboard shortcuts, the download-folder picker, OneDrive, `Saved Games`, the exit warning |

The gates, run from `android/`: `spotlessCheck`, `assembleDebug`,
`testDebugUnitTest` (758 JVM tests), `lintDebug` and `assembleRelease`, all
green. CI runs the same five.

## Ported and working

| Area | Desktop | Android | Notes |
| --- | --- | --- | --- |
| Catalogue | `ContentService.GetGamesAsync`, the Library view | `ContentService`, `LauncherDataCoordinator`, `LibraryScreen` | Real CDN payload, parsed by the captured fixtures in `CdnPayloadTest`. Entries whose id slug repeats are dropped after the first and logged, where the desktop would render both and let their downloads collide |
| Home | `HomeViewModel`, 2-minute refresh | `HomeScreen`, coordinator timer | CET expiry, sticky staleness, both banners. The timer skips ticks while no activity is visible and catches up on return |
| Downloads | `DownloadService`, `LibraryViewModel` | WorkManager + Room + OkHttp | Pause, resume, cancel, retry from a partial, `If-Range`, cache purge, one transfer at a time. Survives backgrounding, rotation and a network drop; a transfer the system stops goes back to `QUEUED` and the card says it is waiting for a connection |
| Special versions | key field in the download dialog, the switch dialog | `SpecialVersionService`, `SpecialVersionPolicy`, both dialogs | Every desktop outcome keeps its own message |
| Maintenance and offline | maintenance flag, offline pill and banners | same | Fails open; a blocked action explains itself once per blocked streak |
| FAQs | `FaqsViewModel`, collation search | `FaqsViewModel`, `SearchMatcher` | Search matches .NET over the shipped corpus (0 of 328,051 pairs differ). One answer is Android content, because the desktop's is false here |
| Settings | theme, language, auto-update, version | same four rows | Persisted in DataStore under the names in `architecture.md` |
| First run | welcome dialog after navigating to Library | same order | Shown once |
| Social links | title-bar buttons, HTTPS guard | same | A failed link stays silent, as on the desktop |
| Themes and text | 10 palettes, 118 keys × 8 languages | 10 palettes, 95 keys × 8 languages | 88 desktop keys kept, 7 Android-only. Hot switching, no activity recreation |
| Dialogs and notices | `CustomMessageBox` and the three dialogs | `LauncherDialog` and friends | Every kept message box is pinned by `DialogMessagesTest` |
| Logging | `Logs` in `%LOCALAPPDATA%` | `FileLogger` in `noBackupFilesDir/logs` | Same line format, monthly files, 10 MiB roll, six months. The same events are logged at the same levels: navigation and loads at debug, user decisions and download lifecycle at info, failures at error, plus every uncaught exception |

## Ported partially

Everything here is complete on the presentation side and waits only on the
install seam. Nothing is a dead button: each one says "not available on Android
yet" (`StatusNotSupportedYet`) where it would otherwise do nothing.

| Area | What works | What is missing and why |
| --- | --- | --- |
| My Games | The screen, its states, the card and every action with its guard | The list of installed games comes from `GameInstallationService.installedGames`, which answers `NotSupportedYet`, so the screen shows that state instead of a list |
| Library card after a download | Transfer to 100 %, then the "Downloaded" chip plus the not-supported line | The archive is never installed (marker 01), so the card cannot reach "installed" or "update available" from a real install |
| Games auto-update | Persisted switch, sequential updater | The updater waits for installed games to be known; the row carries the not-supported line |
| Installation failures | Both desktop messages are wired (`HashMismatch*`, `DownloadError*`) and raised on the transition into a failure | Only a real installer can report one |

## Pending by design: the install and launch seam

Step 09 defined the seam (`service/game/`) and left it inert on purpose. Every
question below is the maintainer's; none has been answered. The full analysis,
with the approaches and their platform consequences, is in
[game-runtime-options.md](game-runtime-options.md). Each marker appears once in
code, and answering one removes the marker and its section together.

| Marker | Question | Status |
| --- | --- | --- |
| `-01` | What an Android game artifact is (APK, data for a bundled runtime, an external listing) and how it is installed | Open |
| `-02` | What uninstall removes, and whether it asks the system to remove a package | Open |
| `-03` | Which source is authoritative for installed identity and version | Open |
| `-04` | How Play launches a game | Open |
| `-05` | Whether a running signal is possible without another app's process or lock | Open |
| `-06` | What marks a session's start and end for playtime | Open |
| `-07` | How game and help files are exposed (URI, in-app viewer, nothing) | Open |
| `-08` | How to persist the catalogue id, expected hash and variant with the download, and how a failed install is retried | Open. Engineering work once `-01` is answered, not a platform question |
| `-09` | Where installation phases and results are stored so they survive process death | Open |
| `-10` | What counts as game help on Android | Open |
| `-11` | How a partial-uninstall blocker is identified and opened | Open |

Things that will need doing when the seam is implemented, recorded so they are
not rediscovered:

- The busy state must include the verify and extract phases, as the desktop's
  `IsDownloading` does.
- A `COMPLETED` download row currently outranks later states on the Library
  card; either the installer deletes the archive or that branch is scoped to the
  row's version.
- `StatusNotSupportedYet`, `NotSupportedYetMessage` and the Settings row notice
  go away in all eight languages at once.
- The DAO SQL, the 1 → 2 migration and any installer would be the first things
  worth an instrumented test; this side has none by rule.

## Not ported, and why

| Desktop behavior | Why | Reference |
| --- | --- | --- |
| Start with Windows, start minimized, the system tray | Android has no user-controlled autostart and no tray; keys and settings removed | `spec/10` 2, 3 |
| Single-instance mutex | The Android task model already gives one instance | `spec/10` 4 |
| Launcher self-update (Velopack), its four dialogs and the "check for updates" row | Updates come from the distribution channel | `spec/10` 5 |
| Window chrome: minimize, close, drag region, 1 px border | There is no window | `spec/10` 6 |
| Download-folder picker, its probe and its dialogs | Games live in app-specific storage, no permission needed | step 07 decision 3, `spec/10` 7, 8 |
| OneDrive detection and warning | No OneDrive-synced folder can be the games root | `spec/10` 9 |
| Keyboard shortcuts | Back replaces Escape; no hardware keyboard is assumed | `spec/10` 13 |
| `Saved Games` rail action | A Windows profile folder | `spec/10` 14 |
| Exit warning | Back on Home backgrounds the task and a transfer keeps running in WorkManager, so there is nothing to lose | step 13 |
| Pre-startup fatal error box | The system's crash dialog covers it; the crash itself is now logged | step 13 |
| Scrollbar | A permanent trough fights the platform scroll indicator | cards' table |
| `UnhandledExceptionPolicy` "keep alive" | An uncaught exception always ends an Android process. The policy stays ported and tested; the logging half is wired through `UncaughtExceptionLogger` | this step |
| `ShutdownWarningPolicy` | Ported and tested, no caller, because the exit warning is gone | step 13 |
| Collation equivalence of 152 letters with no canonical decomposition (`ł` as `l`, `ø` as `o`) | None occurs in any shipped string or payload; matching them would restore a quirk, not a feature | step 06 |

The 26 catalogue keys of the dropped dialogs and Settings rows were removed from
all eight languages in this step; `StringsTest` asserts they stay gone.

## Polish done in this step

**Offline and errors.** Nothing freezes or goes silent: the catalogue and home
content degrade to their empty or last-known states with a message, a transfer
cut off by the network says it is waiting for a connection and resumes by
itself, and a catalogue with repeated ids no longer crashes the Library list.
The FAQ query and its filtered list now come back together after process death
(`SavedStateHandle`).

**Logging.** Checked call site by call site against the desktop's criterion and
filled in: application start, first launch, navigation, refresh, settings
changes, download commands and lifecycle (start, pause, resume, cancel,
completion, failure, system interruption), special-version outcomes,
maintenance blocks, uninstall decisions, and every uncaught exception.

**Accessibility.**

- Every icon-only control has a spoken label, including the dialog ✕
  (`BtnClose`) and the FAQ clear button (`FaqsClearSearch`). Home and Settings
  section titles are headings, and both navigation containers are selectable
  groups, so TalkBack announces the position.
- Text is `sp` everywhere. At 200 % font scale every screen stays usable. The
  FAQ placeholder wrapped and clipped inside its 40 dp bar and now ellipsizes,
  the bar and the offline pill (same fixed-height shape, not renderable on the
  emulator) grow with the text, and dialog titles ellipsize instead of being
  cut.
- Touch targets: navigation items, title-bar buttons and dialog buttons are 48
  dp or more. The desktop's 32 dp card buttons and 24 dp FAQ clear button keep
  their size for fidelity: they meet WCAG 2.2's 24 px minimum and stay below
  Android's 48 dp recommendation, a recorded deviation.
- Contrast, the review list in
  [`localization-and-themes.md`](../.agents/localization-and-themes.md):

  | Finding | Decision |
  | --- | --- |
  | Light themes: inputs identical to the page (`TertiaryBg` = `PrimaryBg`) | **Fixed on Android.** Inputs get a 1 dp `OverlayStrong` outline (4.3:1) only where the palette makes them invisible; dark themes are unchanged |
  | Hover colors, including Cefireon's 1.00:1 | **Accepted.** Touch never shows hover; a mouse is rare on this side |
  | Pressed colors under 2:1 | **Accepted.** Visible only for the duration of a press |
  | Accent used as text, text on accent buttons (worst 1.08:1 and 1.65:1), dim metadata, `SuccessColor` | **Not changed; needs a design decision.** These are the palettes themselves. Changing them on one platform would make the two apps look different on purpose, so it belongs in a change to both sides |
  | The "Downloaded" chip's check | **Accepted.** The label beside it carries the same meaning |

**Screen sizes.** In this step a 411 × 914 dp phone at 100 % and 200 % font and
a 360 × 640 dp phone at 130 % font in both orientations were run on the
emulator; tablets in both orientations were run in steps 11 to 14 and nothing
in this step changes their layout. The shell picks rail or bar from the measured space, cards and Home
adapt by measurement, and dialogs cap to the screen and scroll.

**Code quality.** Android Lint (`warningsAsErrors`) is the static-analysis
gate and ktlint through Spotless the format gate. Lint was red on `HEAD` because
three checks compare against the live Maven index; they are disabled, and
Dependabot's `gradle` entry keeps versions current. CI now also runs
`assembleRelease`, which is what catches a file added to only one build-type
source set and anything R8 breaks.

## Recorded, not done

Carried from the review ledger, none blocking:

| Item | Status |
| --- | --- |
| Release builds cannot export their logs | Needs a "share diagnostics" action and its text; not requested |
| Games auto-update stops without a notice under maintenance, where the desktop shows `ServerActionsUnavailable*` once | Inert while installed games are unknown; wire the notice when the seam lands |
| A card whose action column is empty still reserves the inset beside it, so its text wraps 16 dp earlier (`INSTALLATION_PENDING`, `INSTALLATION_FAILED`) | Cosmetic |
| Spacing token 1 has no owner: its only desktop use is the scrollbar thumb, which is not ported | Add it to `LauncherSpacing` if a scrollbar is ever ported |
| Scrolling the Library to a game (from My Games' update and missing-folder paths) was never run on a device | Its producers need installed games, so it waits on the seam; the code was read and the navigation state is unit-tested |
| After a network drop mid-download the card shows its last speed and time for up to a minute before "waiting for a connection" | The card changes when WorkManager stops the worker, which follows the stalled connection failing (about 47 s on the emulator); the line also flashes for a moment whenever a download is queued |
| The section is not restored after process death | Parity: the desktop starts on Home |
| Open prompts and notices do not survive process death; a notice can pre-empt a prompt and drop a typed key; a prompt whose game disappears reappears if it returns | Rare, cosmetic |
| The foreground notification is rebuilt twice a second and its id derives from the display name | Harmless while one transfer runs at a time |
| Coil uses its own HTTP client | Kept: logos are static CDN images and need none of the content timeouts; the desktop's image loader is separate too |
| The launcher logo is a 60 × 60 PNG, upscaled above ~440 dpi | Cosmetic |
| No guard test against inline regex flags, which ICU rejects | A regex change still needs a device run |
| `spec/` drift: `spec/06` spacing scale, `spec/07` card button padding, `spec/09` step assignments | `spec/` is shared ground; correct it in its own change |
| Pre-existing Spanish comments in root `.editorconfig`, `dependabot.yml`, `CODEOWNERS` | Its own `chore/` change |

## Decisions for the maintainer

1. The eleven seam questions above.
2. The palette contrast rows marked "needs a design decision": both sides or
   neither.
3. Code comments: this side now follows the minimal-comment rule its agents
   work under, and `code-style.md` says so. KDoc written in steps 03 to 06
   survives only in files no later step has edited.
