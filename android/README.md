# LostieLauncher — Android

The native Android client of Lostie Launcher: a **Kotlin / Jetpack Compose** app
following the same **MVVM** separation as the Windows desktop launcher, and
reproducing its behavior on a phone — game catalog, downloads, installed games,
news and FAQs, ten themes and eight interface languages.

It is a port of the desktop launcher's **behavior**, not of its code. The
authority on what the app must do is the desktop side, under
[`../desktop/`](../desktop/); how Android does it is an Android decision.

> **Games install and launch through Kirin.** The client reads the real CDN,
> verifies and extracts game archives, and registers games for Kirin. Running
> detection and some recovery operations remain incomplete — see [Status](#status)
> and the [parity report](docs/parity-report.md).

## Status

| Piece                                 | State                                                   |
| ------------------------------------- | ------------------------------------------------------- |
| Gradle project and app skeleton       | done — builds, runs, tested in CI                       |
| Architecture guidelines               | done — [root index](../AGENTS.md) and [.agents/](.agents/)  |
| CI jobs                               | done — format, build, tests, lint and release build     |
| Dependency injection graph            | done — one Hilt module per area in `core/di/`           |
| Theme system                          | done — ten palettes, plus type, spacing, radii, motion  |
| Text catalogue                        | done — string and FAQ catalogues in eight languages           |
| Domain models and the CDN layer       | done — catalogue, home content, maintenance flag        |
| Settings storage                      | done — DataStore, live state and debounced writes        |
| Local game registry                   | done — JSON in the games root, survives a reinstall      |
| File logs                             | done — monthly files, 10 MB roll and six-month retention |
| Pure decision utilities               | done — ported with their desktop test cases             |
| Download engine                       | done — resumable, observable and lifecycle-resilient    |
| Shared components                     | done — game, news, notification, FAQ cards and skeletons |
| Dialogs and notices                   | done — message box, download, special version, welcome   |
| Product screens                       | done — Home, My Games, Library, FAQs and Settings        |
| Accessibility, offline and logging    | done — reviewed at the end of the port                  |
| Launcher self-update                  | done — asks, downloads, opens the system installer      |
| Installing and launching a game       | implemented through Kirin; [remaining limitations](docs/game-runtime-options.md#remaining-limitations) |

What is implemented, partial and deliberately omitted, with the reason
for each: [docs/parity-report.md](docs/parity-report.md).

## The stack, and why

Versions are all in [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
this table is the reasoning, not the source of truth.

| Concern | Choice | Why |
| --- | --- | --- |
| Build | Gradle (wrapper, checksum-pinned) + AGP, Kotlin DSL, version catalog | AGP 9 compiles Kotlin itself, so there is no `kotlin-android` plugin anywhere in this build |
| UI | Jetpack Compose + Material 3 | declarative UI; the desktop's XAML has no Android equivalent worth emulating |
| DI | Hilt (KSP) | compile-time verified graph, singleton application services and lifecycle-managed ViewModels, all with constructor injection |
| HTTP | OkHttp + Retrofit + kotlinx.serialization | the desktop runs three HTTP clients with three different timeouts for three reasons; three `OkHttpClient`s sharing a pool is the direct equivalent, and raw OkHttp handles the resumable, ranged download |
| Images | Coil 3 (OkHttp fetcher) | the game logos are remote PNGs on the CDN; Coil is the Compose-native loader, and only an HTTPS URL is ever handed to it |
| Persistence | DataStore (settings) + Room (downloads) + JSON file (local library) | settings are a small typed record; the installed-game registry lives beside the games in shared storage, as `local_games.json` does on the desktop, so it survives reinstalling the app |
| Navigation | `NavigationStore` (one section value) + `BackHandler`, no navigation library | the launcher has five top-level sections and no back stack, like the desktop; a `NavController` would be a second source of truth beside the store the ViewModels already navigate through |
| Tests | JUnit Jupiter (JUnit 6), MockK, Kotest assertions, Turbine | the closest mapping of the desktop's xUnit + NSubstitute + Shouldly |
| Format gate | Spotless + ktlint | this side's `dotnet format --verify-no-changes` |

**SDK levels:** `compileSdk` and `targetSdk` 37 (Android 17), `minSdk` 26
(Android 8.0). 26 is what makes `java.time` available without desugaring — the
content feed's timestamps need real time-zone handling — and brings notification
channels and adaptive icons.

## Building and running

Everything runs **from this folder**, not from the repository root.

```bash
./gradlew assembleDebug                 # build the debug APK
./gradlew testDebugUnitTest             # run the unit tests
./gradlew lintDebug                     # Android Lint, warnings are errors
./gradlew spotlessApply                 # format
./gradlew installDebug                  # install on a connected device
./gradlew assembleRelease               # the R8 path and the release source set
```

A debug build installs as its own app, **Lostie Launcher Dev**
(`dev.jagoba.lostielauncher.debug`), next to the release
(`dev.jagoba.lostielauncher`). The two are signed with different keys, and
Android refuses to install a package over one signed with another key: the
installer only says the app *conflicts with an existing package*. Debug builds
made before the suffix existed took the release's id, so a device that still
has one refuses every release until it is gone, including from any secondary
user, work profile or Xiaomi's Second space and Dual apps:

```bash
adb shell pm uninstall dev.jagoba.lostielauncher    # every user on the device
```

Requirements: an Android SDK with the required licences accepted, and a JDK
capable of starting the pinned Gradle wrapper. SDK/toolchain downloads need
network access when components are not already installed.

The Gradle daemon criteria request **Adoptium 17**
(`gradle/gradle-daemon-jvm.properties`), and the app's toolchain requests Java
language level **17** without a separate vendor constraint. Preserve both;
Gradle/Foojay can resolve missing matching JDKs when downloads are available.
See [build and validation](.agents/build-and-validation.md) for exact CI gates.

### Signed release APK (maintainer only)

The app is distributed as an APK outside any store, signed with the project's
own self-signed key. Android identifies an app by that key: every release must
be signed with the same one, or devices refuse it as an update of the installed
app. A lost key cannot be regenerated, so the keystore and its passwords need a
backup outside this machine.

Generate the key once (about 27 years, beyond the 25 Android recommends), and
keep the keystore outside the repository:

```bash
keytool -genkeypair -v -keystore lostie-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias lostie
```

Then create `android/keystore.properties`, which Git ignores. A relative
`storeFile` resolves against `android/`:

```properties
storeFile=C:/path/to/lostie-release.jks
storePassword=...
keyAlias=lostie
keyPassword=...
```

With that file present, `./gradlew assembleRelease` aligns and signs the APK
itself, writes `app/build/outputs/apk/release/app-release.apk` and copies it to
`release/LostieLauncher-<versionName>.apk`, which Git ignores. Without it, as on
CI, the release is built unsigned as `app-release-unsigned.apk` and not copied.
`./gradlew signingReport` shows which key each variant is signed with.

Publishing a release takes four steps, in this order:

1. Bump **both** `versionCode` and `versionName` in `app/build.gradle.kts`, on
   every release. The [self-update](#launcher-self-update) compares
   `versionName`, and Android refuses an APK whose `versionCode` is lower than
   the installed one's.
2. Build it with `./gradlew assembleRelease`, signed with the same key.
3. Upload the APK from `release/`.
4. Only then point `public/android/latest.json` at it, with the new
   `versionName` and `downloadUrl`. The other way round, launchers are offered
   an APK that is not there yet.

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
function in `util/`. The seams include `DispatcherProvider` (threading),
`Logger`, `java.time.Clock` (so content expiry can be pinned in a test),
`MaintenanceFlagApi`, the download seams, the settings and library stores, and
the game-operation interfaces in `service/game/`. The rules, including what
each layer may depend on: [.agents/architecture.md](.agents/architecture.md).

`util/` is where that pays off. It holds the desktop's decision functions and
nothing else — version comparison, accent-insensitive
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
and why: [test correspondence](docs/test-parity.md) and [search evidence](docs/search-parity.md).

### Reading the CDN

Three endpoints, three HTTP clients, three different timeouts, and the
differences are the behaviour rather than an accident. Content gets 10 s,
because the JSON is small and a slow CDN should fall back to the cache rather
than stall the UI. The maintenance flag gets 3 s, because it gates every
server-backed action and must never be what the user waits on. Downloads have
no overall timeout, so a multi-gigabyte transfer is never cut off by the clock,
but a 20 s connect timeout so a dead host still fails fast; stalls are caught
by an inactivity watchdog instead. Retrofit covers
the two JSON endpoints; the maintenance flag goes through raw OkHttp, because
its answer is a status code and Retrofit will not let a `@HEAD` return anything
else. Every URL and every timeout is written in `core/di/NetworkModule.kt` and
nowhere else.

Transport failures degrade to explicit fallback data, while cancellation is
preserved. A CDN that is down produces an empty catalogue or the
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
border widths, elevation, opacity and animation durations — is in
`ui/theme/Tokens.kt` as plain objects, because none of it varies by theme. The
desktop tokenizes colour and nothing else, so those values are the desktop's
but the names are this port's.

**Eight languages**, with strings and FAQ entries in `content/`, in Kotlin
rather than `res/values-xx/`. Read text with `LocalStrings.current`, and
substitute a placeholder with `withArgs` — never `format`, which resolves to the
standard library's and quietly does nothing.
A key missing from a language is a compile error, which is the guarantee the
desktop gets from having one class per language and the one property that makes
this explicit catalogue useful.

Both settings live in `SettingsStore`, whose appearance seam reaches the UI through
`SettingsViewModel`, so changing either recomposes and never recreates the
activity. They are persisted in a Preferences DataStore under the enum member
name. The welcome state shares the same store. Changes update the observable
state immediately and rapid writes are coalesced over 500 ms before DataStore
persists the last snapshot. Stopping the activity flushes a pending snapshot
immediately from an application-owned scope, with a process lifecycle fallback.

See [.agents/localization.md](.agents/localization.md),
[.agents/themes.md](.agents/themes.md) and the historical
[palette review](docs/palette-review.md) for the rules and visual decisions.

## Local persistence

`FileLocalLibraryStore` keeps the installed-game registry in
`<shared-storage>/LostieLauncher/local_games.json`, beside the games, behind a
single concurrency gate and with atomic `.tmp` writes. App-private storage is
wiped on uninstall while the games are not, so this is what lets a reinstalled
launcher still see them; it is reloaded each time the launcher comes to the
foreground, because the all-files-access permission must be granted again
first.
Android does not track playtime: nothing reliably tells the launcher when a game launched through Kirin ends, and a Kirin update could break any signal it relied on, so there is no playtime table. Storage errors are logged and degrade to empty data,
matching the desktop behavior without exposing database details to later
ViewModels.

The downloads root (`StorageLocations.downloadsRoot`) is fixed. It uses the
app-specific external files area when that volume is mounted and falls back to
internal files otherwise. It needs no storage permission and is removed with
the app on uninstall. There is no download-directory setting or folder picker on
Android. Archives, partials and resume metadata live under its `downloads/`
directory. Installed games do not: they are extracted into shared storage (see
below) and survive the launcher's uninstall.

Downloads are durable WorkManager jobs backed by a Room row exposed as a
`Flow`. The foreground notification keeps a large transfer visible outside the
activity; pausing cancels only the worker and retains its validated partial,
resuming sends a ranged request with `If-Range`, and cancelling removes the
archive, partial and metadata. A failed transfer retains its validated partial
for the next attempt; a permission failure removes it. Cache maintenance keeps
recent files for catalogue entries and removes managed files older than 14 days,
files for removed games and durable rows whose artifacts are gone. Only one
transfer may be active. A transfer the system stops, such as on a dropped
connection, goes back to the queue and the card says it is waiting for a
connection until WorkManager resumes it. A completed archive crosses the
`DownloadedFileHandoff` seam, which verifies it, extracts it into
`<shared-storage>/LostieLauncher/<slug>` and registers the game in Kirin and
the local library. The shared-storage root is resolved for the current Android
user with `Environment.getExternalStorageDirectory()`. It carries a `.nomedia`
marker, created before the first extraction and on startup, which keeps the
games' images out of the gallery. Play launches it through Kirin's shortcut
intent (see
[docs/game-runtime-options.md](docs/game-runtime-options.md)).

Which Kirin the launcher needs comes from the server, in
`public/config/kirin-setup-options.json`:

```json
{
    "minimumVersion": "0.4.0",
    "releaseFeedUrl": "https://raw.githubusercontent.com/shogun-drei/kirin-updates/main/latest.json"
}
```

Below `minimumVersion`, `KirinRequirement` replaces the whole shell with a
screen that tells a missing Kirin from an outdated one and links to the FAQ
entry explaining why. When the release in `releaseFeedUrl` is newer than the
installed Kirin, nothing is blocked: My Games shows a "Kirin update
recommended" pill in the title bar, which downloads and installs it when
tapped. The feed is meant to be Kirin's stable one, not `latest-early-access`.
Until the file is read, as when the launcher starts offline, the minimum is
`KirinSetupOptions.fallbackMinimumVersion` (0.4.0, the first Kirin with the
contract) and nothing is recommended or offered. A failed read is retried on
the next return to the foreground; a successful one lasts for the process.

The installed version is read from `versionName`, because `versionCode` is 23
in both 0.3.5 and the 0.4.0 beta, and compared without its pre-release suffix,
so `0.4.0-beta8` passes 0.4.0. It is checked again every time the launcher
returns to the foreground. The feed has the shape of Kirin's own `latest.json`
(`versionName`, `downloadUrl`). Its release is offered only when it is at the
minimum or later, newer than the installed Kirin and served over HTTPS, and the downloaded APK must be `com.gmax.kirin` at that version or later
before the system installer is opened. That engine, in `service/apk/`, is the
one the [launcher's own update](#launcher-self-update) uses.

Logs continue to go to Logcat and also to `noBackupFilesDir/logs`. The file
format matches the desktop, files are named by month, roll at 10 MB and are
retained for six months. Retention maintenance runs on the I/O dispatcher when
the process starts, and every uncaught exception is written there before the
process ends. What is logged, and at which level, follows the desktop. Debug builds can inspect them with Android Studio's Device
Explorer or `adb run-as` without making the directory public.

## Launcher self-update

The launcher is an APK outside any store, so it updates itself. Every time it
returns to the foreground, `LauncherUpdateService` reads
`https://ericlostie-launcher.jagoba.dev/public/android/latest.json` (written in
`core/di/UpdateModule.kt`): once per process when the read succeeds, again on
the next return when it fails. The feed has the shape of Kirin's:

```json
{
    "versionName": "0.2.0",
    "downloadUrl": "https://…"
}
```

Unknown keys are ignored. The release is offered when its `versionName` is
newer than the installed one, compared as Kirin's is, without its pre-release
suffix, and its `downloadUrl` is HTTPS. There is no checksum: the download must
come over HTTPS, the APK must be this package at that version or later, and
Android itself rejects an APK signed with another key.

The launcher asks before it downloads anything, because the APK may come over
mobile data, and never while a game download is active, because installing it
ends the process. Yes downloads it, with a title-bar pill showing its progress,
and opens the system installer; No holds until the next process start or a
manual check from Settings, which always reads the feed again. Permission to
install unknown apps is the system installer's to ask for, as with Kirin.

A debug build is a different package, `dev.jagoba.lostielauncher.debug`, so
the release it downloads is not this package and is rejected before the
installer opens. That is expected, and nothing in the code works around it.

## Contributing

[The root agent index](../AGENTS.md) links conventions and validation topics;
[../CONTRIBUTING.md](../CONTRIBUTING.md) has the workflow and the acceptance
criteria. See [the monorepo README](../README.md) for how the two sides sit
together.
