# 08 · Dialogs and notices

Extracted from `desktop/LostieLauncher/Views/Dialogs/` and
`desktop/LostieLauncher/Styles/DialogStyles.xaml`.

Four window types. Every one of them is modal, centred on screen, not resizable,
chrome-less with a 1 px accent border, and painted with `PrimaryBgBrush`.

## Shared anatomy

All four share the same three-row skeleton:

| Row | Height | Content |
| --- | --- | --- |
| 0 | **50** | title bar |
| 1 | star | body |
| 2 | **60** | footer, buttons right-aligned, margin `20,0,20,20` |

The title bar is draggable on a single left click, and carries an 18 px accent
icon, 8 px to its right the title at 13 px SemiBold in `SecondaryFgBrush`, and
on the far right a 50 × 50 close button that hovers to `#E81123`.

**Ownership.** Each dialog takes the main window as its owner when that window
is loaded. The message box goes further: if the main window is not both loaded
**and** visible — which happens when the launcher started minimized to tray — it
makes itself topmost and activates on load instead, so a dialog can never be
raised behind everything with no way to reach it.

**Keyboard.** `Enter` confirms and `Escape` cancels in all four. On a yes/no
message box, `Y` and `N` answer directly.

The Android port has no chrome to draw and no owner to set, but the two
behaviours behind them still matter: a dialog must be reachable when the app is
not in the foreground, and confirm/dismiss must work from the keyboard or the
system back gesture.

---

## Message box

The general-purpose dialog. Everything in sections 02 and 03 that "shows a
dialog" means this one.

**Size.** Width **420**, height sized to the content between **220** and
**560**. The body is a vertically centred scroller with 20 px horizontal margin
holding wrapped 13 px text in `SecondaryFgBrush`. It grows with the message and
only scrolls once it hits the maximum — the uninstall errors interpolate full
filesystem paths and would otherwise be clipped.

**Two button sets:**

| Set | Footer |
| --- | --- |
| OK | one 100 px accent button, `BtnOk` |
| YesNo | a 90 px accent `BtnYes`, 8 px gap, a 90 px secondary `BtnNo` |

**Three icons**, all 18 px in the accent colour, differing only in glyph:

| Icon | Glyph |
| --- | --- |
| `Update` (the default) | Update |
| `Information` | Information |
| `Error` | Alert |

The severity is carried by the glyph alone — the icon is not recoloured, and
there is no red error styling anywhere in the dialog.

**Results.** Yes → true, No → false, Escape on a yes/no → false, the close
button or Escape on an OK dialog → no result at all. Callers treat only an
explicit true as consent.

### Every message box in the application

| Title / message keys | Buttons | Icon | Raised from |
| --- | --- | --- | --- |
| `FolderNotFound*` | YesNo | Information | opening a missing game folder |
| `UninstallConfirm*` | YesNo | Information | uninstall, game not detected running |
| `UninstallGameRunningTitle` + `UninstallMaybeRunningMessage` | YesNo | Error | uninstall, executable locked |
| `UninstallGameRunning*` | OK | Error | uninstall refused, process tracked |
| `UninstallNotFound*` | OK | Information | uninstall, folder was already gone |
| `UninstallError*` | YesNo | Error | uninstall, some files left behind |
| `UninstallBlocked*` | YesNo | Error | uninstall, nothing could be deleted |
| `CancelDownloadConfirm*` | YesNo | Information | cancelling a download |
| `DownloadError*` | OK | Error | transfer failed, install failed, or a bad special-version response |
| `DownloadPermissionDenied*` | OK | Error | transfer denied by permissions |
| `DownloadKeyInvalid*` | OK | Error | key fails the format check |
| `DownloadKeyNotFound*` | OK | Error | key unknown, or its config is malformed |
| `DownloadKeyMismatch*` | OK | Error | the key belongs to a different game |
| `HashMismatch*` | OK | Error | integrity verification failed or was refused |
| `GameExeNotFound*` | OK | Error | `Game.exe` missing on play |
| `ServerActionsUnavailable*` | OK | Information | the maintenance flag is set — shown **once** per blocked streak |
| `ChangeDownloadDir*` | YesNo | Information | before opening the folder picker |
| `OneDriveWarning*` | YesNo | Information | the picked folder is OneDrive-synced |
| `DownloadDirNotUsable*` | OK | Error | the write-and-rename pre-flight failed |
| `UpToDate*` | OK | Information | manual update check, already current |
| `UpdateCheckFailed*` | OK | Error | manual update check threw |
| `UpdateCheckBusy*` | OK | Information | update check while a download runs |
| `UpdateAvailable*` | YesNo | Update | a launcher update is ready to apply |
| `ExitWarningTitle` + one of three messages | YesNo | Error | exiting while downloading, playing, or both |

**Twenty-four distinct message boxes, raised from 29 call sites.** Five of the
twenty-four are raised from two places each: four because the key-validation
chain is written out once for *start download* and again for *switch to special
version* (`DownloadKeyInvalid*`, `DownloadKeyNotFound*`, `DownloadKeyMismatch*`
and the mapped special-version error), and `DownloadError*` because a failed
transfer and a failed installation report it independently. Eight of the
twenty-four interpolate an argument.

There is exactly one non-localized message box, and it is not in this table: the
pre-startup fatal error, in English, because it can fire before the string
catalogue exists.

---

## Download confirmation

**480 × 420.** Title icon Download, title `DownloadDialogTitle`. Shown before
every fresh download; **not** shown when resuming a paused one.

The body is a scroller with 20 px horizontal margin holding four blocks.

**1 — Game header**, 16 px below. A 90 × 72 logo well at radius 6 on
`TertiaryBgBrush`, clipped, 5 px padding, top-aligned, holding either a 28 px
Pokéball in `OverlayMediumBrush` or the loaded image. Beside it, inset 14 px:

- the game name at 16 px Bold, ellipsised;
- 4 px below, the description at 12 px in the dim foreground, wrapped, capped at
  **100 px tall**, falling back to `DownloadDialogNoDescription` when the
  catalogue description is blank;
- 6 px below, a link button — an 11 px accent OpenInNew icon, 4 px, then
  `DownloadDialogViewPage` at 12 px accent underlined — **hidden entirely when
  the catalogue entry has no page URL**. It opens that URL, and only if it is
  absolute HTTPS.

**2 — Destination**, 14 px below. `DownloadDialogPath` at 12 px dim with 4 px
below, then a `TertiaryBgBrush` box at radius 4 with `10,8` padding holding the
resolved install path at 12 px, ellipsised, with the full path as its tooltip.

**3 — Size and free space.** Two equal columns, each a 14 px dim icon, 6 px, a
dim 12 px label ending in a colon, 4 px, then the value at 12 px SemiBold in
`SecondaryFgBrush`:

| Column | Icon | Label | Value |
| --- | --- | --- | --- |
| left | Harddisk | `DownloadDialogGameSize` | the catalogue size |
| right | DatabaseCheck | `DownloadDialogFreeSpace` | free space on the destination drive |

Free space is formatted like the game size — `{0:0.#} GB` at or above 1 GB,
otherwise `{0:0} MB` — in invariant culture, and renders as an em dash when the
drive cannot be queried. **It is displayed, never enforced**: the dialog does
not block a download that would not fit.

**4 — Key**, 30 px below. A 12 px dim Key icon, 6 px, `DownloadDialogKey` at
12 px dim, then 4 px below the key input. It is always present; the field is
simply left empty for a regular download.

**Footer.** A 120 px accent `BtnDownload` and, 8 px right, a 100 px secondary
`BtnCancel`.

**Result.** Confirming returns the download arguments with the trimmed key
attached, or with no key when the field is empty or whitespace. Cancelling,
closing, or Escape returns nothing and the download never starts. Validation of
the key happens afterwards, in the Library — the dialog accepts any text.

---

## Special version

**440 × 250.** Title icon Key, title `SpecialVersionDialogTitle`. Raised from
*My Games* to switch an installed game to an alternative build.

Body, vertically centred with 20 px horizontal margin:
`SpecialVersionDialogDescription` at 12 px in the dim foreground, wrapped, with
20 px below; then a 12 px dim Key icon, 6 px, `SpecialVersionDialogKeyLabel` at
12 px dim, and 4 px below the key input.

Footer: a 120 px accent `BtnConfirm` and, 8 px right, a 100 px secondary
`BtnCancel`.

**Result.** The trimmed key, or nothing. Unlike the download dialog, this one
**refuses to confirm on an empty field** — both the button and `Enter` do
nothing rather than closing with no key. Format validation still happens
afterwards in the Library.

---

## Welcome

**480 × 500.** Title icon PartyPopper, title `WelcomeDialogTitle`. Shown once,
on the first run, immediately after the launcher navigates to the Library and
marks the welcome as seen. It is bound to the settings ViewModel, so the
language picker inside it is live.

Body, a scroller with margin `24,8,24,0`:

1. **Brand block**, centred, margin `0,12,0,20` — a **64 × 64** tile at radius
   **16** on `TertiaryBgBrush` holding a 30 px accent Gamepad icon, 12 px below
   it the literal `"Lostie Launcher"` at 20 px Bold.
2. **Description** — `WelcomeDialogDescription` at 12 px in the dim foreground,
   wrapped, with an explicit **line height of 18** and 12 px below. This is the
   only place in the application that sets line height.
3. **Language row**, 12 px below — a dim Translate icon, 10 px,
   `SettingsLanguage` at 13 px, and a right-aligned combo box identical to the
   one in Settings. Choosing here changes the application language immediately,
   including the dialog's own text.
4. **Repository link**, left-aligned — a 12 px accent GitHub icon, 4 px, then
   the literal `github.com/jagobainda/LostieLauncher` at 12 px accent
   underlined. It opens `RepositoryUrl` from the string catalogue.

**Footer.** Unlike the other three, it has a **`SecondaryBgBrush` background**
with `20,0` padding, and holds a single 160 px accent button
`WelcomeDialogContinue`. There is no cancel: continue, the close button and
Escape all simply close it, and it never appears again because the flag was
already persisted before it was shown.

---

## Notices that are not dialogs

Three, for completeness, because they carry messages the user must read:

- **The offline pill** in the title bar — section 01.
- **The offline banner and the stale-content banner** on Home — section 02.
- **The OneDrive warning box** in Settings — section 02. Unlike the OneDrive
  *dialog*, this one is persistent and informational: it appears whenever the
  configured library already sits under a synced folder, including for a user
  who installed while an older default put it there, so they are told rather
  than silently left in that state.
