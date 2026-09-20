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
  dependency, no `Thread.sleep`. A loopback `MockWebServer` is not "real
  network" and is the right tool when what is under test is the HTTP stack
  itself — `CdnPayloadTest` uses one to parse the captured CDN payloads through
  the real OkHttp, Retrofit and `Json`, which is the only way to prove the
  payload parses rather than prove that a fixture does.
- **Pin the clock.** Anything that reads "now" takes the injected
  `java.time.Clock`, and a test hands it `Clock.fixed`. A test asserting against
  a payload that expires on a date is otherwise a test with a fuse in it.
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
  making sense on Android, do not delete it silently: say so in the test file,
  in the PR, and in [Desktop test parity](#desktop-test-parity-utils), which is
  where the answer is looked up next time. Port plan step 06 is explicit about
  this.

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

## Desktop test parity: `Utils/`

Port plan step 06 ported the desktop's **pure decision functions** — the ones
with no I/O, no mutable state and no platform dependency — together with their
test cases, and owed a documented correspondence in return. This is it. It
covers `desktop/LostieLauncher.Tests/Utils/` only; the other folders belong to
steps 04, 07, 08 and 10, which each carry their own table.

Counts are `[Fact]` + `[Theory]` **declarations**, not executed cases: a theory
with five rows is one declaration on both sides. The desktop's 24 classes
declare 163 of them, of which 62 are here, beside 37 that are this side's own.

### Ported

| Desktop class | Decl. | Android | Ported | Added |
| --- | --- | --- | --- | --- |
| `VersionUtilsTests` | 15 | `util/version/VersionUtilsTest` | 15 | — |
| `LinkTextParserTests` | 9 | `util/text/LinkTextParserTest` | 9 | — |
| `DownloadPathUtilsTests` | 9 | `util/download/DownloadPathUtilsTest` | 9 | 1 |
| `DownloadCachePolicyTests` | 7 | `util/download/DownloadCachePolicyTest` | 7 | 2 |
| `SearchMatcherTests` | 7 | `util/text/SearchMatcherTest` | 7 | 18 |
| `FileFinalizerTests` | 6 | `util/file/FileMoveRetryPolicyTest` | 2 | 2 |
| `PlaytimeFormatterTests` | 4 | `util/format/PlaytimeFormatterTest` | 4 | — |
| `ShutdownWarningPolicyTests` | 4 | `util/policy/ShutdownWarningPolicyTest` | 4 | — |
| `UrlLauncherTests` | 3 | `util/net/HttpsUrlsTest` | 3 | 9 |
| `UnhandledExceptionPolicyTests` | 2 | `util/policy/UnhandledExceptionPolicyTest` | 2 | — |
| — | — | `util/version/BaseVersionTest` | — | 5 |

Three of those rows need a word.

- **`FileFinalizerTests` — 2 of 6.** Only `IsRetryable` is pure. The four
  `MoveAsync` declarations drive a real file through a real move with a real
  lock on it; that is the filesystem half of the same type and it arrives with
  the download transfer in step 08. The desktop keeps the two apart for the same
  reason, and so does this side: `FileMoveRetryPolicy` is the predicate and
  nothing else.
- **`UrlLauncherTests` — 3 of 3.** The desktop class only ever covered
  `TryGetHttpsUri`; `OpenHttps` shells out and has no test there either. The
  guard became `HttpsUrls`, and the shelling out lands with the screens that
  need it.
- **`DownloadPathUtilsTests` — paths adapted.** Two cases assert a derived path
  and did so with a Windows one. Both functions are string concatenation, so the
  shape proves nothing either way, and a `C:\` path in a test on this side would
  be misleading rather than faithful.

The **Added** column is Android-only coverage — 37 declarations, more than half
the ported count — and every one exists for the same reason: something .NET
supplies was hand-written here, so the rule it implements has to be pinned on
this side rather than inherited.

- `BaseVersionTest` — `System.Version`'s grammar and its `-1`-for-absent
  comparison.
- `SearchMatcherTest` — the folding that stands in for .NET's collation-aware
  `IndexOf`: which characters carry no weight, the middle-dot contraction the
  two Catalan-family catalogues are full of, and where a match stops when the
  next character was dropped.
- `HttpsUrlsTest` — the canonical form .NET gets from `Uri.AbsoluteUri`, the
  URLs `java.net.URI` refuses and `Uri.TryCreate` accepts, and the three places
  the canonical form still differs.
- `FileMoveRetryPolicyTest` — the exception mapping the JVM forced.
- `DownloadPathUtilsTest` — the cache token against the digest the desktop
  computes, because a cache written by one side has to be read by the same
  rules on the other.

### How the collation rules were established

Not by reading a specification. A probe ran `CompareInfo.Compare` under
`IgnoreCase | IgnoreNonSpace` over **every BMP code point** and over each
boundary case, and the implementation follows what came back:

- Two Unicode categories are ignorable in full (`Format`, `EnclosingMark`) and
  one nearly so (`Control`, except the six that collate as whitespace).
  `NonSpacingMark` is *not* — 439 of 1066 carry weight — so dropping the
  category is slightly over-broad, and correct for every Latin-script language
  the launcher ships.
- The middle dot of a Catalan `l·l` is a **contraction**, not an ignorable
  character: `l·l` equals `ll` while `a·b` does not equal `ab`. Its condition is
  the **left neighbour alone** — swept over every ordered pair of a 39-character
  alphabet, the dot is dropped in 39 of 1521, exactly those preceded by `l` or
  `L`, whatever follows and even at the end of the string. Two wrong rules are
  easy to reach from here and both were: dropping the dot unconditionally
  (`a·b` would match `ab`), and requiring an `l` on both sides, which silently
  blanks the FAQ list on the keystroke where a Catalan user has typed `instal·`.
  The neighbour is read literally, not through the folding — an accent between
  the `l` and the dot blocks the contraction on the desktop too.
- A match extends over a trailing character only when it belongs to the same
  grapheme — combining marks and the two joiners, not a soft hyphen, a
  byte-order mark, a word joiner or a control.

The result was then checked end to end rather than assumed. A differential ran
both implementations — .NET's `CompareInfo` and the **compiled** Kotlin class,
not a re-implementation of it — over **328,051 (text, term) pairs** in three
corpora, all drawn from the eight catalogues, the FAQ entries and both captured
CDN payloads. Zero disagreements in all three.

| Corpus | Pairs | Matching |
| --- | --- | --- |
| Terms derived from each text | 64,736 | 64,736 |
| Cross product, so most pairs are negatives | 208,962 | 2,047 |
| **Every prefix of every word**, plus a sweep of both neighbours of a middle dot | 54,353 | 46,161 |

The third corpus is there because the first two could not have caught the
both-sides bug: no shipped string contains `l` + dot + non-`l`, so only a
half-typed search term produces one. A corpus built from whole words is not a
corpus of what a keystroke filter is handed.

The same was done for `LinkTextParser` over 912 texts: zero disagreements on the
shipped corpus, and three on deliberately awkward synthetic URLs, all of them in
the link *target* rather than in whether the text is a link, and all three
pinned by `HttpsUrlsTest`.

### Not ported, and why

| Desktop class | Decl. | Why not |
| --- | --- | --- |
| `LogsMaintenanceTests` | 15 | Log rotation and retention are **step 07's**, which is asked to decide how file logging materialises on Android. Porting the desktop's month-and-index naming now would pre-empt that decision rather than serve it. |
| `DirectoryRemoverTests` | 11 | Recursive deletion with a reparse-point guard — filesystem, and the uninstall flow it serves does not exist yet. |
| `OneDrivePathPolicyTests` | 9 | `spec/10-windows-only.md` §9: OneDrive detection is dropped, along with `OneDriveWarning*`. The variables and the folder convention are Windows. A cloud-backed provider behind the storage access framework would be a different check with different evidence, not this one ported. |
| `DownloadDirectoryProbeTests` | 9 | The write-and-rename pre-flight. `spec/10` §8 keeps the *principle* and re-derives the check once the storage model is decided — step 07. |
| `FileMoveDiagnosticsTests` | 8 | Reads attributes and lock state off a real path, and decodes a 13-entry Win32 error table. |
| `WaitHandleSignalListenerTests` | 7 | A named kernel object. No counterpart, and `spec/09` says not to look for one. |
| `GameArchiveInstallerTests` | 6 | Extract, swap, roll back — filesystem, and step 08 decides which archive formats survive. |
| `FileLockProbeTests` | 5 | `spec/10` §11: the three-valued answer is worth keeping, how the middle value is obtained is step 09's and may not exist. |
| `FolderLauncherTests` | 5 | Opens a folder in the shell. |
| `LogsTests` | 5 | The log line's own format. Same owner as `LogsMaintenanceTests`: step 07. |
| `ProcessUtilsTests` | 5 | Launching and tracking a game process — the step 09 seam, and out of scope for the whole port. |
| `StartupWindowPolicyTests` | 4 | There is no window, and the setting it branches on is gone: `StartMinimized` is dropped in `spec/10`'s settings-survival table. The function has no input it could be given. |
| `AsyncEventHandlerTests` | 4 | A .NET idiom, not a decision. The invariant it protects — a failure in an exit handler must not escape — carries over; the wrapper does not. |
| `DownloadArtifactsTests` | 4 | Deletes the three files of a download. Filesystem, step 08. |
| `FileFinalizerTests` (`MoveAsync`) | 4 | See above. |

Nothing in that table is a case that stopped being interesting. Each is either
**owned by a later step**, or **has no input on Android** — and those are
different, so they are worth telling apart when one of these tables is read
again at step 15.
