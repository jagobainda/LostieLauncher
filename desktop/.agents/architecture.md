# Desktop architecture

Apply the [shared architecture rules](../../.agents/architecture.md).
The app targets WPF on .NET 10; its unit tests are a separate project.

| Layer | Location | May depend on | Must not |
| --- | --- | --- | --- |
| Views | `Views/` | its ViewModel, visual resources | contain business logic or call services |
| ViewModels | `ViewModels/` | Services, Utils, Content, Models | depend on concrete windows or dialogs |
| Services | `Services/` | Models, Utils | know Views or ViewModels, except isolated UI adapters |
| Utils | `Utils/` | Models, ideally nothing | depend on Services or ViewModels |
| Models | `Models/` | nothing | carry orchestration |
| Core | `Core/` | all layers, as composition root | be bypassed by ad hoc service construction |
| Content | `Content/` | Models | be bypassed by inline UI text |
| Converters | `Converters/` | Models | reach into services or ViewModels |

`Styles/`, `Themes/` and `Assets/` contain visual resources. Paths in this
table are relative to [LostieLauncher/](../LostieLauncher/).

## Composition root and adapters

- Register services, ViewModels and `MainWindow` as singletons in
  [DependencyInjection.cs](../LostieLauncher/Core/DependencyInjection.cs).
- Use the named `IHttpClientFactory` clients: `Content`, `SecurityFlag` and
  `Download`. Their timeouts and headers are configured in that same file.
- Options records in `Models/` carry content, download and update endpoints;
  the shared CDN base is in `Core/Endpoints.cs`.
- `App.Services` is the existing composition-root service locator. Do not
  extend its use. `SettingsViewModel.Instance` is an existing legacy accessor;
  do not add equivalent static accessors.
- Wrap Velopack with `IUpdateGateway` / `VelopackUpdateGateway`.
  `IUpdateNotifier` / `WpfUpdateNotifier` and `IDownloadLocationNotifier` /
  `WpfDownloadLocationNotifier` isolate WPF dialogs from service orchestration.
  These adapters are the deliberate exception to the service/UI boundary.
- Extract pure decisions into `Utils/` policies, using existing types such as
  `ShutdownWarningPolicy`, `DownloadPathUtils` and `VersionUtils` as examples.
- Log through `Logs` in `Utils/LogUtils.cs`: debug for diagnostic detail,
  info for lifecycle and user decisions, error for failures.

For ViewModel conventions and asynchronous state updates, read
[mvvm-and-threading.md](mvvm-and-threading.md).
