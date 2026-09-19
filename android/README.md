# LostieLauncher — Android

The native Android client of Lostie Launcher: a **Kotlin / Jetpack Compose** app
following the same **MVVM** separation as the Windows desktop launcher, and
reproducing its behavior on a phone — game catalog, downloads, installed games,
news and FAQs, ten themes and eight interface languages.

It is a port of the desktop launcher's **behavior**, not of its code. The
authority on what the app must do is the desktop side, under
[`../desktop/`](../desktop/), with the extracted contract in
[`../spec/`](../spec/); how Android does it is an Android decision.

> **Early.** The project builds, runs and is tested, but it does not launch
> games yet — see [Status](#status). Nothing below is a promise about a shipped
> app.

## Status

| Piece                                 | State                                                   |
| ------------------------------------- | ------------------------------------------------------- |
| Gradle project and app skeleton       | done — builds, runs, tested in CI                       |
| Architecture guidelines               | done — [AGENTS.md](AGENTS.md) and [.agents/](.agents/)  |
| CI jobs                               | done — two jobs in `../.github/workflows/ci.yml`        |
| Dependency injection graph            | started — `core/di/`, two modules                       |
| Theme system                          | one palette of ten (Volcarona), no other tokens yet     |
| Domain models and the CDN layer       | done — catalogue, home content, maintenance flag        |
| Persistence, downloads, screens       | not started                                             |
| Installing and launching a game       | out of scope for now                                    |

## The stack, and why

Versions are all in [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
this table is the reasoning, not the source of truth.

| Concern | Choice | Why |
| --- | --- | --- |
| Build | Gradle (wrapper, checksum-pinned) + AGP, Kotlin DSL, version catalog | AGP 9 compiles Kotlin itself, so there is no `kotlin-android` plugin anywhere in this build |
| UI | Jetpack Compose + Material 3 | declarative UI; the desktop's XAML has no Android equivalent worth emulating |
| DI | Hilt (KSP) | compile-time verified graph, everything a singleton — the desktop's centralized container, constructor injection included |
| HTTP | OkHttp + Retrofit + kotlinx.serialization | the desktop runs three HTTP clients with three different timeouts for three reasons; three `OkHttpClient`s sharing a pool is the direct equivalent, and raw OkHttp handles the resumable, ranged download |
| Persistence | DataStore (settings) + Room (local library) | settings are a small typed record; the installed-game list is queried and joined |
| Navigation | Navigation Compose, type-safe routes | Navigation 3 is not stable yet; revisit when it is |
| Tests | JUnit 5, MockK, Kotest assertions, Turbine | the closest mapping of the desktop's xUnit + NSubstitute + Shouldly |
| Format gate | Spotless + ktlint | this side's `dotnet format --verify-no-changes` |

**SDK levels:** `compileSdk` and `targetSdk` 37 (Android 17), `minSdk` 26
(Android 8.0). 26 is what makes `java.time` available without desugaring — the
content feed's timestamps need real time-zone handling — and brings notification
channels and adaptive icons, at roughly 96 % of active devices.

## Building and running

Everything runs **from this folder**, not from the repository root.

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew testDebugUnitTest             # run the unit tests
./gradlew lintDebug                     # Android Lint, warnings are errors
./gradlew spotlessApply                 # format
./gradlew installDebug                  # install on a connected device
```

Requirements: an Android SDK (the build accepts its licences and downloads what
it is missing) and a JDK between 17 and 26 for `gradlew` to start with.

Everything after that is pinned, so a build does not vary by machine: the Gradle
daemon runs on **Temurin 17** (`gradle/gradle-daemon-jvm.properties`) and the
code compiles against **17** (a toolchain in `app/build.gradle.kts`). Gradle
downloads that JDK itself if the machine has none. Both pins matter — a
JetBrains Runtime, the JDK a JetBrains IDE ships with, is missing `jlink` that
AGP needs and crashes KSP's worker.

## Architecture

MVVM with a single Hilt object graph, one Gradle module, and a layer contract
enforced by package that mirrors the desktop's layer for layer:

```
core/     composition root — Hilt modules, dispatchers        (desktop: Core/)
model/    immutable domain models, enums, options             (desktop: Models/)
service/  service layer + the interfaces isolating platform   (desktop: Services/)
content/  localized text catalogue                            (desktop: Content/)
util/     pure decision functions, formatting, logging        (desktop: Utils/)
ui/       MainActivity, screen/ component/ dialog/ theme/     (desktop: Views/, ViewModels/, Themes/)
```

Anything touching the network, the filesystem or a platform service sits behind
a narrow interface with a thin adapter, and the decision logic moves into a pure
function in `util/`. The seams that exist so far are `DispatcherProvider`
(threading), `Logger`, `java.time.Clock` (so content expiry can be pinned in a
test) and `MaintenanceFlagApi`. The rules, including what each layer may depend
on: [.agents/architecture.md](.agents/architecture.md).

### Reading the CDN

Three endpoints, three HTTP clients, three different timeouts, and the
differences are the behaviour rather than an accident —
[spec/03-services.md](../spec/03-services.md) explains each one. Retrofit covers
the two JSON endpoints; the maintenance flag goes through raw OkHttp, because
its answer is a status code and Retrofit will not let a `@HEAD` return anything
else. Every URL and every timeout is written in `core/di/NetworkModule.kt` and
nowhere else.

The layer never throws. A CDN that is down produces an empty catalogue or the
last known home content flagged stale, and the maintenance flag fails *open* so
an unreachable server cannot lock a user out of their own launcher.

`app/src/test/resources/cdn/` holds byte-for-byte captures of the two live
payloads, and `CdnPayloadTest` parses them through the real OkHttp, Retrofit and
`Json` against a loopback `MockWebServer`. Re-capture them by fetching the two
URLs in `NetworkModule` again.

## Themes and text

Both are runtime-switchable and persisted, as on the desktop — so neither uses
Android resource qualifiers, which follow the system rather than an in-app
setting. Colours come from `LocalLauncherColors.current`; user-visible text will
come from the `content/` catalogue. `res/values/` holds only what the platform
reads before any Kotlin runs.

Only one of the ten palettes exists so far: **Volcarona**, the desktop's default
and fallback, with its exact values, guarded by a test that compares them
against the desktop's own. See
[.agents/localization-and-themes.md](.agents/localization-and-themes.md).

## Contributing

[AGENTS.md](AGENTS.md) has the conventions and the pre-PR checklist;
[../CONTRIBUTING.md](../CONTRIBUTING.md) has the workflow and the acceptance
criteria. See [the monorepo README](../README.md) for how the two sides sit
together.
