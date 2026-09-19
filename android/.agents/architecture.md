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

## Dependency injection

- Hilt, with every binding a **singleton in `SingletonComponent`** and resolved
  by **constructor injection**, matching the desktop's "everything is a
  singleton, there is no scoping" composition root.
- Application-wide bindings live in
  [`core/di/CoreModule.kt`](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/CoreModule.kt).
  Later areas get their own module beside it — one per area, not one growing
  module. **Never construct a service at a call site.**
- ViewModels are `@HiltViewModel` with `@Inject constructor`, obtained from a
  composable with `hiltViewModel()`. A ViewModel never takes a `Context`; if it
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

## Seams: how untestable things become testable

Same rule as the desktop: anything touching the network, the filesystem, the
package manager or a platform service goes behind a **narrow interface with a
thin adapter**, and the interesting logic moves into a pure function in `util/`.

Already in place:

- [`DispatcherProvider`](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/coroutines/DispatcherProvider.kt)
  — the threading seam. Nothing else in the codebase names `Dispatchers`.
- [`Logger`](../app/src/main/kotlin/dev/jagoba/lostielauncher/util/log/Logger.kt)
  — so the "log it and degrade" rule can be asserted in a test.
- `java.time.Clock`, bound in `CoreModule` — the wall-clock seam. Content expiry
  is the only thing that reads "now", and a test that cannot pin it is a test
  that starts failing on a date nobody chose. Nothing calls `Instant.now()`,
  `LocalDateTime.now()` or `System.currentTimeMillis()`.
- [`MaintenanceFlagApi`](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/cdn/MaintenanceFlagApi.kt)
  — the transport for the one endpoint whose answer is a status code, so the
  decisions that follow from the code stay testable without a socket.

Prefer extracting a branchy decision into a pure function and testing it
directly over testing it through a ViewModel. The desktop's `*Policy` types are
the model; `spec/09-utilities.md` lists them.

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
