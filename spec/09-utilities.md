# 09 · Utilities — the decision functions and their tests

Extracted from `desktop/LostieLauncher/Utils/` and
`desktop/LostieLauncher.Tests/`.

The desktop keeps a deliberate split: anything branchy is extracted into a
static function with values in and a value out, and the I/O that feeds it stays
outside. That is what makes 528 unit tests possible without a desktop session,
and it is the split the Android port is asked to preserve.

This section is the source for **step 06** (port the pure functions with their
test cases) and a reference for **step 10** (port the ViewModel tests).

## Classification

| Utility | Pure? | Port in step 06? |
| --- | --- | --- |
| `VersionUtils` | yes | yes |
| `PlaytimeFormatter` | yes | yes |
| `SearchMatcher` | yes | yes |
| `LinkTextParser` | yes | yes |
| `DownloadCachePolicy` | yes | yes |
| `DownloadPathUtils` | yes | yes |
| `ShutdownWarningPolicy` | yes | yes |
| `StartupWindowPolicy` | yes | yes |
| `UnhandledExceptionPolicy` | yes | yes |
| `OneDrivePathPolicy.IsSynced` | yes (roots are supplied) | decide — see section 10 |
| `FileFinalizer.IsRetryable` | yes | yes, the predicate only |
| `FileMoveDiagnostics` | mostly | the Win32 mapping is Windows-only |
| `AsyncEventHandler` | no — an idiom | no |
| `DirectoryRemover` | no — filesystem | adapt |
| `DownloadArtifacts` | no — filesystem | adapt |
| `DownloadDirectoryProbe` | injectable, tested with fakes | adapt |
| `FileFinalizer.MoveAsync` | injectable delay, real filesystem | adapt |
| `FileLockProbe` | no — filesystem | see section 10 |
| `GameArchiveInstaller` | no — filesystem | adapt |
| `FolderLauncher`, `UrlLauncher` | no — shell | `UrlLauncher`'s URI guard is pure |
| `ProcessUtils` | seam-injected | see section 10 |
| `WaitHandleSignalListener` | no — named kernel object | no, see section 10 |
| `KeyboardShortcuts` | no — WPF input | no |
| `Logs` | no — filesystem; three internals are pure | adapt; port the pure three |

---

## The pure functions, exactly

### `VersionUtils`

- **`IsNewerVersion(remote, local)`** — parse both; if either fails, log and
  return false. Otherwise return `remote > local`.
- **`ParseBaseVersion(version)`** — null or blank → nothing. Strip leading `v`
  or `V`. Cut at the first `-`. Parse as a dotted numeric version; failure →
  nothing.
- **`FormatDisplayVersion(version)`** — null or blank → `"unknown"`. Trim,
  strip leading `v`/`V`; if nothing is left → `"unknown"`; otherwise `"v" + rest`.

Edge cases that matter: `1.2` is **lower** than `1.2.0`, an unparseable version
never produces an update prompt, and `"v"` alone displays as `"unknown"`.

### `PlaytimeFormatter.Format(minutes)`

| Input | Output |
| --- | --- |
| `<= 0` | `""` (empty — the card hides the whole playtime row) |
| `< 60` | `"{n} min"` |
| whole hours | `"{h} h"` |
| otherwise | `"{h} h {m} min"` |

The unit labels are hardcoded English abbreviations and are **not** localized.

### `SearchMatcher`

- `Contains(text, term)` → whether `FindMatches` returns anything.
- `FindMatches(text, term)` → the list of `(start, length)` matches.

Rules: empty text or blank term → no matches. Comparison uses the **invariant
culture** with *ignore case* and *ignore non-spacing marks*, so accents are
transparent. Matching is non-overlapping, scanning forward from the end of each
match. **The matched length can differ from the term's length** — the
diacritic-insensitive comparison can match a different number of characters —
which is why the function returns lengths rather than assuming them.

### `LinkTextParser.Parse(text)`

Splits text into segments, each either plain or a link.

The pattern matches either a literal `https://` followed by non-whitespace, or a
bare domain: `www.` plus labels, **or** dotted labels ending in one of
`com net org es eu io gg dev app me co tv info`, optionally followed by a path.
The bare-domain branch is guarded by a look-behind rejecting a preceding word
character, `@`, `.`, `/` or `-`, which keeps it from firing inside an email
address or mid-URL.

Then, per match: trim trailing `. , ; : ! ? ) ] " '` from the candidate; skip it
if nothing is left; prefix `https://` when it is not already there; and **drop
the match entirely if the result is not an absolute HTTPS URI**. The displayed
text is the trimmed candidate, and the link target is the absolute URI.

Two consequences: trailing punctuation stays in the plain text rather than being
swallowed into the link, and only HTTPS is ever produced — `http://` links do
not survive the check.

### `DownloadCachePolicy.SelectStaleFiles(entries, knownGameIds, nowUtc, maxAge)`

`DefaultMaxAge` is **14 days**. Only files ending in `.zip`, `.part` or
`.part.meta` are considered; anything else in the folder is ignored entirely
rather than deleted.

A managed file is stale when **any** of:

1. It is a `.part.meta` whose matching `.part` is not present — an orphaned
   sidecar.
2. `nowUtc - lastWriteTimeUtc > maxAge`.
3. Its game id is not in the known set. The id is the substring before the
   **first** `.`; a name with no dot, or one starting with a dot, yields the
   empty string, which is never a known id and is therefore always stale.

All comparisons on names and ids are case-insensitive. The function only
*selects* — the caller deletes, counting successes and logging each failure.

### `DownloadPathUtils`

- `GetZipFileName(args)` → `"{gameId}.{token}.zip"`.
- `ComputeToken(version, key)` → the **first 8 bytes** of the SHA-256 of
  `"{version}|{key ?? ""}"`, hex, **lowercase** — 16 characters.
- `GetPartFilePath(final)` → `final + ".part"`.
- `GetMetaFilePath(part)` → `part + ".meta"`.

The token is what keeps two versions, or a keyed and an unkeyed download of the
same version, from colliding in the cache.

### The three one-line policies

| Function | Rule |
| --- | --- |
| `ShutdownWarningPolicy.Decide(isDownloading, isGameRunning)` | `(true,true)` → `Both`, `(true,false)` → `Download`, `(false,true)` → `Game`, else `None` |
| `StartupWindowPolicy.ShouldShowOnStartup(startMinimized, hasSeenWelcome)` | `!startMinimized \|\| !hasSeenWelcome` |
| `UnhandledExceptionPolicy.Decide(startupCompleted)` | `KeepAlive` when startup finished, `Fatal` before |

### `OneDrivePathPolicy.IsSynced(path, syncRoots)`

Pure because the caller supplies the roots; the environment lookup lives in a
separate method that reads `OneDrive`, `OneDriveConsumer` and
`OneDriveCommercial`.

A path is synced when, after normalizing both sides (full path, trailing
separator trimmed; a path that cannot be normalized is logged and treated as not
matching):

- it equals a root, **or** starts with a root plus a separator — the separator
  guard is what keeps `C:\OneDriveBackup` from matching the root `C:\OneDrive`;
- **or** any of its segments is exactly `OneDrive` or starts with
  `"OneDrive - "`, which catches a second account or a hand-repointed folder the
  environment does not advertise.

### `FileFinalizer.IsRetryable(error, destinationState)`

`false` when the destination is a directory or a read-only file — neither
resolves by waiting. Otherwise `true` only for `UnauthorizedAccessException` or
`IOException`.

### `Logs` — the three pure internals

- `BuildLogFileName(month, index)` → `"{month}.log"` at index 0 or below,
  `"{month}.{index}.log"` above.
- `TryParseLogIndex(fileName, month, out index)` → recognises both forms for
  that month; the numeric part must be a positive plain integer.
- `SelectExpiredLogFiles(fileNames, now, retentionMonths)` → the files whose
  `yyyy-MM` prefix is strictly before the first of the month, `retentionMonths`
  back.
- `ResolveActiveFile(...)` is also pure: it decides whether to keep the current
  file, roll to the next index, or probe the directory afresh for a new month.

---

## Utilities that touch the platform

Recorded because the port has to re-implement them, not translate them.

**`DirectoryRemover.Delete(path, maxAttempts = 3, retryDelay = 300 ms)`** —
recursive deletion that reports *what blocked it*. Per attempt: if the path is
gone, succeed. A **reparse point is removed without recursing into it**, so a
junction or symlink never causes a delete outside the tree. Otherwise delete
every file, recurse into every subdirectory, then remove the directory; each
deletion clears the read-only attribute first, and each failure is wrapped with
the exact blocking path. Between attempts it backs off linearly. The result
carries whether it succeeded, how many attempts it took, the blocking path, the
error, and — the field the uninstall flow depends on — **how many entries it did
manage to delete**.

**`GameArchiveInstaller.ExtractAsync(zip, dir)`** — extract to `<dir>.tmp`,
then swap. It refuses to start if a leftover `.tmp` cannot be cleared. Entries
are read with UTF-8 filename encoding, directories skipped, and every
destination path is checked to stay inside the temp folder — a **Zip Slip**
guard that throws on the first escaping entry. The swap moves the existing
directory to `<dir>.old`, moves the temp directory into place, and **rolls back
the old one if that move fails**, then deletes the backup. On any failure the
temp directory is removed. The ZIP is deleted after a successful install.

The archive reader handles ZIP and 7z on desktop. Whether both are needed on
Android is a step 08 question.

**`DownloadArtifacts.Delete(zipPath)`** — deletes `.part`, the ZIP and
`.part.meta`, returning how many existed and were removed. Each failure is
logged and counted as zero rather than thrown.

**`DownloadDirectoryProbe.Run(directory)`** — described in section 03. Its
internal form takes create, write, rename and delete as parameters, which is how
it is tested without a filesystem; the public form binds the real ones.

**`FileLockProbe.IsLockedByAnotherProcess(path)`** — a missing path is not
locked; an exclusive open that throws `IOException` is locked; any other
exception is not.

**`FileMoveDiagnostics.Describe(...)`** — a single log line naming both paths,
their kind, size, attributes and lock state, whether the destination directory
exists, the decoded Win32 error and the exception. The Win32 decoding table
covers 13 codes and is Windows-only.

**`AsyncEventHandler.Wrap(callback)`** — turns an async callback into an event
handler whose body can never throw into the event source, logging instead, with
even the logging guarded. The Android idiom is different, but the invariant is
not: a failure in a process-exit handler must not escape.

---

## Desktop test inventory

**528 tests, 0 skipped.** Counts below are `[Fact]` plus `[Theory]`
declarations and, separately, the `[InlineData]` rows those theories carry — so
the executed count is higher than the declaration count.

Step 06 is asked for a documented correspondence between these and their Android
equivalents, including any case deliberately not ported and why. This table is
the starting list.

### Utils — the step 06 scope

| Test class | Declarations | InlineData rows |
| --- | --- | --- |
| `VersionUtilsTests` | 15 | 20 |
| `LogsMaintenanceTests` | 15 | 12 |
| `DirectoryRemoverTests` | 11 | 0 |
| `DownloadDirectoryProbeTests` | 9 | 2 |
| `DownloadPathUtilsTests` | 9 | 0 |
| `LinkTextParserTests` | 9 | 5 |
| `OneDrivePathPolicyTests` | 9 | 10 |
| `FileMoveDiagnosticsTests` | 8 | 4 |
| `DownloadCachePolicyTests` | 7 | 0 |
| `SearchMatcherTests` | 7 | 3 |
| `WaitHandleSignalListenerTests` | 7 | 0 |
| `FileFinalizerTests` | 6 | 3 |
| `GameArchiveInstallerTests` | 6 | 0 |
| `FileLockProbeTests` | 5 | 3 |
| `FolderLauncherTests` | 5 | 3 |
| `LogsTests` | 5 | 3 |
| `ProcessUtilsTests` | 5 | 0 |
| `AsyncEventHandlerTests` | 4 | 0 |
| `DownloadArtifactsTests` | 4 | 0 |
| `PlaytimeFormatterTests` | 4 | 11 |
| `ShutdownWarningPolicyTests` | 4 | 0 |
| `StartupWindowPolicyTests` | 4 | 0 |
| `UrlLauncherTests` | 3 | 10 |
| `UnhandledExceptionPolicyTests` | 2 | 0 |

### Models

| Test class | Declarations | InlineData rows |
| --- | --- | --- |
| `GameInfoTests` | 19 | 8 |
| `InstalledGameInfoTests` | 7 | 4 |
| `SpecialVersionConfigTests` | 7 | 5 |
| `DownloadResultTests` | 5 | 0 |

### Services — the step 04, 07 and 08 scope

| Test class | Declarations | InlineData rows |
| --- | --- | --- |
| `ContentServiceTests` | 53 | 0 |
| `DownloadServiceTests` | 19 | 0 |
| `SettingsServiceTests` | 17 | 6 |
| `UpdateServiceTests` | 10 | 0 |
| `WindowsStartupServiceTests` | 3 | 2 |
| `DownloadLocationServiceTests` | 2 | 0 |

### ViewModels — the step 10 scope

| Test class | Declarations |
| --- | --- |
| `LibraryViewModelTests` | 33 |
| `SettingsViewModelTests` | 30 |
| `GamesViewModelTests` | 29 |
| `HomeViewModelTests` | 14 |
| `GlobalViewModelTests` | 10 |
| `MainViewModelTests` | 8 |
| `FaqsViewModelTests` | 6 |

### Test infrastructure worth reproducing in spirit

| Helper | Purpose |
| --- | --- |
| `FakeHttpMessageHandler` | canned HTTP responses plus request inspection |
| `HttpClientFactoryStub` | a client factory keyed by the production client name |
| `TestServiceProviderBuilder` | a container mirroring production with doubles, overridable per test |
| `TempDirectoryFixture` | an isolated, self-deleting scratch directory |
| `PropertyChangedRecorder` | asserting change-notification chains |
| `ViewModelTestBase` | a deterministic synchronization context plus a pump |
| `WpfApplicationFixture` | the one shared application instance for tests that need resources |

The last one has no Android equivalent and should not acquire one. The
constraints that produced it do carry over, though, and are worth restating as
the Android test rules: **no real network, no filesystem outside a temp fixture,
no wall-clock dependence, no machine-locale dependence, and no test that needs a
real device UI.**
