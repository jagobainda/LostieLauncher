# Android build and validation

Run commands from `android/`, using the committed Gradle wrapper.
[CI](../../.github/workflows/ci.yml) runs on Linux with Temurin 17.

## Full CI gates

```bash
./gradlew spotlessCheck
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
```

On Windows PowerShell use `.\gradlew.bat` with the same task names.
To apply formatting, run `spotlessApply`, then verify with `spotlessCheck`.
Target one JVM suite while iterating with:

```bash
./gradlew testDebugUnitTest --tests "*ContentServiceTest*"
```

- Kotlin sets `allWarningsAsErrors`; Android Lint sets `warningsAsErrors` and
  `abortOnError`. Keep the full gate, including release assembly and R8.
- Reports are written under `app/build/reports/` and `app/build/test-results/`.
- `installDebug` requires a running emulator or connected device. It is a manual
  development command, not a CI gate or proof that acceptance checks passed.

## Toolchain and build configuration

- `gradle/gradle-daemon-jvm.properties` requests Adoptium JDK 17 for the daemon.
  The app's Java toolchain requests language level 17 via `javaTarget`; it does
  not separately pin a vendor. Preserve both configurations.
- Use a compatible JDK to start the wrapper. Gradle's daemon criteria and
  the Foojay resolver in `settings.gradle.kts` resolve the configured JDKs;
  do not remove them to work around a local runtime problem.
- Preserve the wrapper's `distributionSha256Sum`.
- Library and ordinary plugin versions belong in `gradle/libs.versions.toml`.
  The Foojay settings plugin has an existing literal in `settings.gradle.kts`;
  app `versionCode` / `versionName` are release metadata in the app build file.
- `android/.editorconfig` configures the IDE; the explicit `ktlintSettings`
  map in `build.gradle.kts` configures Spotless overrides. Keep duplicated
  settings synchronized.
- A debug APK has an `.debug` application ID suffix. Without
  `keystore.properties`, release assembly is unsigned; with it, the build signs
  and exports the APK. Keep signing credentials outside version control.
- Release/version changes are the maintainer's workflow in
  [CONTRIBUTING.md](../../CONTRIBUTING.md#android-release),
  not part of a feature or fix.
