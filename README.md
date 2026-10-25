<div align="center">

<img src="android/app/src/main/res/drawable-nodpi/launcher_logo.png" width="80" height="80" alt="LostieLauncher logo" />

# LostieLauncher

Discover, download and play EricLostie's games on **Windows and Android**.

<p align="center">
  <a href="https://lostielauncher.jagoba.dev/">
    <img src="https://img.shields.io/badge/Download-Lostie%20Launcher-blue?style=for-the-badge" alt="Download LostieLauncher for Windows and Android" />
  </a>
  <a href="REPORTING.md">
    <img src="https://img.shields.io/badge/Report%20a%20problem-GitHub-EA580C?style=for-the-badge" alt="Report a problem" />
  </a>
</p>

[![Windows version](https://img.shields.io/badge/version-0.9.2-blue?style=flat-square)](CONTRIBUTING.md#windows-release)
[![Downloads](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fericlostie-launcher.jagoba.dev%2Fstats%2Fdownloads.txt&search=%5Cs*%28%5B%5E%5Cs%5D%2B%29%5Cs*&replace=%241&label=downloads&color=1a7f37&style=flat-square)](https://lostielauncher.jagoba.dev/)
[![Platforms](https://img.shields.io/badge/platforms-Windows%20%7C%20Android-0078D4?style=flat-square)](#platforms)
[![License](https://img.shields.io/badge/license-MIT-lightgrey?style=flat-square)](LICENSE.txt)

</div>

## What you can do

- Browse the game catalog and manage your installed games.
- Download games with pause, resume and progress tracking.
- Install games and download updates from the launcher.
- Access special versions with download keys.
- Read news, announcements and searchable FAQs.
- Choose from ten themes and eight interface languages.

Available languages: Español · English · Català · Euskera · Galego · Português · Valencià · Français.
Change your language and theme in Settings; both apply immediately.

## Platforms

| | Windows | Android |
| --- | --- | --- |
| Requirements | Windows 10 or 11, 64-bit | Android 8.0 or later, with Kirin installed |
| Game launch | Launches installed games directly | Launches installed games through Kirin |
| Downloads | Pause and resume from the launcher | Background downloads with a progress notification |
| Launcher updates | Velopack updates | Downloads an APK and opens the Android installer |
| Platform features | Playtime, save-folder shortcut, system tray and start with Windows | Touch interface and Android storage permissions |

The Windows app is the original launcher. Android is in development and
already supports downloading, installing, updating and launching games.
The applications have separate versions and release processes.

Android does not track playtime or detect whether a game is running.
Some installation recovery operations are still incomplete.

### Install

Download LostieLauncher for Windows or Android from the
[official website](https://lostielauncher.jagoba.dev/).

### Where your games live

On Windows, games go into `%USERPROFILE%\LostieLauncher\` by default.
You can choose another parent folder in Settings. Launcher settings are stored
in `%APPDATA%\LostieLauncher\launcher_settings.json`.

On Android, installed games and their library record live in the shared
`LostieLauncher/` folder and survive uninstalling the launcher. Downloaded
archives use app storage, which Android removes on uninstall. The download
location is fixed.

## Problems and suggestions

Something went wrong, or you have an idea? Follow the short
[reporting guide](REPORTING.md). No programming knowledge is needed.

## Development

This repository contains two applications that share product behavior, with
separate code and toolchains:

| Project | Stack | Source |
| --- | --- | --- |
| Windows | C#, .NET 10, WPF, MVVM | [`desktop/`](desktop/) |
| Android | Kotlin, Jetpack Compose, Hilt, MVVM | [`android/`](android/) |

The [contribution guide](CONTRIBUTING.md) covers setup, builds, tests and
release packaging for each platform. It also links the architecture rules
and the maintained sources for configuration and dependencies.

Coding assistants should start with [AGENTS.md](AGENTS.md).

## License

The launcher source is available under the [MIT License](LICENSE.txt).
