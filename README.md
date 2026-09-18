<div align="center">

<div style="display: flex; align-items: center; gap: 10px; justify-content: center;">
    <img src="https://ericlostie-launcher.jagoba.dev/public/imgs/logo-launcher.png" width="45" height="45" alt="LostieLauncher icon" />
    <h1 style="margin: 20 0;">LostieLauncher</h1>
</div>

<p align="center">
  <a href="https://lostielauncher.jagoba.dev/">
    <img src="https://img.shields.io/badge/Download%20Installer-Lostie%20Launcher-blue?style=for-the-badge" alt="Download Lostie Launcher" />
  </a>
</p>

**Modern game launcher**

[![Version](https://img.shields.io/badge/version-0.9.1-blue?style=flat-square)](desktop/README.md#-build-and-publish) [![Downloads](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fericlostie-launcher.jagoba.dev%2Fstats%2Fdownloads.txt&search=%5Cs*%28%5B%5E%5Cs%5D%2B%29%5Cs*&replace=%241&label=downloads&color=1a7f37&style=flat-square)](https://lostielauncher.jagoba.dev/) [![Platform](https://img.shields.io/badge/platform-Windows-0078D4?style=flat-square&logo=windows)](https://microsoft.com/windows) [![.NET](https://img.shields.io/badge/.NET-10.0-512BD4?style=flat-square&logo=dotnet)](https://dotnet.microsoft.com/) [![WPF](https://img.shields.io/badge/UI-WPF-68217A?style=flat-square)](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/) [![Velopack](https://img.shields.io/badge/updates-Velopack-FFE084?style=flat-square)](https://velopack.io/) [![License](https://img.shields.io/badge/license-see%20LICENSE-lightgrey?style=flat-square)](LICENSE.txt)

Discover, download, install, and manage your games from a clean interface with multi-language support.

</div>

---

> [!TIP]
> **Thinking about contributing?** Read the [contribution guide](CONTRIBUTING.md) first.

---

## 📦 Monorepo

This repository holds **two applications that share a product, not a stack**.
They have separate builds, separate toolchains and separate conventions, and
they live side by side:

| Side                        | Stack                        | Status                                   |
| --------------------------- | ---------------------------- | ---------------------------------------- |
| [`desktop/`](desktop/)      | WPF · .NET 10 · C# · MVVM    | **Shipping** — the Windows launcher      |
| [`android/`](android/)      | Kotlin · Compose · MVVM      | **Not implemented yet** — reserved space |

```
├── .agents/            # Global agent rules (workflow, boundaries)
├── .github/            # CI, Dependabot and CODEOWNERS for both sides
├── .editorconfig       # Monorepo baseline only (charset, CRLF, indentation)
├── .gitignore          # Covers both sides
├── AGENTS.md           # Global agent guidelines → routes to each side
├── CLAUDE.md           # Pointer: @AGENTS.md
├── CONTRIBUTING.md     # Contribution guide
├── LICENSE.txt
├── README.md           # This file
├── spec/               # Product specification — the port contract, shared by both sides
├── desktop/            # Windows launcher
│   ├── .agents/        #   Desktop-only agent rules
│   ├── .editorconfig   #   C#, XAML and MSBuild rules
│   ├── AGENTS.md       #   Desktop agent index
│   ├── CLAUDE.md       #   Pointer: @AGENTS.md
│   ├── README.md       #   Architecture, build, configuration, endpoints
│   ├── global.json     #   Test runner opt-in
│   ├── LostieLauncher.slnx
│   ├── LostieLauncher/         # The app
│   ├── LostieLauncher.Tests/   # Unit tests
│   └── scripts/        #   Release packaging (maintainer only)
└── android/            # Android app — not implemented yet
    ├── AGENTS.md       #   What the Android guidelines must cover
    ├── CLAUDE.md       #   Pointer: @AGENTS.md
    └── README.md       #   Status and scope of the Android side
```

The Android app is a port of the desktop launcher's **behavior**, not of its
code. The desktop side is the authority on what the product does; how Android
does it is an Android decision.

[`spec/`](spec/) is that behavior written down: screens, service contracts, the
data model, the full text catalog, the design tokens, the component and dialog
anatomy, and a section listing what is intrinsically Windows and therefore has
no direct Android translation. It belongs to neither side — both read it — and
the desktop code remains the authority wherever the two disagree.

### Working on each side

> [!IMPORTANT]
> **Run each side's commands from that side's folder, never from the repository
> root.** The configuration that makes them work lives inside the folder:
> `desktop/global.json` is what opts the repo into the test runner, and it is
> found by walking up from the current working directory. From the root,
> `dotnet test` fails with _"Testing with VSTest target is no longer supported"_
> — that means you are in the wrong directory, not that anything is broken. The
> CI jobs do the same thing with `working-directory: desktop`.

```powershell
# Desktop — build and run the three CI gates
cd desktop
dotnet format LostieLauncher.slnx
dotnet restore LostieLauncher.slnx
dotnet build LostieLauncher.slnx --no-restore --configuration Release
dotnet test  LostieLauncher.slnx --no-build --configuration Release
dotnet list  LostieLauncher.slnx package --vulnerable --include-transitive
```

Full detail for the desktop side — architecture, services, ViewModels,
technologies, release packaging, configuration and API endpoints — is in
[desktop/README.md](desktop/README.md). The Android side will document itself
the same way in [android/README.md](android/README.md).

| You are…                              | Read                                       |
| ------------------------------------- | ------------------------------------------ |
| contributing to the desktop launcher  | [desktop/README.md](desktop/README.md)     |
| contributing to the Android app       | [android/README.md](android/README.md)     |
| porting behavior between the sides    | [spec/README.md](spec/README.md)           |
| opening a pull request                | [CONTRIBUTING.md](CONTRIBUTING.md)         |
| a coding assistant                    | [AGENTS.md](AGENTS.md)                     |

---

## ✨ Features

| Feature                       | Description                                                                             |
| ----------------------------- | --------------------------------------------------------------------------------------- |
| 🎮 **Game library**           | Browse the full catalog of available games from the content server                      |
| ⬇️ **Resumable downloads**    | Supports pausing and resuming downloads with `.part` files and a real-time progress bar |
| 🔑 **Key system**             | Access to exclusive content via download keys validated by the server                   |
| 🕹️ **My games**               | Dedicated view with installed games, version tracking and playtime                      |
| 🔄 **Automatic updates**      | Delta updates with [Velopack](https://velopack.io/) — the launcher updates itself       |
| 📰 **News and notifications** | News feed and announcements from the home screen                                        |
| ❓ **FAQs**                   | Searchable frequently-asked-questions view with localized, collapsible entries           |
| 🎨 **Themes**                 | 10 swappable visual themes: **Volcarona**, **Zoroark**, **Infernape**, **Torterra**, **Empoleon**, **Mewtwo**, **Cefireon**, **Sylveon**, **Astrem**, and **Auretoskos** |
| 🌍 **Multi-language**         | 8 available languages with full UI translations                                         |
| 💾 **Save shortcut**          | Quick-access button to open the save folder of an installed game                        |
| 📺 **Social links**           | Direct buttons to open EricLostie's Twitch, YouTube, and Twitter from the main UI       |
| 🖥️ **System tray**            | Minimize to tray with a context menu (Open / Exit)                                      |
| 🚀 **Start with Windows**     | Option to launch the launcher on login, in normal or minimized mode                     |

The feature set above describes the shipping Windows launcher. It is also the
target the Android port works towards.

---

## 🌍 Supported languages

Español · English · Català · Euskera · Galego · Português · Valencià · Français

The language is selected in the settings and applied dynamically throughout the application.

---

<div align="center">

LostieLauncher © 2026

</div>
