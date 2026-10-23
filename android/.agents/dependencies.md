# Android dependency injection and HTTP

Apply the [shared architecture rules](../../.agents/architecture.md).

## Hilt

- Bind application services in area-specific modules under
  [core/di/](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/).
  Services with application lifetime use `@Singleton` in `SingletonComponent`.
  This is not a rule that every binding or ViewModel is a singleton: ViewModels
  use Hilt's ViewModel lifecycle, and value providers may be unscoped.
- ViewModels use `@HiltViewModel` and `@Inject constructor`. UI roots obtain
  them from `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`.
- Keep Context in platform adapters, injected with the appropriate qualifier,
  rather than passing it into a ViewModel.
- Follow the existing separation between abstract `@Binds` modules and object
  `@Provides` modules. Do not widen an internal type for a binding: a provider
  exposing an internal type must also be internal.
- Put environment configuration in immutable options under `model/`, wired
  in the corresponding module.

## Network

- [NetworkModule.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/NetworkModule.kt)
  provides qualified clients for content, maintenance checks and downloads.
  Qualifiers live in `service/cdn/HttpClients.kt` beside their consumers.
- Preserve the content client's short timeout, the shorter maintenance timeout,
  and downloads' lack of an overall timeout with a bounded connection attempt.
  The download transfer also has an inactivity watchdog.
- JSON CDN endpoints use Retrofit with absolute `@Url` values. Maintenance
  checks use raw OkHttp because their result is the HTTP status.
- `RemoteJson` reads runtime requirements and APK release feeds using the
  configured content client. Preserve its logging and cancellation behavior.
- Coil has its own image-loading client; do not route static logos through
  the resumable-download engine.
- A failed maintenance lookup fails open; lack of validated internet is a
  separate `NetworkMonitor` signal. Do not collapse those two decisions.
