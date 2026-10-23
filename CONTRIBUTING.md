# Contribution guide

Thanks for your interest in contributing to **LostieLauncher**! This document
summarizes the workflow, conventions, and requirements that any contribution
must meet to be merged into the project.

**Reporting a problem or suggesting an idea?** Use the short
[reporting guide](REPORTING.md); you do not need to build the app.

LostieLauncher is a **monorepo** holding two applications that share a product
but not a stack:

| Side                   | Stack                     | Status                  |
| ---------------------- | ------------------------- | ----------------------- |
| [`desktop/`](desktop/) | WPF · .NET 10 · C# · MVVM | Shipping                |
| [`android/`](android/) | Kotlin · Compose · MVVM   | In development          |

Before you start, review the [product overview](README.md), then follow the
setup instructions for your platform below.

> [!IMPORTANT]
> **Run each side's commands from that side's folder, never from the repository
> root.** The configuration that makes them work lives inside the folder:
> `desktop/global.json` is what opts the repo into the test runner, and it is
> found by walking up from the current working directory. From the root,
> `dotnet test` fails with _"Testing with VSTest target is no longer supported"_
> — that means you are in the wrong directory, not that anything is broken.

Keep a pull request to **one side**. A change that touches both `desktop/` and
`android/` needs a reason; do not fix something on the other side while you are
in there.

---

## Development setup

### Windows

Use Windows 10 or 11 and the .NET 10 SDK. From the repository root:

```powershell
cd desktop
dotnet build LostieLauncher.slnx
dotnet run --project LostieLauncher
```

The development build does not require the release packaging tools.

### Android

Install Android Studio or the Android SDK command-line tools, accept the SDK
licences and install the SDK requested by
[`libs.versions.toml`](android/gradle/libs.versions.toml).
Set `sdk.dir` in the ignored `android/local.properties`, or configure
`ANDROID_HOME` to point to the SDK.

Use a compatible JDK to start the committed Gradle wrapper. The daemon criteria
request Adoptium 17, and the app's Java toolchain requests language level 17.
Gradle/Foojay can download missing matching JDKs when network access is available.
Keep the daemon criteria and the toolchain settings intact.

From the repository root:

```bash
cd android
./gradlew assembleDebug
./gradlew installDebug
```

On Windows use `.\gradlew.bat` instead of `./gradlew`. Installation requires
a connected device or running emulator. The debug APK is written under
`app/build/outputs/apk/debug/`.

Debug builds install as **Lostie Launcher Dev** (`dev.jagoba.lostielauncher.debug`),
alongside the release app (`dev.jagoba.lostielauncher`). They use different signing
keys. Older debug builds used the release package ID; uninstall those before
installing a release, including copies in secondary users or work profiles.
A debug build rejects the launcher's release APK during self-update because
the package IDs differ.

## Technical references

Both apps use MVVM with injected services and pure decision utilities. Desktop
is the reference for product behavior; Android implements that behavior with
its own platform services. Use the [agent index](AGENTS.md) to find the shared
and platform-specific architecture, UI, localization and test rules.

| Topic | Windows | Android |
| --- | --- | --- |
| Architecture and layers | [Desktop architecture](desktop/.agents/architecture.md) | [Android architecture](android/.agents/architecture.md) |
| Dependencies | [Project file](desktop/LostieLauncher/LostieLauncher.csproj) | [Version catalog](android/gradle/libs.versions.toml) |
| HTTP configuration | [Composition root](desktop/LostieLauncher/Core/DependencyInjection.cs), [endpoints](desktop/LostieLauncher/Core/Endpoints.cs) | [Network module](android/app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/NetworkModule.kt) |
| Settings | [Defaults](desktop/LostieLauncher/Models/AppSettings.cs) | [DataStore implementation](android/app/src/main/kotlin/dev/jagoba/lostielauncher/service/settings/DataStoreSettingsStore.kt) |
| Downloads and game launch | [Desktop architecture](desktop/.agents/architecture.md) | [Android game operations](android/.agents/game-runtime.md) |
| Tests | [Desktop testing](desktop/.agents/testing.md) | [Android testing](android/.agents/testing.md) |

Desktop uses CommunityToolkit.Mvvm, Microsoft dependency injection and named
HTTP clients, SharpCompress for extraction, and Velopack for launcher updates.
Android uses Compose and Material 3, Hilt, OkHttp/Retrofit, Coil, DataStore for
settings, Room and WorkManager for downloads, and a shared JSON game registry.
Android's five top-level sections use `NavigationStore` without a navigation
back stack. Dependency versions belong in the build sources linked above.

Android game setup and launcher updates use separate configurations:

- [Kirin module](android/app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/KirinModule.kt):
  `public/config/kirin-setup-options.json` provides `minimumVersion` and
  `releaseFeedUrl`. Missing/outdated Kirin blocks setup; recommended updates
  do not. The fallback minimum is defined in that module.
- [Update module](android/app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/UpdateModule.kt):
  `public/android/latest.json` provides `versionName` and `downloadUrl` for
  launcher updates. The APK installer validates the package and version before
  handing off to Android, which checks the signing key.

## 🔀 Git workflow

1. **Fork** the repository.
2. Always branch off **`development`**, not `main`.
3. Create a descriptive branch for your change using the
   `<type>/<short-english-description>` format. Use the most specific type:
   `feat/` for features, `fix/` for bug fixes, `security/` for vulnerability
   fixes, `chore/` for maintenance, `docs/` for documentation, `refactor/` for
   restructuring, `perf/` for performance improvements, `test/` for tests only,
   `ci/` for CI/CD changes, and `build/` for build or packaging changes.
4. Write the description in concise English, using lowercase kebab-case with
   only letters, numbers, and hyphens. For example:
   `fix/login-crash-empty-password`.
5. Make your commits.
6. Open a **Pull Request against `development`**. Merging into `main` is reserved
   for releases.

`main` is the stable release branch; `development` is the integration branch
where changes come together before a new version.

---

## ✅ Before opening a Pull Request

Your PR must pass the CI jobs in `.github/workflows/ci.yml`. Run them locally to
avoid surprises. The commands below are the **desktop** gates, and CI runs them
with `working-directory: desktop` — so run them from `desktop/` too.

### 1. Formatting (mandatory)

CI rejects the PR if the code is not formatted. Apply formatting with:

```powershell
cd desktop
dotnet format LostieLauncher.slnx
```

CI verifies it with `dotnet format --verify-no-changes`, so make sure no changes
remain pending.

### 2. Build and tests

```powershell
cd desktop
dotnet restore LostieLauncher.slnx
dotnet build LostieLauncher.slnx --no-restore --configuration Release
dotnet test  LostieLauncher.slnx --no-build --configuration Release
```

> Repository policy treats warnings as failures. The test project sets
> `TreatWarningsAsErrors`, and the app escalates `IDE0005` for unused `using`s.
> Not every compiler warning in the app is automatically a build error.

### 3. Vulnerable dependencies

CI fails if there are NuGet packages with known vulnerabilities. You can check
with:

```powershell
cd desktop
dotnet list LostieLauncher.slnx package --vulnerable --include-transitive --format json --output-version 1
```

---

### Android gates

Run from `android/`:

```bash
./gradlew spotlessCheck
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
```

On Windows use `.\gradlew.bat` with the same tasks. The toolchain and gate
details are in [Android build and validation](android/.agents/build-and-validation.md).
The corresponding [desktop document](desktop/.agents/build-and-validation.md)
also explains the JSON vulnerability check used by CI: a successful NuGet
report command is insufficient if that report contains vulnerable packages.

---

## 🧪 Tests

Android unit tests live in `android/app/src/test/` and use JUnit Jupiter,
MockK, Kotest assertions and Turbine. See the
[Android testing rules](android/.agents/testing.md) for coroutine, fixture
and platform boundaries.

Desktop tests live in `desktop/LostieLauncher.Tests/`, whose folder structure
mirrors the production project (`Services/`, `ViewModels/`, `Utils/`, `Models/`,
`Helpers/`) so you can easily locate the matching test.

Test stack:

- **[xUnit v3](https://xunit.net/)** — test framework.
- **[NSubstitute](https://nsubstitute.github.io/)** — test doubles (mocks).
- **[Shouldly](https://docs.shouldly.org/)** — readable assertions.

Guidelines:

- Add or update tests for any behavior change.
- Tests **must not** load view XAML, instantiate windows, or pump a real
  `Dispatcher`. Resource-dependent tests use the shared `WpfApplicationFixture`;
  theme dictionaries are an existing supported exception. Tests must run on
  a headless CI agent.
- The app exposes its internals to the tests via `InternalsVisibleTo`, so you
  can test `internal` types without making them public.

---

## 📦 Dependencies

Dependency updates (NuGet, Gradle and GitHub Actions) are handled automatically by
**Dependabot** through PRs — the `nuget` ecosystem is pointed at `/desktop`,
where the .NET projects live. Thanks to `.github/CODEOWNERS`, the maintainer is
automatically assigned as reviewer on those PRs. Velopack is excluded from
automatic updates and maintained manually. You do not need to update
dependencies unless your change requires it.

---

## ✔️ Acceptance requirements

For a Pull Request to be accepted it **must mandatorily meet all the
requirements described in this document** (formatting, build, tests, absence of
vulnerable dependencies, and the Git workflow). It must also be **free of merge
conflicts** with `development`; if conflicts arise, you are responsible for
rebasing or merging the latest `development` and resolving them. A PR that fails
any of them will not be merged.

As the maintainer, I **reserve the final say** on what gets merged into the
`development` branch. Meeting all requirements is a necessary condition, but does
not automatically guarantee a merge: any contribution may be rejected or have
changes requested for reasons of **quality control**, consistency with the
architecture, or the project's direction.

---

## 🤖 Use of AI

Using AI tools as an **assistant** during development is allowed. That said:

- Contributions **generated by autonomous agents will be rejected
  automatically.**
- AI must be used only as support. **The developer is ultimately responsible**
  for the code they submit: for maintaining good practices, code quality, and
  security.

If you work with a coding assistant, point it at [AGENTS.md](AGENTS.md): it is
the machine-readable version of this guide. It is the single reading index for
shared topics in [`.agents/`](.agents/) and platform topics in
[`desktop/.agents/`](desktop/.agents/) or [`android/.agents/`](android/.agents/).
The topic files contain architecture rules, code conventions, testing constraints
and the commands CI runs. Follow the index links explicitly. Keeping the
assistant inside those rules saves review cycles, but does not transfer
responsibility for the result.

---

## 🐛 Reporting bugs or proposing improvements

Follow [REPORTING.md](REPORTING.md) for a short guide and a copyable example.
If you're going to work on a large change, discuss it first in an issue to
agree on the approach.

---

## Release packaging (maintainer only)

The apps have separate versions. Do not change version fields or README
version badges as part of feature or fix work.

### Windows release

Packaging needs PowerShell 7+ and the Velopack `vpk` CLI installed globally.
From the repository root:

```powershell
.\desktop\scripts\build-release.ps1
```

The script reads the version from the app's project file, publishes a
self-contained `win-x64` build into `desktop/publish/`, and writes packages
and update metadata into `desktop/releases/`. Outputs include full/delta
`.nupkg` packages, `releases.win.json` and `RELEASES`.

For signing and optional server upload, see the parameters and examples in
[`build-release.ps1`](desktop/scripts/build-release.ps1). Velopack must handle
signing both the app and its own binaries.

### Android release

Keep the release keystore outside the repository and back it up along with
its passwords. Every release must use the same key to update installed copies.
To generate a key once:

```bash
keytool -genkeypair -v -keystore /path/outside/repository/lostie-release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias lostie
```

Create the ignored `android/keystore.properties`. A relative `storeFile`
resolves against `android/`:

```properties
storeFile=C:/path/outside/repository/lostie-release.jks
storePassword=...
keyAlias=lostie
keyPassword=...
```

From `android/`, run `./gradlew assembleRelease` (`.\gradlew.bat` on Windows).
With credentials present, Gradle signs `app/build/outputs/apk/release/app-release.apk`
and copies it to `release/LostieLauncher-<versionName>.apk`.
Without credentials, as on CI, it builds `app-release-unsigned.apk` and does
not export it. Use `signingReport` to inspect the variant keys.

Publish in this order:

1. Increase both `versionCode` and `versionName` in
   [`app/build.gradle.kts`](android/app/build.gradle.kts).
2. Build the release with the existing release key.
3. Upload the APK from `android/release/`.
4. Update `public/android/latest.json` with the new `versionName` and HTTPS
   `downloadUrl` only after the APK is available.

Self-update compares `versionName`; Android also rejects a downgrade in
`versionCode`. Changing either alone is insufficient.

---

Thanks for contributing! 🎮
