# LostieLauncher — desktop

The **Windows desktop launcher**: a WPF application on .NET 10 following the
**MVVM** pattern with centralized **Dependency Injection**. This is the desktop
side of the monorepo — see [the root README](../README.md) for the repository as
a whole and for the Android side.

> [!TIP]
> **Thinking about contributing?** Read the [contribution guide](../CONTRIBUTING.md)
> first. If you work with a coding assistant, point it at [AGENTS.md](AGENTS.md).

> [!IMPORTANT]
> Every command below runs **from this folder** (`desktop/`), not from the
> repository root. `global.json` — which opts the repo into the test runner — is
> discovered by walking up from the current working directory, so `dotnet test`
> from the root fails with _"Testing with VSTest target is no longer supported"_.
> That error means you are in the wrong directory.

---

## 🏗️ Architecture

```
desktop/
├── .agents/            # Agent guidelines for this side
├── .editorconfig       # C#, XAML and MSBuild rules
├── global.json         # Test runner opt-in
├── LostieLauncher.slnx
├── scripts/            # Release packaging (maintainer only)
├── LostieLauncher.Tests/   # Unit tests, mirroring the folders below
└── LostieLauncher/
    ├── Core/               # DI container configuration
    ├── Models/             # Data models
    ├── Services/           # Service layer
    ├── ViewModels/         # ViewModels with CommunityToolkit.Mvvm
    ├── Views/              # Windows, dialogs and WPF components
    │   ├── Components/     # GameCard, NewsCard, NotificationCard, FaqCard (+ skeletons)
    │   ├── Dialogs/        # DownloadConfirmDialog, WelcomeDialog, CustomMessageBox, SpecialVersionDialog
    │   └── Partials/       # GamesView, HomeView, LibraryView, FaqsView, SettingsView
    ├── Converters/         # XAML value converters
    ├── Styles/             # Global styles
    ├── Themes/             # Theme resources
    ├── Content/            # Localized strings
    ├── Utils/              # Logging and process utilities
    └── Assets/             # Icons and graphic resources
```

### Main services

| Service                  | Responsibility                                                |
| ------------------------ | ------------------------------------------------------------- |
| `IContentService`        | Fetches the game catalog, news, and registers installed games |
| `IDownloadService`       | Manages downloads, file extraction, and the key system        |
| `ISettingsService`       | Loads and persists configuration in `launcher_settings.json`  |
| `IWindowsStartupService` | Integration with the Windows registry for automatic startup   |
| `IUpdateGateway`         | Seam over the Velopack update manager; checks for app updates  |
| `IUpdateNotifier`        | Prompts the user to apply/restart via WPF dialogs             |
| `IUpdateService`         | Orchestrates the update check flow (gateway + notifier)       |

### ViewModels

| ViewModel           | View                                           |
| ------------------- | ---------------------------------------------- |
| `MainViewModel`     | Main navigation hub                            |
| `HomeViewModel`     | Home screen with news and notifications        |
| `LibraryViewModel`  | Available game catalog and download management |
| `GamesViewModel`    | Installed games                                |
| `FaqsViewModel`     | Searchable FAQ list                            |
| `SettingsViewModel` | Settings panel                                 |
| `GlobalViewModel`   | Shared global state                            |

---

## 🛠️ Technologies

| Package                                                                                                             | Version  | Usage                                             |
| ------------------------------------------------------------------------------------------------------------------- | -------- | ------------------------------------------------- |
| [CommunityToolkit.Mvvm](https://github.com/CommunityToolkit/dotnet)                                                 | 8.4.2    | MVVM with `ObservableProperty` and `RelayCommand` |
| [MahApps.Metro.IconPacks](https://github.com/MahApps/MahApps.Metro.IconPacks)                                       | 6.2.1    | Vector icons in the UI                            |
| [Microsoft.Extensions.DependencyInjection](https://www.nuget.org/packages/Microsoft.Extensions.DependencyInjection) | 10.0.10  | IoC container                                     |
| [Microsoft.Extensions.Http](https://www.nuget.org/packages/Microsoft.Extensions.Http)                               | 10.0.10  | `IHttpClientFactory` with named clients           |
| [SharpCompress](https://github.com/adamhathcock/sharpcompress)                                                      | 0.50.0   | ZIP/7z extraction of downloaded files             |
| [Velopack](https://velopack.io/)                                                                                    | 0.0.1298 | Automatic delta update system                     |

---

## 🚀 Build and publish

### Prerequisites

- [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0)
- Windows 10/11
- PowerShell 7+ (for the release script)
- `vpk` Velopack CLI installed globally

### Development build

```powershell
cd desktop
dotnet build LostieLauncher.slnx
```

### CI gates

The three checks CI runs, which must all pass before a PR is merged:

```powershell
cd desktop

# 1. Formatting
dotnet format LostieLauncher.slnx

# 2. Build and tests in Release
dotnet restore LostieLauncher.slnx
dotnet build LostieLauncher.slnx --no-restore --configuration Release
dotnet test  LostieLauncher.slnx --no-build --configuration Release

# 3. Vulnerable dependencies
dotnet list LostieLauncher.slnx package --vulnerable --include-transitive
```

### Release build (local)

Run from the repository root:

```powershell
.\desktop\scripts\build-release.ps1
```

The script resolves its paths from its own location, so artifacts are generated
in `desktop/releases/`:

- `LostieLauncher-0.9.1-full.nupkg` — initial installation package
- Delta packages (on successive builds)
- `releases.win.json` — update manifest
- `RELEASES` — Velopack metadata

### Release build with server upload

```powershell
.\desktop\scripts\build-release.ps1 -Upload -SshHost "user@my-server.com" -SshPath "/var/www/installer/"
```

---

## ⚙️ Configuration

Configuration is automatically saved to `launcher_settings.json` in `%APPDATA%\LostieLauncher\` (settings from older versions stored next to the executable are migrated automatically).

Games are installed into `<DownloadDirectory>\LostieLauncher\`, which defaults to `%USERPROFILE%\LostieLauncher\`. The launcher deliberately stays out of Documents (usually OneDrive-synced), out of `%LOCALAPPDATA%` (its own install directory) and out of Downloads (Storage Sense can delete its contents). Before a folder is accepted it must pass a write **and rename** check, because a folder can grant write while refusing the rename that finalizes every download.

| Option              | Type          | Default      | Description                                  |
| ------------------- | ------------- | ------------ | -------------------------------------------- |
| `Language`          | `AppLanguage` | `Esp`        | Interface language                           |
| `Theme`             | `AppTheme`    | `Volcarona`  | Visual theme                                 |
| `StartWithWindows`  | `bool`        | `false`      | Launch on Windows startup                    |
| `StartMinimized`    | `bool`        | `false`      | Start in the system tray                     |
| `AutoUpdate`        | `bool`        | `false`      | Check for updates on startup                 |
| `DownloadDirectory` | `string`      | `%USERPROFILE%` | Game library root; games are installed into its `LostieLauncher\` subfolder |
| `HasSeenWelcome`    | `bool`        | `false`      | Controls whether the welcome dialog is shown |

---

## 📡 API Endpoints

| Endpoint                                                                 | Description                        |
| ------------------------------------------------------------------------ | ---------------------------------- |
| `https://ericlostie-launcher.jagoba.dev/games/listado.json`              | Available game catalog             |
| `https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json` | Home screen news and notifications |
| `https://cdn.jagoba.dev/ericlostie-launcher/flag.txt`                     | Maintenance flag (gates server actions) |
| `https://ericlostie-launcher.jagoba.dev/games`                           | Base URL for downloads             |
| `https://ericlostie-launcher.jagoba.dev/public/installer/`               | Velopack update feed               |

---

## 🧪 Tests

`LostieLauncher.Tests/` mirrors the production folders so the matching test is
easy to find. Stack: **xUnit v3**, **NSubstitute**, **Shouldly**. Tests must not
load XAML, instantiate a `Window`, or use a real `Dispatcher` — CI is a headless
Windows agent. The details are in [.agents/testing.md](.agents/testing.md).
