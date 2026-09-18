# 03 · Services — contracts, endpoints, caching and degradation

Extracted from `desktop/LostieLauncher/Services/`,
`desktop/LostieLauncher/Core/DependencyInjection.cs` and
`desktop/LostieLauncher/Core/Endpoints.cs`.

## Endpoints

One shared base constant, `https://ericlostie-launcher.jagoba.dev`, plus two
absolute URLs on a different host. Every one of them reaches a service as part
of an injected options record — nothing is hardcoded inside a service.

| Purpose | URL | Options record | Client |
| --- | --- | --- | --- |
| Game catalogue | `https://ericlostie-launcher.jagoba.dev/games/listado.json` | `ContentOptions.ContentEndpoint` | `Content` |
| Home news and notifications | `https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json` | `ContentOptions.NotificationsEndpoint` | `Content` |
| Maintenance flag | `https://cdn.jagoba.dev/ericlostie-launcher/flag.txt` | `ContentOptions.FlagEndpoint` | `SecurityFlag` |
| Download base | `https://ericlostie-launcher.jagoba.dev/games` | `DownloadOptions.BaseUrl` | `Download` |
| Launcher update feed | `https://ericlostie-launcher.jagoba.dev/public/installer/` | `UpdateOptions.FeedUrl` | Velopack's own |

Two derived URL shapes, built by the Library:

- Regular download: `<DownloadOptions.BaseUrl>` + the catalogue entry's
  `rutaRelativa`, which already starts with a slash —
  `…/games/pokemon-anil/4-13-0.zip`.
- Special version: `<DownloadOptions.BaseUrl>/<key>/<archivo>`, and its config
  at `<DownloadOptions.BaseUrl>/<key>/game.config`.

Game logos are `https://ericlostie-launcher.jagoba.dev` + the entry's `logo`,
which also starts with a slash. The window icon is
`https://ericlostie-launcher.jagoba.dev/public/imgs/logo-launcher.png`.

## The three HTTP clients

They differ on purpose. Reproducing the three timeouts is the point; using one
client for everything would break two of the three behaviours.

| Client | Timeout | Connect timeout | Why |
| --- | --- | --- | --- |
| `Content` | **10 s** | default | JSON is small; a slow CDN should degrade to cache quickly rather than stall the UI |
| `SecurityFlag` | **3 s** | default | this check gates every server-backed action, so it must never be what the user waits on; on timeout it assumes *not blocked* |
| `Download` | **infinite** | **20 s** | a multi-gigabyte transfer must not be killed by a wall-clock timeout, but a dead host must still fail fast at connect. Stalls are caught by an inactivity watchdog instead — see below |

All three send `User-Agent: LostieLauncher/<version>`.

---

## IContentService

The catalogue, the home content, the maintenance flag, the local game registry
and the playtime ledger. Nine members.

### `GetGamesAsync()` → list of catalogue entries

1. **If server actions are blocked, return an empty list** without calling the
   catalogue at all, and log why.
2. GET the catalogue on the `Content` client, throw on a non-success status,
   deserialize, drop any null element.
3. Any exception at all → log and return an empty list.

There is **no caching**: every call is a live fetch.

### `GetHomeContentAsync(forceRefresh = false)` → home content

Two stages, and the caching lives in the first.

**Stage 1 — resolve the cache**, serialised behind a semaphore:

- Not forcing and a cache exists → return it with the current staleness flag.
- Otherwise fetch, and on success replace the cache and clear staleness.
- On failure **keep the previous cache** and mark it stale. A transport failure
  (`HttpRequestException`, `TaskCanceledException`, `OperationCanceledException`)
  is logged at info level as "keeping the last known content"; anything else is
  logged as an error.
- The cache is in-memory only and unbounded in time. It never expires; it is
  only replaced by a successful fetch.

**Stage 2 — project it for the current language:**

- Read the language from settings and map it to a code: `es`, `en`, `ca`, `eu`,
  `gl`, `pt`, `val`, `fr`.
- Drop every item whose `expires_at` is at or before *now in Central European
  Time* — see the time-zone note below.
- Resolve each localized field, then return the items with the staleness flag.
- A failure here returns empty content **marked stale**.

With no cache at all it returns empty content carrying the staleness flag from
stage 1, so a first run with no network shows "content unavailable" rather than
"no content".

### `IsServerActionBlockedAsync(forceRefresh = false, ct)` → bool

The maintenance kill switch: while the flag file exists on the CDN, the launcher
refuses to fetch the catalogue, start a download, update a game, or switch to a
special version.

- Cached for **30 seconds** in a volatile field, force-refreshable.
- The probe is a `HEAD` on the `SecurityFlag` client, reading headers only. If
  the server answers `405 Method Not Allowed` it retries with `GET`, again
  headers only.
- **Any 2xx means blocked.** Anything else does not.
- Cancellation or timeout → log, cache *not blocked* for 30 s, return false.
- Any other exception → log, cache *not blocked* for 30 s, return false.

Failing open is deliberate: an unreachable flag file must not lock the user out
of their own launcher.

### `GetGameDirectory(gameName)` → path

`<gamesRoot>/<gameName>`, fully resolved, with a **containment check**: if the
resolved path does not start with the canonical games root plus a separator it
throws. This is what stops a catalogue name like `../..` from escaping the
library folder. Callers that cannot afford a throw wrap it and log.

### `GetLocalGamesAsync()` → list of local records

Reads `local_games.json` from the games root under a static file lock. A missing
file is an empty list, not an error. Then **deduplicates**: by GUID when the
record has one, by name case-insensitively when it does not. The first
occurrence wins and each skipped duplicate is logged. Any failure → log, empty
list.

### `RegisterGameAsync(id, name, version, tipo)`

Under the same lock: ensure the games root exists, read the current list,
**remove every entry with the same name, case-insensitively**, append the new
one, and write atomically. Failures are logged and swallowed.

### `RemoveGameRegistryAsync(name)`

Read, filter out by name case-insensitively, write atomically. A missing file is
a no-op. Failures logged and swallowed.

### `AddPlaytimeAsync(id, minutes)`

An empty GUID is ignored outright. Under a separate lock: read `playtime.json`,
add the minutes to the existing record or create one, ensure the folder exists,
write atomically. Failures logged and swallowed.

### `GetAllPlaytimesAsync()` → map of GUID to minutes

Reads `playtime.json`, drops empty GUIDs, returns a map. A missing file is an
empty map. Failures logged and empty.

### Atomic writes

Every write to the two registry files goes through the same routine: write to
`<path>.tmp`, then move over the target with overwrite. A crash mid-write
therefore cannot leave a half-written registry.

### Two locks, not one

`local_games.json` and `playtime.json` have **separate** static locks, so
registering a game and recording playtime do not contend. Both are static, not
per-instance — the service is a singleton, but the locks being static makes the
guarantee independent of that.

---

## IDownloadService

Two members. This is the only service that moves large amounts of data.

### `FetchSpecialVersionConfigAsync(key, ct)` → outcome + config

GET `<base>/<key>/game.config` on the **`Content`** client — not the download
client. Outcomes:

| Status / condition | Outcome |
| --- | --- |
| `404` | `NotFound` |
| non-success | `NetworkError` (via a thrown-and-caught exception) |
| body does not parse | `InvalidResponse` |
| cancelled by the caller's token | `Cancelled` |
| any other exception | `NetworkError` |
| otherwise | `Success` with the parsed config |

The config format and its parser are in section 04.

### `DownloadAsync(url, destination, progress, ct)` → outcome

Resumable, retrying, watchdogged, and finalized by rename.

**Retry envelope.** Up to **3 attempts** (2 retries). Only `HttpRequestException`
and `IOException` are retried, and only when the caller has not cancelled.
Backoff is linear: 1 s after the first failure, 2 s after the second. Exhausting
them returns `Failed` with the message "Download failed after maximum retries."
`UnauthorizedAccessException` anywhere returns `PermissionDenied`; anything else
returns `Failed` carrying the exception message.

**File layout.** The transfer writes `<destination>.part` and keeps resume
metadata in `<destination>.part.meta`. The final rename produces
`<destination>`.

**Resume.** If a `.part` exists, its length is the resume offset and the sidecar
is read. A conditional resume is only attempted when the sidecar yields a usable
validator:

- a **strong** ETag (weak ones are rejected), or
- a `Last-Modified` date.

With a validator it sends `Range: bytes=<offset>-` and
`If-Range: <validator>`. **Without one it deletes the partial file and starts
over**, rather than risk splicing two different files together.

**Responses:**

| Status | Handling |
| --- | --- |
| `206 Partial Content` | resume; append to the partial file |
| `200 OK` with an offset | the resource changed — delete the partial file, restart from zero, and record fresh metadata |
| `200 OK` from zero | normal start; record metadata |
| `416 Range Not Satisfiable` with a partial file matching the recorded total | treat as already complete: finalize, drop the sidecar, report 100 % |
| `416` with any other size | delete the partial file and throw an `IOException`, which the retry envelope catches |
| anything else | throw on non-success |

**Inactivity watchdog.** A linked cancellation source is re-armed with a
**60 second** deadline before the response and again before **every** read. A
stall therefore cancels the read; because the caller's own token is not set, that
cancellation is translated into an `IOException` reading *"Download stalled: no
data received within 60s."*, which the retry envelope treats as retryable. A
genuine user cancellation propagates as `Cancelled` instead.

**Buffering and progress.** 64 KiB buffer. Speed is recomputed at most every
**500 ms** from the bytes moved since the last sample. Progress is reported only
when the total size is known; the report carries percent, bytes per second, total
bytes and downloaded bytes. The total is `Content-Length` **plus the resume
offset**, so a resumed download reports absolute progress rather than progress
through the remainder.

**Finalization.** The `.part` file is renamed over the destination with up to
**3 attempts** and a **500 ms** base delay multiplied by the attempt number.
A retry only happens for `UnauthorizedAccessException` or `IOException`, and
**never** when the destination is a directory or a read-only file — those cannot
resolve by waiting. A final failure logs a diagnostic line naming both paths,
their attributes and sizes, whether either is locked by another process, whether
the destination directory exists, and the decoded Win32 error code, then
rethrows. On success the sidecar is deleted and 100 % is reported.

---

## IDownloadLocationService

Two members, both seams over the filesystem and the environment.

- `Probe(directory)` — the write-and-rename pre-flight. Creates the folder if
  needed, writes a two-byte file named `.lostie-probe-<guid>.tmp`, renames it to
  `.lostie-probe-<guid>.tmp.moved`, and deletes both names whatever happened. It
  reports which step failed: `CannotCreate`, `CannotWrite`, `CannotRename`, or
  `Usable`.

  The rename step is the reason the probe exists. Renaming needs DELETE on the
  file while writing does not, so a network share, an external drive with
  foreign ACLs or a managed machine can accept gigabytes and then fail the one
  rename that finalizes the download. Without this check the user pays a full
  download per game to learn the folder cannot work.

  The enum is ordered so that its default value is `Usable`: an unconfigured
  probe must never block a download by accident.

- `IsOneDriveSynced(directory)` — see section 09 for the decision function and
  section 10 for why it exists.

## IDownloadLocationNotifier

The seam over the two download-location dialogs, so the ViewModels that decide
*when* to show them stay testable.

- `NotifyDirectoryNotUsable(result)` — `DownloadDirNotUsableTitle` plus
  `DownloadDirNotUsableMessage` formatted with the folder and a localized step
  name (`DownloadDirStepCreate`, `…Write`, `…Rename`), OK, error icon.
- `ConfirmOneDriveDirectory(directory)` → bool — `OneDriveWarning*` formatted
  with the folder, yes/no, information icon.

Both marshal onto the UI thread when there is one, and run inline when there is
not.

## IWindowsStartupService

Three members over the Windows `Run` registry key — see section 10.
`IsEnabled()`, `Enable()`, `Disable()`, each returning `false` (or `true` for a
missing key on disable) rather than throwing. The command written is the
executable path wrapped in double quotes; an unavailable process path is a
failure, not a silent success.

## IUpdateService, IUpdateGateway, IUpdateNotifier

Three pieces so that the orchestration is testable without the network or the
Velopack runtime.

- **`IUpdateGateway`** wraps Velopack's update manager. `CheckForUpdatesAsync()`
  returns a pending package or nothing. A package exposes its version, a
  download, and apply-and-restart.
- **`IUpdateNotifier`** wraps the dialogs: `NotifyUpToDate`,
  `NotifyCheckFailed`, `NotifyDownloadInProgress`, and
  `PromptApply(version) → bool`. Same UI-thread marshalling as above.
- **`IUpdateService.CheckForUpdatesAsync(notifyWhenUpToDate)`** is the flow:

  1. Check. No update → log, and notify *only if asked*.
  2. Update found → log the version, **download it**, then prompt.
  3. Accepted → log and apply-and-restart. Declined → log and do nothing.
  4. Any exception → log, and notify a failed check *only if asked*.

  The flag is what separates the silent startup check from the user-initiated
  one in Settings. The update is downloaded before the user is asked, so
  accepting is instant.

- `NotifyDownloadInProgress()` is called by Settings instead of checking at all
  while a game download is running.

## ISettingsService

Load, save, and the two games-root helpers.

- **`Load()`** is cached after the first read, behind a lock. The first read
  migrates a legacy file (see section 04), reads, sanitizes, and caches. A
  missing file yields sanitized defaults; a corrupt file is logged and also
  yields sanitized defaults.
- **`Save(settings)`** sanitizes, replaces the cache, and **debounces the disk
  write by 500 ms** — the settings screen writes on every toggle and every
  keystroke of a path, so the debounce is what keeps that from being a write
  storm. Saving after disposal is a no-op.
- **Disposal** stops the timer and flushes any pending write, so a pending
  change is never lost on exit.
- `GetGamesRootDirectory()` → `<DownloadDirectory>/LostieLauncher`.
- `EnsureGamesRootDirectoryExists()` creates it.

Sanitizing rules are in section 04.

---

## Degradation summary

The rule the whole service layer follows: **never propagate a failure that would
take the application down.** Log it, return an empty or last-known value, and
keep the UI usable.

| Failure | Result |
| --- | --- |
| Catalogue unreachable | empty list; the Library shows its empty state |
| Catalogue returns invalid JSON | empty list |
| Home content unreachable, cache present | the cached content, flagged stale; the stale banner appears |
| Home content unreachable, no cache | empty content, flagged stale; "content unavailable" |
| Maintenance flag unreachable | treated as *not blocked* |
| Maintenance flag set | catalogue fetch skipped; downloads, updates and switches refused with a dialog shown once |
| Local registry unreadable | empty list; installed games disappear from the UI but nothing is deleted |
| Playtime file unreadable | empty map; playtimes show as absent |
| Settings file unreadable | sanitized defaults |
| Registry write for startup fails | the toggle reverts to the real state |
| Download stalls 60 s | retried up to the attempt limit, then `Failed` |
| Download folder cannot finalize | the download never starts; the user is told which step failed |
| Hash missing, malformed or mismatched | the archive is deleted and nothing is installed |
