# Android architecture

Apply the [shared architecture rules](../../.agents/architecture.md).
The app uses Kotlin, Compose and MVVM in one Gradle module, `:app`.

Paths below are relative to
[dev/jagoba/lostielauncher/](../app/src/main/kotlin/dev/jagoba/lostielauncher/).

| Layer | Location | May depend on | Must not |
| --- | --- | --- | --- |
| Screens | `ui/screen/` | its ViewModel, components, theme | contain business logic or call services |
| Components and dialogs | `ui/component/`, `ui/dialog/` | parameters, callbacks, theme, localized content | know a ViewModel or service |
| ViewModels | `ui/viewmodel/` | service, util, content, model | depend on composables, Activity or Context |
| Theme | `ui/theme/` | Compose value types and theme configuration | depend on services |
| Services | `service/` | model, util, injected dispatchers | depend on ViewModels or composables |
| Utils | `util/` | model, ideally nothing | depend on service or UI |
| Content | `content/` | model | be bypassed by inline UI text |
| Models | `model/` | value types | carry application orchestration |
| Core | `core/` | all layers, as composition root | be bypassed by ad hoc service construction |

- There is no WPF converter layer; formatting is a plain function under `util/`.
- Do not copy desktop static accessors or `App.Services`. Dependencies arrive
  through constructors or `hiltViewModel()` at UI roots.
- Keep platform adapters testable through interfaces and pure decisions.
  Existing seams include `DispatcherProvider`, `Logger`, `Clock`,
  `NetworkMonitor`, the settings/library stores and download/game services.
- Keep build-type-specific code in `src/debug/` and `src/release/` when needed.
  Do not add production-only branches merely to hide an implementation mismatch.

Read [dependencies.md](dependencies.md) for Hilt and HTTP configuration,
[state-and-lifecycle.md](state-and-lifecycle.md) for flow ownership, and
[game-runtime.md](game-runtime.md) before changing game operations.
