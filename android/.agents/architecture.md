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
calls that has a different implementation per build type. The debug one shows
the token catalogue; the release one shows `EmptyScreen`.

Two consequences. A file added to one build type's source set must be added to
the other or the release build stops compiling — and CI only builds debug, so
nobody finds out until someone runs `assembleRelease`. And a step that adds
something R8 can break has to run `assembleRelease` itself for the same reason.

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
  — the DataStore seam for theme, language and welcome state. `AppearanceStore`
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
