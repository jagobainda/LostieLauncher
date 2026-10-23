# Desktop MVVM and threading

Use CommunityToolkit.Mvvm as the existing ViewModels do.

- ViewModels derive from `ObservableObject` and use the partial-property form
  of `[ObservableProperty]`, not annotated private backing fields.
- Use `[NotifyPropertyChangedFor(nameof(...))]` for derived properties when
  the generator can raise the notification.
- Use `[RelayCommand]` and its `CanExecute` option. Refresh guards with
  `NotifyCanExecuteChanged()` when their inputs change.
- Communicate through state notifications or explicit events. A ViewModel
  does not reach into a View.
- Code-behind handles visual concerns: initialization, injected DataContext,
  window chrome, keyboard bindings, dependency properties and scrolling.
  A ViewModel requests a visual effect with an event; unsubscribe when the
  DataContext changes, following `LibraryView`.

## Async and threading

- Service and utility awaits use `.ConfigureAwait(false)`.
- Do not block with `.Result` or `.Wait()`, or wrap blocking work in `Task.Run`
  just to simulate an asynchronous API. Accept cancellation for long operations.
- Restrict `async void` to event handlers, with exception handling and logging.
- Marshal bound-state changes to the UI thread. Preserve the guard around
  `Application.Current` so tests can execute without an application:

```csharp
var app = Application.Current;
if (app is null) return;
app.Dispatcher.Invoke(() => { /* update bound state */ });
```

- `GlobalViewModel` also uses `CheckAccess()` / `BeginInvoke`. Follow the
  established ownership of the dispatcher rather than introducing another one.
- Use `Interlocked` / `Volatile` for shared counters where appropriate.
- Tests use the fixtures in [testing.md](testing.md), without pumping a real
  WPF dispatcher.
