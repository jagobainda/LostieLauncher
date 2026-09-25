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
| Dependency injection graph            | started — `core/di/`, persistence and network modules   |
| Theme system                          | done — ten palettes, plus type, spacing, radii, motion  |
| Text catalogue                        | done — 114 keys and 6 FAQs, in eight languages          |
| Domain models and the CDN layer       | done — catalogue, home content, maintenance flag        |
| Settings storage                      | done — DataStore, live state and debounced writes        |
| Local game registry and playtime      | done — Room, transactional and concurrency-safe          |
| File logs                             | done — monthly files, 10 MB roll and six-month retention |
| Pure decision utilities               | done — ported with their desktop test cases             |
| Download engine                       | done — resumable, observable and lifecycle-resilient    |
| Product screens                       | not started                                             |
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
| Navigation | `NavigationStore` (one section value) + `BackHandler`, no navigation library | the launcher has five top-level sections and no back stack, like the desktop; a `NavController` would be a second source of truth beside the store the ViewModels already navigate through |
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
./gradlew assembleRelease               # the R8 path, which CI does not gate
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
test), `MaintenanceFlagApi`, `DownloadManager`, `DownloadTransfer`,
`DownloadWorkScheduler` and `DownloadedFileHandoff`. The rules, including what
each layer may depend on: [.agents/architecture.md](.agents/architecture.md).

`util/` is where that pays off. It holds the desktop's decision functions and
nothing else — version comparison, playtime formatting, accent-insensitive
search, link detection in CDN text, the HTTPS-only guard every outbound link
passes, download cache naming and expiry, and the small exit and crash
policies. Every one takes values and returns a value, so all of it is tested
directly and none of it needs a device.

Matching .NET exactly is harder than it sounds where the framework was doing
the work. Searching is the clearest case: the desktop leans on the invariant
collation, so `instal·lació` and `installació` are the same word, and the FAQ
filter in Catalan and Valencian is useless without it. The rules were measured
against .NET rather than inferred, and a differential over the whole shipped
corpus keeps them honest. Which desktop test cases came across, which did not
and why: [Desktop test parity](.agents/testing.md#desktop-test-parity-utils).

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
setting. `res/values/` holds only what the platform reads before any Kotlin
runs.

**Ten palettes**, in `ui/theme/Palettes.kt`, with the desktop's exact values;
read one with `LocalLauncherColors.current` and never inline a colour.
Everything that is not a colour — the type scale, the spacing scale, radii,
border widths, elevation and the four animation durations — is in
`ui/theme/Tokens.kt` as plain objects, because none of it varies by theme. The
desktop tokenizes colour and nothing else, so those values are the desktop's
but the names are this port's.

**Eight languages**, in `content/`, in Kotlin rather than `res/values-xx/`: 114
string keys and six FAQ entries each. Read text with `LocalStrings.current`, and
substitute a placeholder with `withArgs` — never `format`, which resolves to the
standard library's and quietly does nothing.
A key missing from a language is a compile error, which is the guarantee the
desktop gets from having one class per language and the one property that makes
this worth 900 lines of `override val`.

Both settings live in `SettingsStore`, whose appearance seam reaches the UI through
`AppearanceViewModel`, so changing either recomposes and never recreates the
activity. They are persisted in a Preferences DataStore under the enum member
name. The welcome state shares the same store. Changes update the observable
state immediately and rapid writes are coalesced over 500 ms before DataStore
persists the last snapshot. Stopping the activity flushes a pending snapshot
immediately from an application-owned scope, with a process lifecycle fallback.

A debug-only **token catalogue** shows every colour, type size, spacing and
radius on one page with live theme and language pickers. In a debug build it is
reached from the bug icon in the top bar, beside the download harness; it lives
in `src/debug/`, so it is not compiled into a release APK at all.

See [.agents/localization-and-themes.md](.agents/localization-and-themes.md),
which also carries the contrast review list — colours that fail WCAG on the
desktop values and are deliberately **not** being changed until the end of the
port.

## Local persistence

Room stores the installed-game registry and playtime in separate tables. Their
service keeps independent concurrency gates, and Room transactions make a
playtime increment atomic. Storage errors are logged and degrade to empty data,
matching the desktop behavior without exposing database details to later
ViewModels.

The game-library root is fixed. It uses the app-specific external files area
when that volume is mounted and falls back to internal files otherwise. It
needs no storage permission and is removed with the app on uninstall. There is
no download-directory setting or folder picker on Android. Archives, partials
and resume metadata live under the injected root's `downloads/` directory.

Downloads are durable WorkManager jobs backed by a Room row exposed as a
`Flow`. The foreground notification keeps a large transfer visible outside the
activity; pausing cancels only the worker and retains its validated partial,
resuming sends a ranged request with `If-Range`, and cancelling removes the
archive, partial and metadata. A failed transfer retains its validated partial
for the next attempt; a permission failure removes it. Cache maintenance keeps
recent files for catalogue entries and removes managed files older than 14 days,
files for removed games and durable rows whose artifacts are gone. Only one
transfer may be active. A completed archive crosses the `DownloadedFileHandoff`
seam; extraction and installation remain step 09 work and are deliberately
absent here. The debug build exposes a smallest-game download harness so
progress, pause, resume, cancellation, rotation and background behavior can be
exercised without shipping test UI in the release APK.

Logs continue to go to Logcat and also to `noBackupFilesDir/logs`. The file
format matches the desktop, files are named by month, roll at 10 MB and are
retained for six months. Retention maintenance runs on the I/O dispatcher when
the process starts. Debug builds can inspect them with Android Studio's Device
Explorer or `adb run-as` without making the directory public.

## Contributing

[AGENTS.md](AGENTS.md) has the conventions and the pre-PR checklist;
[../CONTRIBUTING.md](../CONTRIBUTING.md) has the workflow and the acceptance
criteria. See [the monorepo README](../README.md) for how the two sides sit
together.
