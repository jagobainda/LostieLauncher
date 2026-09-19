# Testing

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before writing or editing any test.

Tests live in `app/src/test/kotlin/`, mirroring the production packages, one
`<Type>Test.kt` per production type. Stack: **JUnit 5** (`junit-jupiter`),
**MockK**, **Kotest assertions**, **Turbine** for flows, and
`kotlinx-coroutines-test`.

The desktop's stack is xUnit v3 + NSubstitute + Shouldly, and the mapping is
deliberate: `[Fact]` → `@Test`, `[Theory]` + `[InlineData]` → `@ParameterizedTest`
+ `@CsvSource`/`@MethodSource`, `Substitute.For<T>()` → `mockk()`,
`result.ShouldBe(x)` → `result shouldBe x`, `Received(1)` → `verify(exactly = 1)`.
A test ported from the desktop should be recognisable beside its original.

## Hard rules

- **No device, ever.** JVM unit tests only. No Robolectric, no
  `androidTest/`, no emulator in CI. A test that needs a device is a test CI
  cannot run, and it is a sign the seam is missing — add the interface instead.
- **Nothing real on the other side of a seam.** No real network, no filesystem
  outside a JUnit `@TempDir`, no wall-clock dependency, no machine locale
  dependency, no `Thread.sleep`.
- Coroutines are tested with `runTest` and an injected test dispatcher, never by
  waiting. `DispatcherProvider` exists precisely so a test can hand a
  `StandardTestDispatcher` to the thing under test.
- Flows are asserted with Turbine, not by collecting into a list and hoping the
  timing works out.
- **Never widen visibility to make something testable.** Unit tests are in the
  same module and already see `internal`.
- Add or update tests for **every** behaviour change, including the failure path
  you just fixed. A bug fix without a regression test is incomplete.
- When a test is ported from the desktop, keep the case. If a desktop case stops
  making sense on Android, do not delete it silently: say so in the test file
  and in the PR. Port plan step 06 is explicit about this.

## Shape of a test class

```kotlin
@DisplayName("ContentService")
class ContentServiceTest {
    private val api = mockk<ContentApi>()
    private val logger = mockk<Logger>(relaxed = true)

    private fun createSut() = ContentService(api, logger)

    @Test
    fun `returns an empty catalogue and logs when the content server fails`() = runTest {
        // Arrange — the CDN is down, which must not take the app with it.
        coEvery { api.games() } throws IOException("boom")
        val sut = createSut()

        // Act
        val games = sut.getGames()

        // Assert
        games.shouldBeEmpty()
        verify(exactly = 1) { logger.error(any(), any()) }
    }
}
```

- Names are a sentence in backticks describing scenario and expected result.
  `@DisplayName` on the class names the unit under test.
- Collaborators are `private val` MockK mocks; the system under test comes from
  a `createSut()` factory so each test arranges first.
- Keep the `// Arrange` / `// Act` / `// Assert` comments and use them to say
  *why*, not to restate the code.
- Group related cases behind a `// ---- section ----` comment, as the desktop
  files do.
- Prefer `@ParameterizedTest` over copy-pasting a `@Test`.

## Running them

From `android/`, never from the repository root:

```bash
./gradlew testDebugUnitTest
./gradlew testDebugUnitTest --tests "*ContentServiceTest*"
```

The suite runs on the JUnit Platform through the
`de.mannodermaus.android-junit` plugin, which is what teaches AGP to run JUnit 5
for Android unit tests — AGP does not do it on its own. If that plugin ever
blocks an AGP upgrade, the fallback is JUnit 4, not an older AGP.

The module compiles with `allWarningsAsErrors`, tests included. Do not add a
suppression to make a test compile.
