# Android state and lifecycle

Apply the [shared architecture rules](../../.agents/architecture.md).

- Expose immutable presentation state through `StateFlow`. UI collects with
  `collectAsStateWithLifecycle()`.
- Use `viewModelScope` for ViewModel work. Application coordinators and durable
  workers own operations that must outlive a screen.
- Inject `DispatcherProvider`; do not name `Dispatchers` at use sites.
  Put blocking I/O on the injected I/O dispatcher inside the suspending operation.
- Preserve structured cancellation. When catching failures around suspending
  work, rethrow `CancellationException` before degrading or retrying.
- Inject `java.time.Clock` for wall-clock decisions. Use atomic state updates,
  mutexes or appropriate atomic primitives for shared mutable state.
- Log through the injected `Logger`, not directly through `android.util.Log`
  outside the logger adapter. Do not repeat a service's accepted-command log
  in its caller.

## Ownership

- `NavigationStore` owns the selected section and pending Library target;
  ViewModels do not refer to one another.
- `LauncherDataCoordinator` owns shared Home/catalogue data. Process foreground
  startup starts its loads and timer even if no screen ViewModel exists yet.
- `ApplicationLifecycleObserver` starts auto-update once, refreshes installed
  games on foreground entry, refreshes Kirin/launcher updates on resume, and
  flushes pending settings when the process stops.
- `SettingsStore` owns appearance, welcome and game auto-update preferences.
  Changes are immediately observable; persistence is debounced using injected
  options. Activity/process stop flushes the pending snapshot.
- Room owns durable download command/state data; a composable or ViewModel
  must not become the owner of a transfer.
- Rotation survival is not process-death persistence. Persist pending user
  prompts, non-keyed continuations and FAQ view state in `SavedStateHandle`
  (and mark onboarding as seen only when dismissed). Keep documenting which
  state stays in memory only, such as special-version keys, installation phases
  and transient update-check notices. See [game runtime](../docs/game-runtime-options.md).
