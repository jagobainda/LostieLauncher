# Agent guidelines — Android

Instructions for AI coding agents working on the **native Android app**. This
file is the index for this side of the monorepo: it holds the rules that apply
to **every** Android change, and routes you to a topic file for the rest.

Read [the global guidelines](../AGENTS.md) first — English-only, branching, PR
target, scope and the shared `.github/` infrastructure are defined there and are
not repeated here. Then read the topic file **before** you write the
corresponding code. The links are plain Markdown on purpose — the detail is
loaded on demand, not injected into every session — so following them is your
job, not the tool's.

| Read this before…                                          | File                                                                     |
| ----------------------------------------------------------- | ------------------------------------------------------------------------ |
| adding a service, ViewModel, Hilt binding or seam           | [.agents/architecture.md](.agents/architecture.md)                       |
| writing or editing any `.kt` or `.kts` file                 | [.agents/code-style.md](.agents/code-style.md)                           |
| writing or editing any test                                 | [.agents/testing.md](.agents/testing.md)                                 |
| adding user-visible text or touching the theme              | [.agents/localization-and-themes.md](.agents/localization-and-themes.md) |
| branching, committing or opening a PR                       | [../.agents/workflow.md](../.agents/workflow.md)                         |

This app is a port of the desktop launcher's **behavior**, not of its code. The
behavioural contract is [`../spec/`](../spec/); where the spec and
[`../desktop/`](../desktop/) disagree, **the desktop code wins** and the drift is
a bug in the spec. The human-facing docs are
[CONTRIBUTING.md](../CONTRIBUTING.md) and [README.md](README.md).
The deliberately pending game installation and launch contract is documented in
[docs/game-runtime-options.md](docs/game-runtime-options.md).

## Non-negotiables

These apply to every Android change, with no topic file to look up:

1. Run the gates locally before proposing a change. See [Commands](#commands).
2. Warnings are failures. `allWarningsAsErrors` on the Kotlin compiler, and
   Android Lint runs with `warningsAsErrors` and `abortOnError`.
3. Tests never need a device: JVM unit tests only, no Robolectric, no
   `androidTest/`. If something cannot be tested without one, add the seam.
4. Never widen visibility to make code testable — the tests are in the same
   module and already see `internal`.
5. A new user-visible string means **all 8 languages**. A new colour role means
   **all 10 themes**. See [spec/05](../spec/05-localization.md) and
   [spec/06](../spec/06-design-tokens.md).
6. No version literal in a build script — it goes in
   [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
7. No URL, path or magic number inside the type that does the work: it arrives
   injected, as it does on the desktop.
8. Branch off `development`; PRs target `development`, never `main`.
9. Everything you write is in **English**: code, comments, KDoc, commits, branch
   names, PR text.
10. Stay inside the scope of what was asked. You assist; the developer opening
    the PR is responsible for the result. See
    [Boundaries](../.agents/workflow.md#boundaries).
11. Do not touch `../desktop/`. It is a shipping application with its own CI
    gates, and nothing on this side needs to modify it.

## Project shape

Kotlin, Jetpack Compose, **MVVM**, a single Hilt object graph. One Gradle
module, `:app`; the layer contract is enforced by package, and it mirrors the
desktop's layer for layer.

```
android/
├── .agents/          # the topic files linked above
├── docs/             # open game runtime decisions and their consequences
├── .editorconfig     # Kotlin and Gradle rules (inherits the monorepo baseline)
├── build.gradle.kts  # plugin versions + the Spotless/ktlint gate
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml            # every version in the build lives here
│   ├── gradle-daemon-jvm.properties  # pins the daemon's JDK (generated)
│   └── wrapper/                      # pinned Gradle distribution + checksum
├── gradlew, gradlew.bat
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/kotlin/dev/jagoba/lostielauncher/
        │   ├── core/        # composition root: Hilt modules, dispatchers
        │   ├── model/       # immutable domain models, enums, options
        │   ├── service/     # service layer + the interfaces isolating the platform
        │   ├── content/     # localized text catalogue, eight languages
        │   ├── util/        # pure decision functions, formatting, logging
        │   └── ui/          # MainActivity, screen/ component/ dialog/ theme/ viewmodel/
        ├── main/res/        # only what the platform reads before Kotlin runs
        ├── debug/kotlin/    # debug-only surfaces — the token catalogue
        ├── release/kotlin/  #   and their release counterparts, so neither ships the other
        └── test/kotlin/     # JVM unit tests, mirroring the packages above
```

Which layer may depend on which, and why `Converters/` has no counterpart:
[.agents/architecture.md](.agents/architecture.md).

## Commands

> **Run these from `android/`, not from the repository root.** The Gradle build
> and its wrapper live here, the same way `desktop/global.json` pins the .NET
> toolchain on the other side. The CI jobs use `working-directory: android`.

These are exactly the two Android jobs in
[`.github/workflows/ci.yml`](../.github/workflows/ci.yml). A PR that fails
either will not be merged.

```bash
# 1. Formatting — CI runs `spotlessCheck`, so leave nothing pending
./gradlew spotlessApply

# 2. Build, test and lint
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Useful while iterating:

```bash
./gradlew testDebugUnitTest --tests "*ContentServiceTest*"
./gradlew assembleRelease     # the R8/minified path, worth checking before a PR
```

Notes:

- **The JDK is pinned to Temurin 17, twice, and both pins are load-bearing.**
  `gradle/gradle-daemon-jvm.properties` pins the JVM the **daemon** runs on, and
  the toolchain in `app/build.gradle.kts` pins the one the code is **compiled**
  with. Gradle downloads a matching JDK if the machine has none; the foojay
  resolver in `settings.gradle.kts` is what makes the second one resolvable.
  Whatever `JAVA_HOME` points at only has to be able to start the Gradle
  launcher (17–26).
  Do not remove either pin to "simplify". A JetBrains Runtime — what a JetBrains
  IDE ships with, and a plausible `JAVA_HOME` on a dev machine — breaks the
  build twice over: AGP's `JdkImageTransform` needs `jlink`, which JBR does not
  ship, and KSP's worker dies on JBR 25 with
  `NoClassDefFoundError: ...PluginEnabler`. Both were hit while setting this up;
  both are why the pins exist.
- The Gradle wrapper pins the distribution **and its SHA-256**. Do not remove
  `distributionSha256Sum` when bumping it.
- The formatting rules are duplicated between `.editorconfig` and
  `build.gradle.kts` because Spotless does not read the former. Both are
  commented; change one, change the other.
- The Android app has **its own version** (`versionCode` / `versionName`),
  unrelated to the desktop's. Never bump it as part of a feature or fix.

## Before you open a PR

- [ ] Commands were run from `android/`.
- [ ] `./gradlew spotlessApply` leaves nothing pending.
- [ ] `assembleDebug`, `testDebugUnitTest` and `lintDebug` are all green, with
      no warnings.
- [ ] New behavior is covered by tests, and no test needs a device.
- [ ] New strings exist in all 8 languages; new colour roles in all 10 themes.
- [ ] New services and ViewModels are bound in a Hilt module, not constructed at
      a call site.
- [ ] No hardcoded URLs, paths, colours or user-visible literals.
- [ ] New versions went into `gradle/libs.versions.toml`, not into a build script.
- [ ] No version bump, and no unrelated file touched — `../desktop/` included.
- [ ] Everything written in English.
