# 01 · Overview — what the launcher is and how it is put together

Extracted from `desktop/LostieLauncher/App.xaml.cs`, `App.xaml`,
`Core/DependencyInjection.cs`, `Views/MainWindow.xaml{,.cs}`,
`ViewModels/MainViewModel.cs` and `ViewModels/GlobalViewModel.cs`.

## What the product does

Lostie Launcher distributes and runs a small catalogue of fan-made Pokémon
games. It fetches a game catalogue from a CDN, downloads a game as a ZIP,
verifies it against a published SHA-256, extracts it into a library folder,
registers it locally, launches it, and tracks how long it was played. It also
shows news and notifications on a home screen, answers frequently asked
questions, and updates itself.

Seven concerns make up the whole product:

1. **Catalogue** — what games exist remotely, at which version, at what size.
2. **Library** — which of them are installed locally, at which version.
3. **Transfer** — downloading, pausing, resuming, verifying, extracting.
4. **Launch** — starting a game and recording the play session.
5. **Home content** — news and notifications, localized server-side.
6. **Settings** — language, theme, startup behaviour, download location.
7. **Self-update** — checking for and applying a new launcher build.

## Composition root

A single dependency-injection container is built once at startup
(`DependencyInjection.Configure()`), and **everything is a singleton**: options
records, services, all seven ViewModels and the main window. There is no scoping
and no transient registration anywhere.

Registration order and content:

| Kind | Registered |
| --- | --- |
| Options | `ContentOptions`, `DownloadOptions`, `UpdateOptions` |
| Services | `ISettingsService`, `IWindowsStartupService`, `IContentService`, `IDownloadService`, `IDownloadLocationService`, `IDownloadLocationNotifier`, `IUpdateGateway`, `IUpdateNotifier`, `IUpdateService` |
| HTTP | three named clients: `Content`, `SecurityFlag`, `Download` |
| ViewModels | `GlobalViewModel`, `HomeViewModel`, `GamesViewModel`, `LibraryViewModel`, `FaqsViewModel`, `SettingsViewModel`, `MainViewModel` |
| Views | `MainWindow` |

Two consequences the Android port must preserve:

- **No configuration inside a service.** URLs and base paths arrive as options
  records; the shared CDN base is a single constant
  (`https://ericlostie-launcher.jagoba.dev`). Section 03 lists every endpoint.
- **A user agent is attached to every outbound request**, built as
  `LostieLauncher/<version>` where the version is the assembly version trimmed
  to at most three fields.

The container also carries one **legacy static accessor**:
`SettingsViewModel.Instance`, set in the `SettingsViewModel` constructor and
read from XAML (`{x:Static}`) and from the two notifier adapters. It throws a
descriptive `InvalidOperationException` if read before the container builds. It
exists because XAML cannot inject; an Android port has no equivalent need and
should not reproduce it.

## ViewModel graph

`MainViewModel` is the hub. It holds every other ViewModel and exposes the
active one as `CurrentViewModel`, from which five booleans are derived
(`IsHomeActive`, `IsGamesActive`, `IsLibraryActive`, `IsFaqsActive`,
`IsSettingsActive`).

Dependencies between ViewModels, as constructed:

```
MainViewModel
├── GlobalViewModel      (also injected into Games, Library, Settings)
├── HomeViewModel        ── IContentService, SettingsViewModel
├── GamesViewModel       ── IContentService, LibraryViewModel, GlobalViewModel
├── LibraryViewModel     ── IContentService, ISettingsService, IDownloadService,
│                           GlobalViewModel, DownloadOptions,
│                           IDownloadLocationService, IDownloadLocationNotifier
├── FaqsViewModel        ── SettingsViewModel
└── SettingsViewModel    ── ISettingsService, IWindowsStartupService,
                            GlobalViewModel, IUpdateService,
                            IDownloadLocationService, IDownloadLocationNotifier
```

`GamesViewModel` depends on `LibraryViewModel`, never the reverse. They
communicate through two events on `LibraryViewModel` and one on
`GamesViewModel`; nothing reaches into a View.

### Cross-ViewModel signals

| Signal | Raised by | Consumed by | Effect |
| --- | --- | --- | --- |
| `LibraryViewModel.GameInstalled(name, version, tipo)` | after a successful install | `GamesViewModel` | inserts or replaces the entry in the installed list without a full reload |
| `LibraryViewModel.ScrollToGameRequested(gameId)` | before an update or special-version switch | `LibraryView` | brings that card into view |
| `GamesViewModel.NavigateToLibraryRequested()` | "go to library", update, switch, download-from-missing-folder | `MainViewModel` | switches to the Library screen and retitles the window |
| `PropertyChanged(SettingsViewModel.Strings)` | language change | `MainViewModel`, `FaqsViewModel`, tray menu | re-resolves every visible string |
| `PropertyChanged(SettingsViewModel.Language)` | language change | `HomeViewModel`, `FaqsViewModel` | reloads remote home content and the FAQ set in the new language |
| `PropertyChanged(SettingsViewModel.DownloadDirectory)` | library folder change | `MainViewModel` | triggers `LibraryViewModel.RefreshAsync()` |
| `PropertyChanged(GlobalViewModel.IsDownloading / IsRefreshing)` | transfer state | `MainViewModel` | re-evaluates whether refresh is allowed |
| `PropertyChanged(<screen>.IsLoading)` | any of Home, Library, Games | `MainViewModel` | re-evaluates whether refresh is allowed |

## Global state

`GlobalViewModel` is the only genuinely application-wide state. It is small:

| Member | Type | Meaning |
| --- | --- | --- |
| `IsDownloading` | `bool` | a transfer is active; only one at a time is allowed |
| `IsRefreshing` | `bool` | the manual refresh of all screens is running |
| `IsBusy` | derived | `IsDownloading \|\| IsRefreshing` |
| `ActivePlaySessions` | `int`, interlocked | number of tracked game processes currently alive |
| `IsGameRunning` | derived | `ActivePlaySessions > 0` |

`BeginPlaySession()` / `EndPlaySession()` increment and decrement the counter
atomically and then raise change notification on the UI thread. The counter is
read with `Volatile.Read`. On Android the same invariant holds — the counter is
mutated from a background completion and read by the UI — so it needs the same
thread-safety, whatever the mechanism.

`IsDownloading` and `IsGameRunning` together decide whether exiting the
application needs a confirmation; see the exit rules below.

## Startup sequence

`App.OnStartup` runs, in this exact order:

1. **Velopack bootstrap** (`VelopackApp.Build().Run()`). This must be the first
   statement: the updater hooks install, update and uninstall hooks here.
2. **Single-instance guard.** A named mutex `LostieLauncherSingleInstance` is
   claimed. If it was already held, the process signals the named event
   `LostieLauncherShowWindow` so the running instance restores its window, then
   shuts itself down without touching anything else.
3. **Show-window listener.** The winning instance registers a wait on that event
   and restores its main window whenever it fires.
4. **Shutdown mode** set to explicit — closing the window does not exit.
5. **Three global exception hooks** are installed: dispatcher-unhandled,
   appdomain-unhandled, and unobserved task exceptions. All three log; only the
   dispatcher one can end the process (see below).
6. **The container is built.**
7. **Old logs are purged** (six-month retention) and startup is logged.
8. **An update check is fired and forgotten** on a background task, with
   `notifyWhenUpToDate: false` — a silent check.
9. **The main window is resolved** and the tray icon is created.
10. **The window is shown** unless `StartupWindowPolicy.ShouldShowOnStartup`
    says otherwise: it is `!startMinimized || !hasSeenWelcome`. A first run
    therefore always shows the window, even with "start minimized" enabled.
11. **On a first run** (`HasSeenWelcome == false`) the launcher navigates to the
    Library, marks the welcome as seen, and shows the welcome dialog modally.

### Unhandled exception policy

`UnhandledExceptionPolicy.Decide(startupCompleted)` returns `Fatal` before
startup finished and `KeepAlive` afterwards. A dispatcher exception is always
logged and always marked handled; only a pre-startup one shows the fatal message
box (English, hardcoded, not localized — the string catalogue may not exist yet)
and shuts down.

## Window chrome and the shell

The main window is **1000 × 650**, `CanMinimize` only (not resizable),
centred on screen, with `WindowStyle="None"` and a 1 px border in the accent
colour. Its background is the primary background brush. Its icon is loaded from
a remote HTTPS URL.

The shell is two rows:

- **Row 0 — title bar, 50 px.** Draggable by left-click (single click only).
  Left: the window icon at 22 × 22, the current screen title at 13 px SemiBold,
  and an *offline* pill. Right: context buttons, then minimize, then close.
- **Row 1 — content.** Two columns: a 90 px navigation rail and the active
  screen, the rail inset by 15 px on all sides and the content by 15 px on top,
  right and bottom.

### The offline pill

Visible only when `MainViewModel.IsOfflineMode` (proxied from
`HomeViewModel.IsOfflineMode`). Height 22, padding 8 horizontal, radius 4, a 1 px
border in `#FFFFC107` (amber — a literal, not a theme key), background
`OverlaySubtleBrush`, a 12 × 12 amber cloud-off icon, and the
`OfflineModeLabel` string at 11 px SemiBold. Its tooltip is
`ServerMaintenanceNotificationTitle`.

### Title-bar context buttons

They appear per screen, 50 × 50, transparent, hover `OverlayMediumBrush`:

| Screen | Buttons |
| --- | --- |
| Settings | GitHub |
| Home | Twitch, YouTube, Twitter/X |
| any | minimize, close (close hovers `#E81123`) |

The social links open fixed HTTPS URLs through a launcher that refuses anything
that is not an absolute `https://` URI:
`github.com/jagobainda/LostieLauncher`, `twitch.tv/ericlostie`,
`youtube.com/@EricLostie`, `x.com/Eric_Lostie`.

### Navigation rail

Background `SecondaryBgBrush`. Items are 60 × 60. Three navigation items and two
action buttons at the top, two navigation items at the bottom:

| Position | Item | Icon | Kind |
| --- | --- | --- | --- |
| top 1 | Home | House | navigation |
| top 2 | My Games | Play | navigation |
| top 3 | Library | Download | navigation |
| top 4 | Saved Games | Save | action |
| top 5 | Refresh | RefreshCw | action |
| bottom 1 | FAQs | CircleQuestionMark | navigation |
| bottom 2 | Settings | Cog | navigation |

A navigation item is a radio button bound one-way to its `Is…Active` flag. Its
selected state paints the foreground with the accent, the background with
`OverlaySubtleBrush`, and shows a **3 px accent bar down the left edge**. Hover
paints `OverlayLightBrush`. Icons are 22 × 22 and inherit the item foreground,
which is `SecondaryFgDimBrush` when unselected. A disabled action button drops
to **30 % opacity**.

Tooltips come from the string catalogue except *Saved Games*, which is the
literal `"Saved Games"`.

### Screen titles

The title bar shows the active screen's title, re-resolved whenever the language
changes: `TitleHome`, `TitleGames`, `TitleLibrary`, `TitleFaqs`,
`TitleSettings`.

## Refresh

`RefreshDataCommand` refreshes Home and Library **in parallel**, then Games
**after both complete** — Games derives its "has update" flags from the loaded
library, so the order matters. It sets `GlobalViewModel.IsRefreshing` for the
duration and always clears it.

It can only run when **all five** of these are false: `IsDownloading`,
`IsRefreshing`, `HomeViewModel.IsLoading`, `LibraryViewModel.IsLoading`,
`GamesViewModel.IsLoading`. Each of those raising `PropertyChanged` re-evaluates
the guard.

## Keyboard shortcuts

| Shortcut | Action |
| --- | --- |
| `Alt+1` | Home |
| `Alt+2` | My Games |
| `Alt+3` | Library |
| `Alt+4` | Settings |
| `F5` | Refresh all |
| `Ctrl+G` | Open the Windows *Saved Games* folder |
| `Esc` | Hide the window (minimize to tray) |
| `Ctrl+M` | Minimize the window |
| `Ctrl+Shift+Q` | Request shutdown (with the exit confirmation) |

There is deliberately **no shortcut for the FAQ screen**.

Dialogs get their own binding set via a shared helper: `Enter` confirms,
`Escape` cancels, and on a yes/no dialog `Y` and `N` answer directly.

## Closing, hiding and exiting

Three different things, kept distinct:

- **Close button / window close** — cancelled; the window hides. The process
  keeps running in the tray.
- **Minimize** — normal window minimize.
- **Exit** — only from the tray menu or `Ctrl+Shift+Q`, and only after
  `ConfirmShutdown()`.

`ConfirmShutdown()` consults `ShutdownWarningPolicy.Decide(isDownloading,
isGameRunning)`, which returns `None`, `Download`, `Game` or `Both`. Anything
but `None` shows a yes/no dialog with the matching message
(`ExitWarningDownloadMessage`, `ExitWarningGameMessage`,
`ExitWarningBothMessage`) under `ExitWarningTitle`, and exits only on yes.

On exit: the show-window listener and event are disposed, the mutex released and
disposed, the tray icon disposed, and the container disposed — each inside its
own try/catch so one failure cannot block the rest.

## Tray icon

Visible for the whole process lifetime. Its icon is extracted from the running
executable, falling back to the system application icon. Tooltip
`"Lostie Launcher"`. Its context menu has two items separated by a divider —
`TrayOpen` and `TrayExit` — whose text is refreshed whenever the language
changes. Double-clicking the icon restores the window (show, set normal state,
activate).

## Game process lifecycle

Launching is `GamesViewModel.Play`:

1. Resolve `<gamesRoot>/<gameName>/Game.exe`. If it is missing, show
   `GameExeNotFound*` and stop.
2. Start it with the shell, working directory set to the game folder.
3. **Minimize the main window.**
4. Register a play session: increment the global counter, remember the process
   under the game name, subscribe to its exit, and — because a process can exit
   between start and subscribe — check `HasExited` immediately and fire the
   handler manually if it already did. The handler is guarded by an interlocked
   flag so it can only run once.

On exit: compute whole minutes elapsed, persist them against the game's GUID
(skipped when zero minutes or an empty GUID), add them to the in-memory playtime
of both the installed entry and the library entry, **restore and activate the
main window**, then untrack the process and decrement the counter.

"Is this game running?" has three answers
(`GamesViewModel.GetRunningSignal`): `TrackedProcess` when the launcher started
it and it is still alive, `ExecutableLocked` when the executable cannot be
opened exclusively, and `NotRunning` otherwise. Uninstall treats them
differently — see section 02.

## Restarting the application

`ProcessUtils.RestartApplication` exists behind an `IApplicationRestarter` seam.
It resolves the executable path, **releases the single-instance mutex**, starts a
new process, and shuts down — re-acquiring the mutex if the start throws, so a
failed restart leaves a working instance rather than one that has given up its
lock. Nothing in the current UI calls it; Velopack's own restart is used instead.

## Logging

A single static logger writes to
`%LOCALAPPDATA%/<app friendly name>/logs/`, one file per month named
`yyyy-MM.log`, rolling to `yyyy-MM.1.log`, `.2` and so on at **10 MB**. Lines are
`yyyy-MM-dd HH:mm:ss [LEVEL] -> message` with levels `DEBUG`, `INFO`, `ERROR`.
Retention is **six months**, purged at startup. Writing is lock-guarded and
every failure is swallowed: losing a log line never propagates to the caller.

Log messages are English and state what happened. The one exception, worth
recording because it is a genuine inconsistency rather than a rule:
`VersionUtils.IsNewerVersion` logs in Spanish.
