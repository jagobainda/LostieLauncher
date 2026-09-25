# Architecture

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before adding a service, a ViewModel, a
Hilt binding, or anything that needs a testability seam.

Native Android (Kotlin, Jetpack Compose), **MVVM**, one Gradle module (`:app`),
one Hilt object graph. The layer contract is deliberately the same as the
desktop's — same layers, same prohibitions — because the two applications are
the same product and a reviewer should not have to hold two mental models.

## Layer rules

Base package `dev.jagoba.lostielauncher`. The desktop counterpart is named so a
change can be traced across the two sides.

| Layer | Lives in | Desktop counterpart | May depend on | Must never |
| --- | --- | --- | --- | --- |
| Screens | `ui/screen/` | `Views/Partials/` | its own ViewModel, `ui/component`, `ui/theme` | contain business logic or call a service |
| Components | `ui/component/` | `Views/Components/` | `ui/theme`, its own parameters | know a ViewModel or a service exists |
| Dialogs | `ui/dialog/` | `Views/Dialogs/` | `ui/theme`, its own parameters | decide anything the caller should decide |
| ViewModels | `ui/viewmodel/` | `ViewModels/` | `service`, `util`, `content`, `model` | reference a `Composable`, an `Activity` or a `Context` |
| Theme | `ui/theme/` | `Themes/` + `Styles/` | nothing | read a service or hardcode a colour at a use site |
| Services | `service/` | `Services/` | `model`, `util`, `core/coroutines` | know a ViewModel or a composable exists |
| Utils | `util/` | `Utils/` | `model`, ideally nothing | depend on `service` or `ui` |
| Content | `content/` | `Content/` | `model` | be bypassed by hardcoded user-visible text |
| Models | `model/` | `Models/` | nothing | carry behaviour beyond simple derived members |
| Core | `core/` | `Core/` | everything (composition root) | be bypassed by an ad-hoc constructor call |

Two notes on where this diverges from the desktop, both on purpose:

- **`Converters/` has no counterpart.** WPF value converters exist because XAML
  can only bind, not call. Compose calls functions, so the formatting logic
  lands in `util/format/` as plain functions and is unit-tested directly.
- **There is no `App.Services` equivalent and there must not be one.** The
  desktop keeps `SettingsViewModel.Instance` as a static because XAML cannot
  inject; `spec/01-overview.md` says as much and says not to reproduce it.
  Everything here is reached by constructor injection or by `hiltViewModel()`.

Only packages with something in them exist. Do not create an empty package with
a placeholder file to "reserve" it — create it with its first real type.

### Build-type source sets

`app/src/debug/` and `app/src/release/` exist and each holds its own
`ui/StartSurface.kt`. That is how something is made **debug-only**: not a
`BuildConfig.DEBUG` branch, which still ships the code, but a symbol `main`
calls that has a different implementation per build type. Both render the
navigation shell; the debug one also hands it a debug-tools action (the bug
icon in the top bar) that swaps the section content for the download harness
and the token catalogue, and the system back gesture closes it again. The
release one passes nothing, so neither surface is compiled into it.

Two consequences. A file added to one build type's source set must be added to
the other or the release build stops compiling — and CI only builds debug, so
nobody finds out until someone runs `assembleRelease`. And a step that adds
something R8 can break has to run `assembleRelease` itself for the same reason.

### The shell

`ui/screen/LauncherShell.kt` is the only screen root that reads
`MainViewModel`. It maps `MainUiState` onto stateless pieces in
`ui/component/` (`ShellTopBar`, `OfflinePill`, `ShellNavigationRail`,
`ShellNavigationBar`, `LauncherTooltip`) and hosts the active section.

- **Navigation has one source of truth, `NavigationStore`.** There is no
  navigation library and no back stack: the desktop has five top-level
  sections and no history, and a `NavController` would be a second record of
  the current section beside the one ViewModels already navigate through.
  Screen ViewModels obtained with `hiltViewModel()` are therefore scoped to the
  activity, and survive rotation.
- **System back is state.** `MainUiState.canNavigateBack` is true on every
  section but Home and `MainViewModel.navigateBack()` returns to Home; on Home
  the shell leaves back to the system. On Android 12 and later that moves the
  task to the background; on 8 to 11 it finishes the activity, and the next
  launch starts on Home. Either way it is the platform convention for
  top-level destinations.
- **The layout adapts to the space, not to the device type.**
  `shellNavigationLayout` (`ui/screen/ShellNavigationLayout.kt`, JVM-tested)
  decides from the width and from the height below the 50 dp title bar once
  the system bars are taken off. Below `LauncherSizes.NavigationRailBreakpoint`
  (600 dp, Material's compact width boundary) it is always the bottom bar. At
  or above it the desktop's rail is used - 90 dp wide, top group and bottom
  group - and all six items stay visible. They keep the desktop's 60 dp and
  15 dp inset when there is room; when there is not, the items shrink first,
  then the vertical inset gives way, and no item ever goes below
  `LauncherSizes.MinimumTouchTarget` (48 dp). On a tablet that is the desktop
  composition exactly. On a 411 dp-tall phone in landscape it is 48 dp items
  with a 12.5 dp inset and the two groups meeting in the middle. A rail never
  scrolls: only if six 48 dp targets cannot fit at all is the bottom bar used.
  The bottom bar is flush with the screen edge and puts the active indicator on
  the outer edge, as the rail has it on the left.
- **What went with the window.** The minimize and close buttons, the 1 px
  window border, the drag region and the `Saved Games` rail action have no
  meaning on Android (`spec/10-windows-only.md` 6 and 14). Hover states are
  kept for a mouse and mapped to the pressed state for touch; tooltips open on
  long press. There is no ripple, because the desktop has no press animation.
- **Title-bar social buttons come from state** (`MainUiState.contextLinks`),
  and their tooltips are `ExternalLink.brandName`: brand names, not copy, kept
  in the model as `AppLanguage.displayName` is.

### Components

`ui/component/` holds the desktop's `Views/Components/`: `LibraryGameCard` and
`InstalledGameCard` (the two modes of `GameCardComponent`), `NewsCard`,
`NotificationCard`, `FaqCard`, and a skeleton for each of the first three. The
debug catalogue (`src/debug/…/ComponentCatalogScreen.kt`, behind the bug
action) shows every one in every state, under the live theme and language
pickers.

- **Stateless, and they know no ViewModel.** A card takes an immutable state
  (`LibraryGameCardState`, `InstalledGameCardState`) or plain values, and
  lambdas. Fixed labels come from `LocalStrings`, as the desktop's cards bind
  `Strings.*`. `LibraryCardStatus` lives in `model/` so that the component and
  `LibraryViewModel` share it without the component importing the ViewModel
  package.
- **The game card adapts by measurement, not by device.** `GameCardColumns`
  keeps the desktop's three columns (logo, text, actions) while the text column
  keeps at least `LauncherSizes.GameCardTitleWidth`. When it can't, the actions
  drop to a right-aligned row under the text and wrap if needed. The skeleton
  uses the same layout, so content arriving never shifts it.
- **"Not supported yet" is a state, never a dead button.** The Library card
  renders `INSTALLATION_UNSUPPORTED` as the disabled "Downloaded" chip plus an
  amber `StatusNotSupportedYet` line: the archive is downloaded, the game is
  not installed. On a My Games card, `runtimeSupported = false` marks Play,
  Help, Open folder and Uninstall (the four that go through `service/game/`)
  with an amber dot and adds the text to their long-press tooltip. They stay
  tappable, so the seam's `NotSupportedYet` reaches the ViewModel as a notice
  rather than the tap doing nothing.
- **Links are gesture-driven text.** `LinkText` renders `RichText.runs`
  (`util/text/`: link detection plus search highlighting). A tap calls
  `onOpenLink` with the canonical HTTPS URL. A long press shows that URL, which
  is the touch stand-in for the desktop's hover tooltip. A press recolours the
  link to the hover accent, and each link is also a TalkBack custom action.
  Opening the URL is the caller's job, through `ExternalLinkService`.
- **Logos load through Coil**, and only after `HttpsUrls.parseOrNull` accepts
  the URL, as the desktop's converter does.
- **Regex patterns must run on ICU.** Android compiles `java.util.regex`
  through ICU, which rejects inline flags the JVM accepts. `(?U)` crashed the
  first link-scanning card on a device while every JVM test passed.
  `LinkTextParser` therefore spells out its Unicode word class instead of
  switching one on. JVM tests cannot catch this, so a regex change needs a run
  on a device.

#### Where the cards differ from the desktop

Measured against `Views/Components/*.xaml`, `Styles/SkeletonStyles.xaml` and
`Styles/ScrollViewerStyle.xaml`. Everything not listed matches the XAML value
for value. Where `spec/07-components.md` disagrees with the XAML, the XAML was
followed.

| Area | Desktop | Android | Why |
| --- | --- | --- | --- |
| Card dates | Always en-US (`dd MMM yyyy` / `dd MMM`): WPF bindings format in en-US and the app never overrides that | The launcher's language: day, standalone abbreviated month, year (`CardDateFormatter`) | Step 02 decision 7: visible dates follow the selected language. The standalone month keeps Catalan and Valencian from printing the genitive (`de març`, `d'abr.`), and the month is lowercased in every language but English, because locale data disagrees on the case (JDK 17 gives Galician `Mar.`, JDK 21 `mar.`). The abbreviation itself still comes from the platform's locale data, so a dot can differ between Android versions. `CardDateFormatterTest` pins all eight languages on the build's JDK |
| Card layout | Fixed three columns in a fixed-size window | Three columns while the text keeps `GameCardTitleWidth`; otherwise the actions drop under the text and wrap | A phone is narrower than the desktop's content area; the rule is measured, so a tablet keeps the desktop layout |
| Labelled card buttons | No horizontal padding: the style sets `Padding="14,0"` but its template never binds it, so width is `MinWidth="110"` or the content | Same, no padding | Matched. `spec/07` says `14,0` and is wrong |
| Disabled secondary card buttons | No disabled look (same colours, just inert) | 0.5 opacity, like the accent buttons | Touch has no hover to reveal that a button is inert; a button that looks live and does nothing is exactly what the port must avoid |
| Hover and tooltips | Mouse hover colours; tooltips on hover; link hover recolours and shows the URL | A mouse still gets the hover colours; a press shows the desktop's pressed colour, or the hover colour for links, which have no pressed state; tooltips and link URLs open on long press | The touch equivalent of each mouse interaction, as the step 11 shell established; no ripple, because the desktop has no press animation |
| Missing or failed logo | Empty well (the Pokéball shows only when there is no URL) | Pokéball until the image loads, and again if it fails | An empty well reads as broken on a slow mobile connection |
| Text metrics | Segoe UI, no letter spacing, font-derived line height | Material's default body style neutralised theme-wide (`Typography(bodyLarge = TextStyle.Default)` in `Theme.kt`), so text has no extra letter spacing or 24 sp line height | Material's defaults made wrapped card text visibly looser and wider than the desktop's. It also tightens the step 11 shell's labels, towards the desktop |
| Running game | The card has no running state; running is a global signal (`GlobalViewModel`) | No running state either | Faithful. The prompt lists "en ejecución" generically, but the desktop card has none, and inventing one would be a divergence |
| `INSTALLATION_PENDING` (Android only) | No such state: verification starts as soon as the transfer ends | Progress block at 100 % with "100%", empty action column | The gap between the finished transfer and the installer's first report; the bar stays where the download left it, so nothing jumps |
| `INSTALLATION_UNSUPPORTED` (Android only) | No such state | Disabled "Downloaded" chip plus an amber `StatusNotSupportedYet` line | The archive is downloaded and the game is not installed; see "Not supported yet" above |
| `INSTALLATION_FAILED` (Android only) | Failure shows a `DownloadError` or `HashMismatch` dialog and resets the card to Available | Amber `DownloadErrorTitle` line, empty action column | The card cannot know which failure happened, and `LibraryViewModel` offers no retry yet (R-10-g); step 14 owns the retry path |
| Unsupported My Games actions (Android only) | Every action works | Amber dot, tooltip line, still tappable | The seam answers `NotSupportedYet`; see above |
| Card bottom margin | 10 px inside each card | Not in the component; the list spaces cards 10 dp apart | Compose components do not carry outer margins |
| Touch target | 32 px buttons | 32 dp buttons, below Android's 48 dp minimum | Kept for fidelity; step 15's accessibility pass decides |
| Scrollbar | Custom 8 px trough and thumb on every list | Not ported | A permanent trough on a touch list fights the platform's own scroll indicator. The screens that own the lists decide, and the 1 dp spacing token waits for them |

## Dependency injection

- Hilt, with every binding a **singleton in `SingletonComponent`** and resolved
  by **constructor injection**, matching the desktop's "everything is a
  singleton, there is no scoping" composition root.
- Application-wide bindings live in
  [`core/di/CoreModule.kt`](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/CoreModule.kt).
  Later areas get their own module beside it — one per area, not one growing
  module. **Never construct a service at a call site.**
- ViewModels are `@HiltViewModel` with `@Inject constructor`, obtained from a
  composable with `hiltViewModel()` — from
  `androidx.hilt.lifecycle.viewmodel.compose`, not the deprecated copy in
  `androidx.hilt.navigation.compose`. A ViewModel never takes a `Context`; if it
  needs something the platform owns, that goes behind an interface in
  `service/`.
- **No URL, path or magic number inside the type that does the work.**
  Configuration arrives as an immutable options `data class` in `model/`,
  constructed in a Hilt module — the same indirection as the desktop's
  `ContentOptions` / `DownloadOptions` / `UpdateOptions`.
- HTTP clients are provided by the graph, one per purpose with its own timeout,
  never constructed ad hoc. `spec/03-services.md` documents why the three
  differ. They are told apart by the qualifiers in
  [`service/cdn/HttpClients.kt`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/cdn/HttpClients.kt),
  which live beside their consumers so the service layer never has to reach into
  the composition root.
- **Retrofit where there is a payload, raw OkHttp where there is not.** The two
  JSON endpoints go through a Retrofit interface taking an absolute `@Url`,
  because the endpoints do not share a host. The maintenance flag does not:
  its answer is a status code, and Retrofit will not let a `@HEAD` return
  anything but `Unit`.
- A `@Provides` function that returns or takes an `internal` type is itself
  `internal`. Kotlin rejects the alternative, and widening the type instead
  would be widening visibility to satisfy the container.
- A module cannot hold `@Binds` and `@Provides` together — the first needs an
  abstract class, the second an object. When an area needs both, it gets two
  modules side by side, as `SettingsModule` and `SettingsBindingsModule` do.

## Seams: how untestable things become testable

Same rule as the desktop: anything touching the network, the filesystem, the
package manager or a platform service goes behind a **narrow interface with a
thin adapter**, and the interesting logic moves into a pure function in `util/`.

Already in place:

- [`DispatcherProvider`](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/coroutines/DispatcherProvider.kt)
  — the threading seam. Nothing else in the codebase names `Dispatchers`.
- [`Logger`](../app/src/main/kotlin/dev/jagoba/lostielauncher/util/log/Logger.kt)
  — so the "log it and degrade" rule can be asserted in a test. Production
  writes to both Logcat and rotating files in the injected private log directory.
- `java.time.Clock`, bound in `CoreModule` — the wall-clock seam. Content expiry
  is the only thing that reads "now", and a test that cannot pin it is a test
  that starts failing on a date nobody chose. Nothing calls `Instant.now()`,
  `LocalDateTime.now()` or `System.currentTimeMillis()`.
- [`MaintenanceFlagApi`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/cdn/MaintenanceFlagApi.kt)
  — the transport for the one endpoint whose answer is a status code, so the
  decisions that follow from the code stay testable without a socket.
- [`SettingsStore`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/settings/SettingsStore.kt)
  — the DataStore seam for theme, language, welcome and games auto-update state. `AppearanceStore`
  is its narrow UI-facing parent. Updates are visible immediately and a 500 ms
  debounce coalesces persistence writes. Activity and process stop events flush
  any pending snapshot from an application-owned I/O scope.
- [`LocalLibraryStore`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/library/LocalLibraryStore.kt)
  — the Room-backed registry and playtime seam. Its two concerns have separate
  concurrency gates and playtime increments are transactional.
- [`StorageLocations`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/storage/StorageLocations.kt)
  — the injected roots for game files and logs. Game files use app-specific
  external storage with an internal fallback; logs use `noBackupFilesDir`.
- [`DownloadManager`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/download/DownloadManager.kt)
  — the command and observable-state boundary for one active download. Room is
  the durable source of truth, so activity recreation does not own the transfer.
- `DownloadWorkScheduler` and `DownloadTransfer` — WorkManager and OkHttp stay
  behind separate seams. The first owns lifecycle-resilient foreground work;
  the second owns ranged I/O, retry, inactivity timeout and atomic finalization.
- `DownloadWorkerRunner` — the worker's compare-and-set state machine is free of
  Android worker types. `GameDownloadWorker` is only the adapter for foreground
  notification and WorkManager progress callbacks.
- `DownloadedFileHandoff` — completion ends at a downloaded archive and calls
  `GameInstallationService.install`. The pending implementation reports
  `NotSupportedYet` and leaves the archive untouched.
- `GameInstallationService` — installation, uninstall, per-game lookup and an
  observable installed-game list. `observeInstallation(gameId)` exposes
  verification, extraction and terminal outcomes to presentation, including a
  recoverable terminal state when an installer is eventually implemented.
- `GameLaunchService` and `PlaySessionService` — launching, running signals,
  observable active sessions and playtime accounting. Pending implementations
  report unsupported rather than claiming a game is stopped or a session ended.
- `GameLocationService` — help availability, opening game/help files, and
  returning an opaque partial-uninstall location to its owning adapter.
- `LauncherDataCoordinator` — the shared Home and catalogue snapshots and the
  refresh sequence. The first foreground `onStart` starts its initial loads and
  Home timer, independently of screen ViewModel creation. Home and catalogue load
  concurrently on a global refresh; the installed-game projection is
  invalidated only after both finish.
- `GameAutoUpdateCoordinator` — reads the persisted game-update preference at
  process startup, waits for the catalogue and installed games, then queues
  regular outdated games sequentially through `DownloadManager`.
- `NavigationStore` — selected section and a pending Library target. An action
  can be consumed while the target remains available for the future Library
  screen to scroll to and acknowledge.
- `SpecialVersionService` — the `game.config` lookup through the content HTTP
  client, separate from the archive transfer.
- `ExternalLinkService` — opens only validated HTTPS destinations through an
  Android handler. `MainViewModel` sees a result and no `Intent` or `Context`.

The seven presentation ViewModels have no ViewModel-to-ViewModel references.
`MainViewModel` projects shell state from the coordinator, navigation and
download flows. `GlobalViewModel` derives busy state from download and refresh
flows and derives game activity from `GameLaunchService.activeSessions`.
`SettingsViewModel` also owns the appearance and app version state previously exposed by
`AppearanceViewModel`; both the activity and the Settings screen observe the
same `SettingsStore`. The process lifecycle starts shared loading without
requiring Home, Library or Games ViewModels to exist.

Game lifecycle operations always go through `service/game/`. ViewModels must
not call `LocalLibraryStore.registerGame`, `removeGame` or `addPlaytime`; those
writes belong behind the game seam after the Android runtime decision. A
completed download is only a finished transfer, never proof of installation.
While the adapter is pending, presentation must show its
`Finished(NotSupportedYet)` state only for a game with a `COMPLETED` download
row; the adapter reports that state for any id, including games never downloaded.
The open decisions and exact TODO markers are in
[docs/game-runtime-options.md](../docs/game-runtime-options.md).

`DownloadManager.purgeStale` materializes `DownloadCachePolicy`: callers pass
the non-empty catalogue id set after a successful load, managed files older
than 14 days or belonging to removed games are deleted, and inactive Room rows
are removed once no transfer artifact remains. An empty catalogue never purges,
because a failed catalogue request must not turn into data loss.

Prefer extracting a branchy decision into a pure function and testing it
directly over testing it through a ViewModel. The desktop's `*Policy` types are
the model; `spec/09-utilities.md` lists them.

Those are ported and live in `util/`, one sub-package per concern —
`version/`, `format/`, `text/`, `net/`, `download/`, `file/`, `policy/`, and the
pre-existing `log/`. Nothing in there touches the filesystem, the network or a
platform type; the halves that do stayed on the desktop or wait for the step
that owns them. Which desktop test cases came across and which did not:
[testing.md](testing.md#desktop-test-parity-utils).

## Coroutines and state

- A ViewModel exposes state as a `StateFlow` and nothing else. Collect it in a
  composable with `collectAsStateWithLifecycle()`, never `collectAsState()` —
  the second keeps collecting while the app is in the background.
- Work is launched in `viewModelScope`, and anything long-running takes the
  dispatcher from the injected `DispatcherProvider`. No `GlobalScope`, no
  `runBlocking` outside a test, no `Dispatchers.X` written at a call site.
- `withContext(dispatchers.io)` goes **inside** the suspending function that
  needs it, so callers never have to know where it runs. This is the Android
  reading of the desktop's rule that every `await` in a service carries
  `.ConfigureAwait(false)`.
- Suspending functions that can run long accept cancellation by structure
  (`viewModelScope`, `withContext`) rather than a token parameter. Where a
  cancellation has to outlive a scope, pass a `Job` explicitly and say why.
- State shared across threads uses an atomic, as the desktop's interlocked
  `ActivePlaySessions` counter does — `spec/01-overview.md` says the same
  invariant holds here.

## Logging and failure handling

- Log through the injected `Logger`, never `android.util.Log` directly: the
  static is a no-op in a JVM test, which is exactly where the failure paths are
  asserted.
- **Never swallow an exception silently.** Log it, then degrade: return an empty
  list, keep the screen usable, surface a localized message when the user needs
  to know. The launcher must not fall over because the content server is down —
  `spec/03-services.md` has the degradation table, and it is behaviour, not
  advice.
- Log messages are English and state what happened, not that a method ran.
