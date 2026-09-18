# 02 · Screens — state, actions and enablement

Extracted from `desktop/LostieLauncher/ViewModels/` and
`desktop/LostieLauncher/Views/Partials/`.

Five screens, each a ViewModel plus a view. All five ViewModels are singletons
created at startup, so **every screen loads its data once at construction**,
before the user ever navigates to it. Navigation swaps which one is displayed;
it does not create or dispose anything.

Common conventions across the screens:

- Each list screen has `IsLoading`, a collection, and two derived booleans:
  `IsEmpty` = `!IsLoading && count == 0` and `IsListVisible` =
  `!IsLoading && count > 0`. The three states are mutually exclusive, so there
  is always exactly one thing on screen: skeletons, an empty state, or the list.
- The content area of every screen is padded **20 px** inside the 15 px shell
  inset.
- Failures are logged and degrade to an empty list. No screen shows a stack
  trace or an error state; "empty" and "failed" look the same, except on Home
  where staleness is distinguished (below).

---

## Home

`HomeViewModel` + `HomeView.xaml`. The landing screen.

### State

| Member | Type | Notes |
| --- | --- | --- |
| `News` | observable collection of news items | replaced wholesale on each load |
| `Notifications` | observable collection of notification items | replaced wholesale |
| `IsLoading` | `bool` | only set by a foreground load |
| `IsOfflineMode` | `bool` | the maintenance flag is set on the server |
| `IsContentStale` | `bool` | the last refresh failed and cached content is being shown |
| `IsOutOfDateWarningVisible` | derived | `IsContentStale && !IsOfflineMode` |
| `IsEmpty` / `IsListVisible` | derived | over both collections combined |
| `IsNewsEmpty` / `IsNotificationsEmpty` | derived | per column |

`IsOfflineMode` is proxied up to `MainViewModel` so the title bar can show the
offline pill.

### Loading

- **At construction**, a foreground load runs and a **background refresh loop**
  starts on a two-minute period.
- The loop refreshes with `forceRefresh: true` and `showLoading: false`, so the
  screen never flashes its skeletons on a background tick.
- A semaphore serialises loads: a manual refresh and a background tick cannot
  interleave.
- A language change triggers a fresh foreground load, because the server returns
  all languages and the service resolves one.
- `RefreshAsync()` is a foreground load with `forceRefresh: true`.
- Disposal cancels the loop and unsubscribes from the settings ViewModel.

Each load asks two questions in order: *is the server in maintenance?*
(`IsServerActionBlockedAsync`) and *what is the home content?*
(`GetHomeContentAsync`). Both take the force-refresh flag.

### Layout

Two equal columns separated by a 20 px gap, each a header plus a scrollable
list. Headers are `HomeNews` and `HomeNotifications` at 18 px SemiBold with
14 px below.

Above both columns, spanning the full width, sit two mutually exclusive banners
(radius 6, 1 px `#FFFFC107` border, `OverlaySubtleBrush` background, 14 px
padding, 16 px below):

| Banner | Shown when | Icon | Title | Body |
| --- | --- | --- | --- | --- |
| Offline | `IsOfflineMode` | CloudOffOutline 22 px | `OfflineModeLabel` | `ServerMaintenanceNotificationMessage` |
| Stale content | `IsOutOfDateWarningVisible` | CloudAlertOutline 22 px | `ContentOutOfDateLabel` | `ContentOutOfDateMessage` |

Titles 14 px SemiBold in the primary foreground; bodies 12 px wrapped in the
dim foreground with 4 px above.

### The three states

| State | What is shown |
| --- | --- |
| Loading | Both column headers, and **four** news skeletons and **four** notification skeletons |
| Empty (both lists) | Centred CloudOffOutline at 48 px in the dim foreground, then one message at 14 px: `HomeNoContent` normally, or `HomeContentUnavailable` when `IsContentStale` |
| List | Per column: the cards, or a per-column empty state |

Per-column empty states, centred, 36 px icon plus a 13 px message
(`HomeNoContent`), 12 px apart: news uses NewspaperVariantOutline, notifications
use BellOffOutline.

---

## My Games

`GamesViewModel` + `GamesView.xaml`. The installed library.

### State

| Member | Type | Notes |
| --- | --- | --- |
| `InstalledGames` | observable collection of installed-game entries | |
| `IsLoading` | `bool` | |
| `IsEmpty` / `IsListVisible` | derived | |

Plus a private map of running game processes keyed by game name,
case-insensitively.

### Loading

At construction it **waits for the Library to finish loading first**
(`LibraryViewModel.LibraryLoadedTask`), because each installed entry needs the
remote catalogue to know whether an update exists, and to borrow the logo.
`RefreshAsync()` does not wait.

Each entry is built by matching the local record against the remote catalogue.
The rule is a scan taking the **first** catalogue entry for which
`(the local record has a GUID and the GUIDs are equal) OR (the names are equal,
case-insensitively)`. Both branches are always live: **a local record with a
GUID still matches on name** when no GUID matches. It is deliberately **not**
the same rule the Library uses — see the warning under *Library* below. Then:

| Field | Source |
| --- | --- |
| `Id`, `Nombre`, `InstalledVersion`, `Tipo` | the local registry |
| `HasUpdate` | remote version is strictly newer than the installed one |
| `UpdateVersion` | the remote version, only when `HasUpdate` |
| `Logo` | the remote entry, empty when there is no match |
| `PlaytimeMinutes` | the playtime file, by GUID; 0 for an empty GUID |
| `HasHelpFolder` | a subdirectory named `ayuda` exists in the game folder |
| `IsSpecialVersion` | derived: `Tipo` is non-empty |

**Auto-update**: after the initial load only — never after a manual refresh —
if the `AutoUpdate` setting is on, every entry that has an update and is *not* a
special version is updated sequentially, without navigating to the Library.
Special versions are deliberately excluded: updating one would replace it with
the regular build.

### Actions

| Action | Parameter | Enabled when | Effect |
| --- | --- | --- | --- |
| Play | game name | not updating, and not (`HasUpdate` and not a special version) | launch the game — see section 01 |
| Update | game name | `HasUpdate` | mark the entry updating, navigate to the Library, run the library update, unmark |
| Switch to special version | game name | not updating | navigate to the Library and run the special-version flow there |
| Open folder | game name | not updating | open the game folder; if it does not exist, offer to download instead |
| Open help folder | game name | a folder named `ayuda` exists | open it |
| Uninstall | game name | always | see below |
| Go to library | — | always | navigate to the Library |

The Play guard is the interesting one: **a game with a pending regular update
cannot be played until it is updated**, but a special version can be played even
when the catalogue has a newer regular version, because the special build is not
what the catalogue is offering.

"Open folder" on a missing folder shows `FolderNotFound*` as a yes/no; on yes it
navigates to the Library and starts a download for that game.

### Uninstall

A three-step flow, deliberately asymmetric about whether the game is running:

1. **Signal check.** `TrackedProcess` → refuse outright with
   `UninstallGameRunning*`. `ExecutableLocked` → do not refuse; warn instead.
2. **Confirmation.** Locked executable: title `UninstallGameRunningTitle`,
   message `UninstallMaybeRunningMessage`, error icon. Otherwise:
   `UninstallConfirmTitle` / `UninstallConfirmMessage`, information icon. Both
   are yes/no and both interpolate the game name. On no, nothing happens.
3. **Deletion**, on a background thread, with its outcome mapped to a dialog:

| Outcome | Condition | User sees |
| --- | --- | --- |
| `GameRunning` | the tracked process came alive between steps | `UninstallGameRunning*` |
| `FilesNotFound` | the folder did not exist; the entry is still unregistered | `UninstallNotFound*`, information |
| `Completed` | everything deleted | nothing |
| `FilesLeftBehind` | some files deleted, some not | `UninstallError*` (name + blocking path), yes/no; on yes, open the blocking location |
| `NothingDeleted` | not a single entry could be deleted | `UninstallBlocked*` (name + blocking path), yes/no; on yes, open the blocking location |

The distinction between the last two is the point of the whole flow. If nothing
could be deleted the installation is intact, so the game **stays registered** —
unregistering it would strip the user of an entry they can still use. If part of
it was deleted the installation is already broken, so the game **is
unregistered** anyway rather than leaving an entry that can neither launch nor
be removed.

On a successful unregistration the entry is removed from the installed list and
the matching library card drops back to *Available*.

### The three states

| State | What is shown |
| --- | --- |
| Loading | **Five** game-card skeletons in a scroller with 20 px padding |
| Empty | Centred GamepadVariantOutline at 48 px, `GamesNoContent` at 14 px, then an accent button `GamesGoToLibrary` with 20 × 8 padding |
| List | Game cards in *Games* mode |

---

## Library

`LibraryViewModel` + `LibraryView.xaml`. The remote catalogue, and the whole
download pipeline.

### State

| Member | Type | Notes |
| --- | --- | --- |
| `Games` | observable collection of catalogue entries | |
| `IsLoading` | `bool` | |
| `IsEmpty` / `IsListVisible` | derived | |
| `LibraryLoadedTask` | task | completes after the first load, success or not |
| `PendingScrollGameId` | `string?` | set while an update or switch is scrolling into view |

Private: a dictionary of download sessions keyed by game id, the active session,
and a flag so the "server actions unavailable" message is shown at most once per
blocked streak.

### Loading

Fetches the catalogue, the local registry and the playtimes, then decorates each
catalogue entry:

- Matched against the local registry with **two lookup maps**, built once: one
  keyed by GUID holding only the local records that **have** a GUID, and one
  keyed by name holding only the local records whose GUID is **empty**. A
  catalogue entry with a GUID is looked up by GUID; failing that — and for an
  entry with no GUID — it is looked up by name. A matched entry becomes
  `UpdateAvailable` when the remote version is newer, `Downloaded` otherwise.
- Playtime is attached by GUID.

> [!WARNING]
> **The two screens do not match the same way, and the difference is visible.**
> My Games falls back to a name match for *every* local record; the Library's
> name map deliberately excludes local records that carry a GUID, so for those
> there is no name fallback at all.
>
> Take a local record with GUID `X` and name `N`, where the catalogue publishes
> `N` under a different GUID. My Games matches it by name and shows it as
> installed; the Library finds nothing and leaves the card `Available`. The same
> game therefore reads as installed on one screen and not installed on the other.
>
> This is recorded as **observed desktop behaviour, not as a contract**. The
> port should implement one matching rule for both screens and say which; a
> faithful reproduction of the divergence would be reproducing a defect. Raise
> it in step 10 rather than deciding silently.

Afterwards it **purges the stale download cache** — see section 03 for the
policy.

### Download states

Seven, on the card rather than on the screen:

`Available` · `Downloading` · `Paused` · `Downloaded` · `UpdateAvailable` ·
`Extracting` · `VerifyingIntegrity`

### Actions

Three entry points, all guarded identically at the top: **if a download is
already active, the request is ignored silently** (logged, no dialog), and
**if the server maintenance flag is set, the request is refused** with
`ServerActionsUnavailable*` shown once.

#### Start download

1. If the game is `Paused` and a session exists, resume it directly — no dialog.
2. Otherwise show the download confirmation dialog (section 08). It returns the
   arguments, optionally carrying a key; cancelling returns nothing and stops.
3. **With a key**: validate the format against
   `^[A-Za-z0-9]{4}(-[A-Za-z0-9]{4}){4}$`; on failure show
   `DownloadKeyInvalid*`. Fetch the special-version config; map a failure to a
   dialog (`NotFound` → `DownloadKeyNotFound*`, `NetworkError` or
   `InvalidResponse` → `DownloadError*`, `Cancelled` → nothing). Validate the
   config: the archive name must match `^[A-Za-z0-9._-]+\.zip$`, the hash must
   be 64 hex characters, and the version must be non-blank; a bad config shows
   `DownloadKeyNotFound*`. Then check the config's `juego-principal` GUID
   equals this game's; a mismatch shows `DownloadKeyMismatch*`. The download
   version becomes the config's version and the URL becomes
   `<downloadBase>/<key>/<archivo>`.
4. **Without a key**: the URL is `<downloadBase><rutaRelativa>`.
5. Create a session and run the transfer.

#### Start update

No dialog and no key. URL is `<downloadBase><rutaRelativa>`, session marked as
an update, the card is scrolled into view, and the transfer runs.

#### Switch to special version

Prompts for a key with its own dialog (section 08), then runs the same
validation chain as above, marks the session as an update, scrolls the card into
view and transfers.

The difference between "update" and "not update" is only visible on failure: a
failed or cancelled update returns the card to `UpdateAvailable`, a failed
download returns it to `Available`.

#### Pause and cancel

- **Pause** cancels the session's token. The transfer returns *Cancelled*, and
  because the session is not marked as cancelling, the card goes to `Paused` and
  the partial file is kept.
- **Cancel** asks for confirmation (`CancelDownloadConfirm*`, yes/no) and then
  behaves differently by state. Already paused: delete the artifacts, reset the
  card, and clear the global downloading flag if this was the active session.
  Still downloading: mark the session as cancelling and cancel the token, so the
  *Cancelled* outcome deletes the artifacts and resets the card.
- Both accept an optional game id; without one they act on the active session.

### Transfer pipeline

Per attempt:

1. **Pre-flight the destination folder** with a write-and-rename probe. On
   failure, log, show `DownloadDirNotUsable*` naming the folder and the failed
   step, and **do not start the download or touch the card**.
2. Set the card to `Downloading`, progress 0, and the global downloading flag.
3. Report progress continuously: percent, bytes per second, and a remaining-time
   string rendered as `· 1h 20m`, `· 5m 3s` or `· 42s` — only when both the
   speed and the total size are known.
4. On the outcome:

| Outcome | Handling |
| --- | --- |
| `Success` | verify, extract, register, mark `Downloaded`, raise `GameInstalled` |
| `Cancelled` | paused or cancelled, as above |
| `Failed` | log, reset the card, show `DownloadError*` |
| `PermissionDenied` | log, delete the artifacts, reset the card, show `DownloadPermissionDenied*` |

5. Always clear the speed and remaining text, and clear the global downloading
   flag if this session is still the active one.

#### Verification and installation

- The expected hash is the special config's when there is one, otherwise the
  catalogue entry's.
- **A missing or malformed hash fails verification.** It does not skip it. The
  card shows `VerifyingIntegrity` while the hash is computed on a background
  thread, compared case-insensitively.
- On mismatch: delete the ZIP, log whether it was a real mismatch or a refusal
  for want of a valid hash, reset the card, and show `HashMismatch*`.
- On match: card to `Extracting`, progress pinned at 100, extract, register the
  game locally with its version and `tipo`, card to `Downloaded`, remove the
  session, raise `GameInstalled`.
- Any exception in this whole block resets the card and shows
  `DownloadError*`.

### The three states

| State | What is shown |
| --- | --- |
| Loading | **Six** game-card skeletons in a scroller with 20 px padding |
| Empty | Centred CloudOffOutline at 48 px and `LibraryNoContent` at 14 px |
| List | Game cards in *Library* mode |

---

## FAQs

`FaqsViewModel` + `FaqsView.xaml`. A searchable accordion. Entirely local: no
network, no persistence.

### State

| Member | Type | Notes |
| --- | --- | --- |
| `SearchText` | `string` | two-way, updated on every keystroke |
| `FilteredFaqs` | observable collection | mutated in place, not replaced |
| `HasNoResults` | `bool` | |

The full set is held privately and reloaded whenever the language changes.

### Filtering

On every change to `SearchText`:

- The term is trimmed; an empty term means "not searching".
- An entry survives if the term matches its question **or** its answer, compared
  case-insensitively **and diacritic-insensitively** against the invariant
  culture — so `anil` matches `Añil`.
- **Every surviving entry is expanded while searching and collapsed when the
  search is cleared.** Expansion is a property of the entry, so a manual
  expansion is discarded by the next keystroke.
- `HasNoResults` is set when nothing survives.

`ClearSearchCommand` sets the text to empty, which re-runs the filter.

### Layout

A search bar docked at the top: `TertiaryBgBrush`, radius 6, height 40, 12 px
horizontal padding, 16 px below. Inside: a 16 px Search icon in the dim
foreground with 10 px to its right; the text box at 13 px, transparent, no
border, caret in the primary foreground; a 24 × 24 clear button visible only
when the text is non-empty, hovering to `OverlayLightBrush` with the foreground
brightening.

The placeholder `FaqsSearchPlaceholder` shows only when the box is **both empty
and unfocused**.

Below: the accordion, or a centred empty state with a 36 px SearchX icon and
`FaqsNoResults` at 13 px, 12 px below the icon.

---

## Settings

`SettingsViewModel` + `SettingsView.xaml`. Also the holder of the active string
set and the active theme, which makes it the most widely observed ViewModel in
the application.

### State

| Member | Type | Default | Persisted |
| --- | --- | --- | --- |
| `Language` | enum, 8 values | `Esp` | yes |
| `Strings` | the resolved string set | `Esp` | derived from `Language` |
| `Theme` | enum, 10 values | `Volcarona` | yes |
| `StartWithWindows` | `bool` | read from the registry, not from the file | registry |
| `StartMinimized` | `bool` | `false` | yes |
| `AutoUpdate` | `bool` | `false` | yes |
| `DownloadDirectory` | `string` | the user profile folder | yes |
| `GamesRootDirectory` | `string` | derived: `<DownloadDirectory>/LostieLauncher` | no |
| `IsDownloadDirectoryOneDriveSynced` | `bool` | computed | no |
| `HasSeenWelcome` | `bool` | `false` | yes, privately |

`LanguageOptions` and `ThemeOptions` are the full enum value sets, exposed
statically for the two combo boxes.

### Loading and saving

Loading sets a suppression flag so the change hooks do not write back what was
just read. It reads the settings file, **reads `StartWithWindows` from the
registry rather than from the file** — the registry is the truth about whether
the entry exists — ensures the games root exists, and refreshes the OneDrive
state.

Every property has a change hook that logs and saves. Saving is debounced by
500 ms in the service; see section 03.

Three hooks do more than save:

- **Language** re-resolves `Strings` to the matching implementation, then saves.
  Everything observing `Strings` or `Language` reacts.
- **Theme** applies the theme immediately, then saves. A theme that fails to
  apply falls back to `Volcarona` and logs; if that fails too, nothing changes.
- **Start with Windows** writes or deletes the registry entry *first*. **If the
  write fails, the toggle is reverted to the real registry state** and nothing is
  saved, so the UI can never show "on" for a startup entry that was never
  written.

### Actions

| Action | Effect |
| --- | --- |
| Browse download directory | see below |
| Check for updates | blocked with `NotifyDownloadInProgress` while a download runs; otherwise a check with `notifyWhenUpToDate: true` |

**Browse** is a four-step gate:

1. Confirm the change at all (`ChangeDownloadDir*`, yes/no). On no, stop.
2. Open a folder picker seeded with the current directory. On cancel, stop.
3. **Probe the picked folder** with the write-and-rename check. On failure,
   log, show `DownloadDirNotUsable*` and keep the old folder.
4. If the folder is OneDrive-synced, warn (`OneDriveWarning*`, yes/no) and keep
   the old folder unless the user insists.

Only then is `DownloadDirectory` assigned, which saves, ensures the new games
root exists, refreshes the OneDrive banner state, and — through `MainViewModel`
— refreshes the Library.

### Layout

A single scroller, content padded `20,20,20,4`. Two sections, each a
section title at 14 px SemiBold in the **accent** colour with 4 px left inset and
8 px below, then a `SecondaryBgBrush` card at radius 6 whose rows are separated
by 1 px `OverlayLightBrush` dividers inset 16 px horizontally. Rows are padded
`16,14`.

**General** (24 px below the card):

| Row | Icon | Label | Control |
| --- | --- | --- | --- |
| 1 | Launch | `SettingsStartWithWindows` | toggle switch |
| 2 | WindowMinimize | `SettingsStartMinimized` | toggle switch |
| 3 | Update | `SettingsAutoUpdate` | toggle switch |
| 4 | Refresh | the current version string, e.g. `v0.9.1` | button `SettingsCheckForUpdates` |
| 5 | FolderDownload | `SettingsDownloadDir` | read-only path box + `BtnBrowse` button |

Row 5 is taller and stacks: the label with 10 px below, then the path box and
button on one line (button 8 px to the right of the box), then a line reading
`SettingsGamesStoredIn` in the dim foreground at 12 px followed by the resolved
games root at 12 px, ellipsised with the full path as a tooltip. Below that, only
when the library sits under OneDrive, an `OverlayLightBrush` box at radius 4 with
`10,8` padding holding a 14 px Alert icon and `SettingsOneDriveWarning` wrapped
at a maximum width of 520.

**Appearance**:

| Row | Icon | Label | Control |
| --- | --- | --- | --- |
| 1 | Translate | `SettingsLanguage` | combo box of the 8 languages, showing each language's own endonym |
| 2 | Palette | `SettingsTheme` | combo box of the 10 themes, showing the enum name |

The language combo shows the description attached to each enum value —
`Español`, `English`, `Català`, `Euskera`, `Galego`, `Português`, `Valencià`,
`Français`. The theme combo shows the bare names, which are not localized and
not meant to be: `Volcarona`, `Zoroark`, `Infernape`, `Torterra`, `Empoleon`,
`Mewtwo`, `Cefireon`, `Sylveon`, `Astrem`, `Auretoskos`.

Both combos are 160 px minimum width, 34 px tall, 12 px text, right-aligned in
their row; their anatomy is in section 06.
