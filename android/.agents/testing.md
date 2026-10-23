# Android testing

Apply the [shared testing rules](../../.agents/testing.md).
Tests live in `app/src/test/kotlin/`, mirroring production packages, normally
one `<Type>Test.kt` per type.

- Use JUnit Jupiter on the JUnit Platform, MockK, Kotest assertions, Turbine
  and `kotlinx-coroutines-test`. The catalogue currently selects JUnit 6;
  use [libs.versions.toml](../gradle/libs.versions.toml) for actual versions.
- The `de.mannodermaus.android-junit` plugin enables this runner for Android
  unit tests. Preserve that integration when changing build configuration.
- JVM unit tests are the automated CI suite: no Robolectric, `androidTest/`
  or emulator-based CI tests. This does not replace manual device acceptance
  for platform behavior that JVM tests cannot exercise.
- Use JUnit `@TempDir` for filesystem cases and a loopback `MockWebServer`
  for HTTP-stack tests such as `CdnPayloadTest`.
- Pin `Clock` with `Clock.fixed`. Use `runTest` and injected test dispatchers;
  assert flows with Turbine.
- Use `TestDispatcherProvider` and the existing service seams. For example,
  `ArchiveGameInstallationServiceTest` exercises extraction under a temporary
  root; it does not need Android storage permissions or an installed Kirin.

## Test shape

- Use `@DisplayName` on the class and descriptive sentences in backticks for
  test names.
- Dependencies are `private val` mocks and `createSut()` constructs the subject.
- Separate arrange, act and assert with blank lines, without narration comments.
- Prefer `@ParameterizedTest` for parallel cases.
- When porting a desktop case, preserve its behavior or record why the Android
  contract differs. Historical correspondence and search rationale live in
  [test parity](../docs/test-parity.md) and [search parity](../docs/search-parity.md).

## Device acceptance

Exercise changed platform flows on a device/emulator when relevant: storage
access, Kirin intents, notifications, background downloads, APK installation,
ICU regex behavior, rotation and adaptive UI. Report what was actually run.
Keep these manual checks distinct from the JVM CI gates.

Use [build-and-validation.md](build-and-validation.md) for commands. Kotlin
warnings are errors in tests too; do not silence them to obtain a green run.
