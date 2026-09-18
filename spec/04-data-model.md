# 04 · Data model — domain, wire format and local persistence

Extracted from `desktop/LostieLauncher/Models/`,
`desktop/LostieLauncher/Services/ContentService.cs` and
`desktop/LostieLauncher/Services/SettingsService.cs`. The two captured payloads
in [`samples/`](samples/) are the live responses on 2026-09-18.

## Wire format — the game catalogue

`GET /games/listado.json` returns a **bare JSON array**. Seven entries at the
time of capture. Property names are Spanish and camel-cased, and they are matched
**exactly as written** — the catalogue is deserialized with explicit property
names, not by convention.

```json
{
  "id": "6b49940d-5910-49e5-aab8-f933cd51c388",
  "nombre": "Pokémon Añil",
  "version": "v4.13.0",
  "pesoGB": 0.5889,
  "descripcion": "Remake de Kanto totalmente modernizado…",
  "url": "https://lostiefangames.blogspot.com/p/pokemon-anil.html",
  "logo": "/public/imgs/4.png",
  "rutaRelativa": "/pokemon-anil/4-13-0.zip",
  "sha256": "e379a48072c42942fd4f3f99b6aed0c4d1aff3d1f691448d65af76e035e17ac5"
}
```

| Field | Type | Notes |
| --- | --- | --- |
| `id` | GUID | may be absent or empty in old data; the code treats the empty GUID as "no id" and falls back to matching by name |
| `nombre` | string | the display name **and** the folder name on disk |
| `version` | string | `v`-prefixed semantic version; see the parsing rules below |
| `pesoGB` | number | size in gigabytes, fractional |
| `descripcion` | string | shown in the download dialog |
| `url` | string | a human page about the game, opened from the download dialog; may be empty |
| `logo` | string | path relative to the CDN base, **leading slash included** |
| `rutaRelativa` | string | ZIP path relative to the download base, **leading slash included** |
| `sha256` | string | 64 hex characters; anything else fails verification |

Easy to miss: a null element in the array is dropped rather than crashing the
deserialization, and the whole fetch is skipped entirely when the maintenance
flag is set.

### Derived, never serialized

| Member | Rule |
| --- | --- |
| `LogoUrl` | `null` when `logo` is empty, otherwise `<cdnBase><logo>` |
| `GameId` | a slug: lowercase the name, replace every run of characters outside `[a-z0-9]` with a single `-`, trim leading and trailing `-`. `Pokémon Añil` → `pok-mon-a-il` |
| `PesoFormateado` | `≥ 1` → `"{0:0.#} GB"`, otherwise `"{0:0} MB"` from `pesoGB × 1024`, both **invariant culture** so the decimal separator never follows the machine locale |
| `DownloadSpeedText` | `≥ 1 MiB/s` → `"{0:0.0} MB/s"`, `> 0` → `"{0:0.0} KB/s"`, otherwise the literal `"0 KB/s"`. Divisors are 1 048 576 and 1024 |
| `PlaytimeText` | see `PlaytimeFormatter` in section 09 |

`GameId` is the identity used for download sessions, cache filenames and the
scroll-into-view request. It is derived from the name, so **renaming a game
remotely changes its download-cache identity** and orphans its cached files —
which the cache purge then collects.

### Transient UI state on a catalogue entry

`DownloadStatus` (7 values), `DownloadProgressValue`, `DownloadSpeedBytesPerSec`,
`DownloadRemainingText` and `PlaytimeMinutes` live on the catalogue entry itself
and are observable. They are explicitly excluded from serialization. An Android
port is free to hold them beside the immutable model instead, as long as the card
still updates continuously during a transfer.

## Wire format — home content

`GET /homepage-notifications.json` returns an object with two arrays. Property
names here are **camelCase by convention**, with one exception: `expires_at`,
which is mapped explicitly.

```json
{
  "news": [
    {
      "id": "3b5d0ad6-9cf6-434c-8da8-25b971f5f053",
      "title":       { "es": "…", "en": "…", "eu": "…", "ca": "…",
                       "val": "…", "gl": "…", "pt": "…", "fr": "…" },
      "description": { "es": "…", "…": "…" },
      "tag": "Release",
      "date": "2026-07-24T00:00:00",
      "expires_at": "2026-10-21T00:00:00"
    }
  ],
  "notifications": [
    {
      "id": "140431da-5f94-4a41-b8eb-32a926e3019d",
      "title":   { "es": "…", "…": "…" },
      "message": { "es": "…", "…": "…" },
      "type": "Info",
      "date": "2026-07-24T00:00:00",
      "expires_at": "2026-10-22T00:00:00"
    }
  ]
}
```

News carry `title` + `description` + `tag`; notifications carry
`title` + `message` + `type`. Both carry `id`, `date` and an optional
`expires_at`.

### Things that are easy to get wrong

These are the details worth calling out explicitly, because none of them is
visible from the shape alone.

**1 — Localized fields are maps, not strings.** Keys are the eight language
codes `es en ca eu gl pt val fr`. Note that Valencian is `val`, not a two-letter
code.

**2 — Resolution falls back in a fixed order.** For a requested code: the exact
key, then `es`, then `en`, then the **first non-null value in ordinal key
order**, then the empty string. A null map resolves to the empty string. A
missing language therefore never blanks an item.

**3 — `type` is an enum serialized as a string.** Values are `Info`, `Warning`
and `Exclamation`, matched by name. Only `Info` appears in the captured payload.

**4 — Dates carry no offset and are compared in Central European Time.** The
strings are local-kind `yyyy-MM-ddTHH:mm:ss`. Expiry is evaluated against *now*
converted to CET, and an item's `expires_at` is normalized before comparison:
UTC-kind converts from UTC, local-kind converts from the machine's zone, and
unspecified-kind is **taken as already being CET**. In practice the payload
is unspecified-kind, so the effective rule is *the publisher writes CET wall-clock
times and they are compared to CET wall-clock now* — not to the device's local
time, and not to UTC. A device in another time zone must therefore not compare
in its own zone.

If the CET zone cannot be resolved on the host, the code logs and **falls back
to UTC**, which shifts expiry by one or two hours. That fallback is a documented
compromise, not a target to reproduce faithfully.

**5 — Filtering is `> now`, not `>= now`.** An item expiring exactly now is
dropped. A missing or null `expires_at` never expires.

**6 — `date` is not used for filtering or ordering.** Items appear in payload
order. `date` is only rendered: `dd MMM yyyy` on news, `dd MMM` on
notifications. Both use the machine's current culture in WPF, which means the
month abbreviation is already locale-dependent on desktop and is **not** tied to
the launcher's own language setting. An Android port should format these with
the launcher's selected language rather than inherit that inconsistency; this is
the one place where matching the desktop exactly would reproduce a defect.

## Version strings

Both the catalogue and the local registry store versions as `v4.13.0`.

- **Comparison** (`VersionUtils.IsNewerVersion`) strips a leading `v` or `V`,
  cuts everything from the first `-` (so a pre-release suffix is ignored), and
  parses the rest as a dotted numeric version. **If either side fails to parse,
  the answer is "not newer"** and the fact is logged — an unparseable version
  never produces a spurious update prompt.
- Comparison is on the numeric components, so `1.2` and `1.2.0` are **not**
  equal: the shorter one has `-1` for the missing fields and sorts lower.
- **Display** (`FormatDisplayVersion`) trims, strips leading `v`/`V`, and
  re-prefixes a single `v`. A null, blank, or `"v"`-only value renders as the
  literal `"unknown"`.

## The special-version config

`GET <downloadBase>/<key>/game.config` returns a plain-text key/value file, not
JSON:

```
sha256=…64 hex…
tipo=Halloween
juego-principal=6b49940d-5910-49e5-aab8-f933cd51c388
vers=v4.13.0-halloween
archivo=anil-halloween.zip
```

Parsing rules, exactly:

- Split on `\n`, dropping empty entries and trimming each line.
- Split each line at the **first** `=`. A line with no `=`, or with `=` at
  index 0, is skipped.
- Keys are compared **case-insensitively**; a repeated key wins with its last
  occurrence.
- **All five keys are required**: `sha256`, `tipo`, `juego-principal`, `vers`,
  `archivo`. A missing one makes the whole parse fail.
- `juego-principal` must parse as a GUID or the parse fails.

The caller then validates further before using it: `archivo` must match
`^[A-Za-z0-9._-]+\.zip$`, `sha256` must be 64 hex characters, `vers` must be
non-blank, and `juego-principal` must equal the game the user is downloading.

`tipo` is the special-version label, rendered verbatim as a badge on the card.
It is **not** localized.

## Domain models

| Model | Shape | Notes |
| --- | --- | --- |
| Catalogue entry | the nine wire fields + derived + transient UI state | above |
| Local record | `id`, `nombre`, `version`, `tipo?` | what is on disk |
| Installed entry | `Id`, `Nombre`, `InstalledVersion`, `HasUpdate`, `UpdateVersion`, `Logo`, `Tipo?`, + observable `IsUpdating`, `IsUninstalling`, `HasHelpFolder`, `PlaytimeMinutes` | built by joining the two |
| News item | `Id`, `Title`, `Description`, `Tag`, `Date`, `ExpiresAt?` | resolved to one language |
| Notification item | `Id`, `Title`, `Message`, `Type`, `Date`, `ExpiresAt?` | resolved to one language |
| Home content | news list, notifications list, `IsStale` | |
| Playtime record | `id`, `playtimeMinutes` | |
| FAQ item | `Question`, `Answer`, observable `IsExpanded` | |
| Download arguments | `GameId`, `Version`, `RutaRelativa`, `Key?` | the command parameter |
| Download progress | `Percent`, `BytesPerSecond`, `TotalBytes` (default `-1`), `DownloadedBytes` | |
| Download result | outcome + optional message | `Success`, `Cancelled`, `Failed`, `PermissionDenied` |
| Special-version result | outcome + optional config | `Success`, `NotFound`, `NetworkError`, `InvalidResponse`, `Cancelled` |
| Directory probe result | outcome + directory + error text | `Usable`, `CannotCreate`, `CannotWrite`, `CannotRename` |
| Directory deletion result | `Deleted`, `Attempts`, `BlockingPath?`, `Error?`, `DeletedEntries` | |
| Uninstall result | outcome + blocking path | `Completed`, `FilesNotFound`, `FilesLeftBehind`, `NothingDeleted`, `GameRunning` |
| Download cache entry | `FileName`, `LastWriteTimeUtc` | input to the purge policy |

Enums: `AppLanguage` (8), `AppTheme` (10), `GameCardMode` (2),
`GameDownloadStatus` (7), `NotificationType` (3), `GameRunningSignal` (3), plus
the outcome enums above.

The error text on a probe result is built as `"<ExceptionType>: <message>"` and
is **for the log only** — the dialog composes its message from the outcome and
the localized step name, never from this field.

## On-disk layout

Two roots, deliberately separate.

### Settings

`%APPDATA%/LostieLauncher/launcher_settings.json`, written indented.

```json
{
  "Language": 0,
  "Theme": 0,
  "StartWithWindows": false,
  "StartMinimized": false,
  "AutoUpdate": false,
  "DownloadDirectory": "C:\\Users\\…",
  "HasSeenWelcome": false
}
```

Enums are serialized as **numbers** here (no string-enum converter is applied to
this file), unlike the remote payloads.

**Migration**: if the roaming file does not exist but a legacy
`launcher_settings.json` sits next to the executable, it is moved to the roaming
path on first load. Older builds stored it beside the binary, where a Velopack
update would replace the folder and lose it.

**Sanitizing**, applied on every load *and* every save:

- `Language` not a defined enum value → log and fall back to `Esp`.
- `Theme` not a defined enum value → log and fall back to `Volcarona`.
- `DownloadDirectory` blank → log and fall back to the default.
- `DownloadDirectory` not fully qualified → log and fall back to the default. A
  relative path would otherwise resolve against the working directory.
- Otherwise it is fully resolved.

### Game library

`<DownloadDirectory>/LostieLauncher/`, where `DownloadDirectory` defaults to the
**user profile folder**. Inside it:

```
<DownloadDirectory>/LostieLauncher/
├── local_games.json          # the installed registry
├── playtime.json             # minutes per game GUID
├── .downloads/               # the transfer cache
│   ├── <gameId>.<token>.zip
│   ├── <gameId>.<token>.zip.part
│   └── <gameId>.<token>.zip.part.meta
└── <Game Name>/              # one folder per installed game
    ├── Game.exe
    └── ayuda/                # optional; enables the help button
```

`local_games.json` — a bare array, camelCase:

```json
[ { "id": "…", "nombre": "Pokémon Añil", "version": "v4.13.0", "tipo": null } ]
```

`playtime.json` — a bare array, camelCase:

```json
[ { "id": "…", "playtimeMinutes": 137 } ]
```

Both are written atomically through a `.tmp` file plus a rename.

### The download cache filename

`<gameId>.<token>.zip`, where the token is the **first 8 bytes of the SHA-256 of
`"<version>|<key ?? \"\">"`, lowercase hex** — 16 characters. Two different
versions, or the same version with and without a key, therefore never collide,
and resuming works only against a partial file for exactly the same target.

The purge policy that cleans this folder is in section 09.

## Why the default library location is the user profile

Recorded here because it is a decision, not an accident, and the Android port
has to make the equivalent one.

The desktop default is `%USERPROFILE%`, so the library lands in
`%USERPROFILE%/LostieLauncher`. Every other candidate carries a hazard a
multi-gigabyte library cannot afford:

- **Documents** is redirected into OneDrive on most consumer installs — cloud
  quota, sync contention, Controlled Folder Access, and online-only placeholders
  that fail offline.
- **`%LOCALAPPDATA%`** would resolve to the launcher's own Velopack install
  directory, because the pack id is the same name. Uninstalling the launcher
  would delete the whole library.
- **The drive root** can be locked down by policy on a managed machine.
- **Downloads** is the one folder Windows points an automatic cleanup at:
  Storage Sense deletes its contents by last-access date, which would strip a
  game's assets while leaving the executable behind.

The Android equivalents of these hazards are different — scoped storage,
app-private versus shared storage, and uninstall semantics — so step 07 must
make this decision again rather than translate it.
