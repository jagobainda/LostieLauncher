# 10 · What is Windows, and has no direct Android translation

Everything in the launcher that depends on being a Windows desktop application.
Each entry says what it does, why it exists, and what the Android side has to
decide instead. **Nothing here should be translated literally.**

Step 09 owns the seam for installing and launching games. Steps 07 and 08 own
storage and transfers. Step 15 checks that every entry here was either answered
or consciously dropped.

---

## 1 · Installing and launching a game

**Desktop:** a game is a ZIP containing a Windows executable. Installing means
extracting it into `<gamesRoot>/<Game Name>/`; launching means starting
`Game.exe` from that folder with the shell, with the working directory set to
the folder; tracking means holding the process object, subscribing to its exit,
and measuring wall-clock minutes between start and exit.

**Why it cannot move:** Android has no arbitrary executables, no user-visible
process to own, and no exit event to subscribe to. The games themselves are
Windows RPG Maker builds; there is no Android build of them to launch.

**What Android must decide:** this is explicitly **out of scope for the whole
port**. Step 09 defines the seam — an interface with the same shape as the
desktop's install/launch/track responsibilities — and leaves it unimplemented,
with deliberate TODOs. Everything that depends on it (the Play button, the
playtime ledger, the running-game signal, the exit warning about a running game)
must be expressible through that seam so the rest of the application compiles
and is testable around it.

Related constants that only make sense on desktop: the executable name
`Game.exe` and the help folder name `ayuda`.

## 2 · Startup with the operating system

**Desktop:** `IWindowsStartupService` writes the quoted executable path to
`HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Run` under the value
`LostieLauncher`. The Settings toggle reads its state **from the registry**, not
from the settings file, and reverts itself if a write fails.

**Why it cannot move:** Android has no user-controlled autostart. Boot-completed
receivers exist but are restricted, are not a user preference, and are not what
this setting means.

**What Android must decide:** most likely to **drop the setting entirely**, and
with it `SettingsStartWithWindows`. If it is dropped, the string key stays in the
catalogue or is removed in all eight languages — half-removing it is the failure
mode to avoid.

## 3 · Start minimized, and the system tray

**Desktop:** the launcher lives in the notification area for its whole
lifetime, with a two-item context menu, a double-click to restore, and a
`StartMinimized` setting that starts it hidden. Closing the window hides it
rather than exiting; only the tray menu or `Ctrl+Shift+Q` exits.

**Why it cannot move:** Android has no tray, and no concept of an application
that is running with no UI at the user's request. The back gesture is not a
"hide to tray".

**What Android must decide:** `StartMinimized`, `TrayOpen` and `TrayExit` have
no meaning. A foreground-service notification during an active download is the
nearest useful analogue, and is a step 08 decision, not a translation of this.

## 4 · Single-instance enforcement

**Desktop:** a named mutex `LostieLauncherSingleInstance` plus a named event
`LostieLauncherShowWindow`. A second launch signals the event so the running
instance restores its window, then exits. The mutex is also released and
re-acquired around a self-restart.

**Why it cannot move:** Android runs one process per application by
construction, and there are no named kernel objects to coordinate with.

**What Android must decide:** nothing — the requirement disappears. The launch
mode of the main activity covers the "bring the existing one forward" behaviour.

## 5 · Self-update through Velopack

**Desktop:** Velopack bootstraps first thing at startup, hosts install and
uninstall hooks, checks a feed at `/public/installer/`, downloads a delta or full
package, and applies it with a restart. The `AutoUpdate` setting only controls
whether **games** auto-update; the launcher's own check always runs at startup,
silently.

**Why it cannot move:** application updates on Android come from the store or
the package installer. There is no in-process updater, and no restart-into-a-new
build.

**What Android must decide:** the whole `IUpdateGateway` / `IUpdateNotifier` /
`IUpdateService` triple has no direct equivalent. The strings `UpToDate*`,
`UpdateCheckFailed*`, `UpdateCheckBusy*`, `UpdateAvailable*` and
`SettingsCheckForUpdates` go with it. The game-update flow is unaffected and
must be kept — the two are separate concerns that happen to share vocabulary.

## 6 · Custom window chrome

**Desktop:** `WindowStyle="None"` with a hand-built 50 px title bar, a drag
region, a 1 px accent border, minimize and close buttons, a fixed 1000 × 650
size and `CanMinimize` only.

**Why it cannot move:** there is no window.

**What Android must decide:** the *content* of the title bar still matters — the
screen title, the offline pill, and the per-screen social buttons all carry
meaning and must land somewhere, most likely a top app bar. The chrome itself
does not. Note that the desktop is **fixed-size and non-resizable**, so no
layout in this application has ever had to respond to a width change; every
screen will need a responsive layout decision that the WPF has no answer for.

## 7 · Filesystem model

**Desktop:** an arbitrary user-chosen absolute path, defaulting to
`%USERPROFILE%`, with the library at `<that>/LostieLauncher`. Settings live in
`%APPDATA%/LostieLauncher/`, logs in
`%LOCALAPPDATA%/<app>/logs/`. A folder picker lets the user put the library
anywhere, and the launcher probes it for write-and-rename before accepting it.

**Why it cannot move:** Android has scoped storage. There is app-private
storage that is wiped on uninstall, and shared storage that needs the storage
access framework and gives a tree URI rather than a path.

**What Android must decide** (step 07): where the library lives, whether the
user may move it, and what happens on uninstall. The desktop reasoning behind
the default — recorded in section 04 — is a *pattern* to re-apply, not a set of
paths to translate: every candidate has a hazard, and the choice is the one
whose hazards a multi-gigabyte library can survive. The specific hazards are
different on Android and must be enumerated again.

Consequences that follow the decision: the folder picker and `BtnBrowse`; the
path text box; `SettingsGamesStoredIn` and the resolved games-root display; and
whether `ChangeDownloadDir*` and `DownloadDirNotUsable*` still apply.

## 8 · The write-and-rename pre-flight

**Desktop:** before a download starts, and before a picked folder is accepted,
the launcher creates the folder, writes two bytes, renames the file and deletes
both names — because renaming needs DELETE on the file while writing does not,
so a share or a managed machine can accept gigabytes and then fail the final
rename.

**Why it may not move:** the specific ACL asymmetry is a Windows one. Whether an
equivalent trap exists on a `content://` tree is a different question with a
different answer.

**What Android must decide:** keep the *principle* — verify that the
destination can complete a transfer before starting one — and re-derive the
check. The three outcomes (`CannotCreate`, `CannotWrite`, `CannotRename`) and
their three localized step names may well collapse to fewer.

## 9 · OneDrive detection

**Desktop:** a path is flagged as OneDrive-synced by comparing it against three
environment variables and by looking for a path segment named `OneDrive` or
`OneDrive - <tenant>`. A synced library means every install is uploaded against
the user's cloud quota, the sync engine competes with extraction, and dehydrated
placeholders can make a game unplayable offline.

**Why it cannot move:** the variables and the folder convention are Windows.

**What Android must decide:** almost certainly drop it, along with
`OneDriveWarning*` and `SettingsOneDriveWarning`. If a comparable hazard exists —
a cloud-backed provider exposed through the storage access framework — it is a
different check with different evidence, not this one ported.

## 10 · Free space on a drive

**Desktop:** the download dialog reads the destination drive's available free
space and displays it beside the game size. It is informational only and never
blocks a download.

**Why it cannot move cleanly:** "the drive root of a path" is not a concept on
a scoped-storage URI.

**What Android must decide:** whether the figure is obtainable at all for the
chosen library location, and whether to keep the field. `DownloadDialogFreeSpace`
depends on the answer.

## 11 · The file-lock probe

**Desktop:** "is this game running?" falls back, when no tracked process
exists, to trying to open `Game.exe` exclusively. A sharing violation means
something holds it open.

**Why it cannot move:** no executable, no exclusive-open semantics of that kind.

**What Android must decide:** the *three-valued* answer is the part worth
keeping — tracked, maybe-running, definitely-not — because the uninstall flow
branches on it. How the middle value is obtained is a step 09 question, and it
may simply not exist.

## 12 · Time zone and culture

**Desktop:** home content expiry is evaluated in **Central European Time**,
looked up by the Windows zone id `"Central Europe Standard Time"`, falling back
to UTC when the host does not have it. Dates in the news and notification cards
are formatted with the **machine's current culture**, which is not the
launcher's own language setting.

**Why it needs attention:** the zone id is a Windows identifier; the IANA name
is `Europe/Paris` (or `Europe/Madrid` — the publisher's intent is a
Central-European wall clock, not a specific city). More importantly, an Android
device is far more likely to sit in a different zone than a desktop in this
application's audience.

**What Android must decide:** keep the expiry comparison in the publisher's zone
— that is the behaviour, and comparing in the device's zone would change which
items are visible — but format the visible dates with the **launcher's selected
language**, not the device locale. Section 04 records that the desktop's use of
machine culture here is a defect rather than a contract.

Where the desktop deliberately uses the **invariant culture** — the game size,
free space and download speed formatting — that is a contract: those numbers
must not follow the locale's decimal separator, and the Android port must be
equally explicit.

## 13 · Keyboard shortcuts

**Desktop:** nine bindings on the main window — the table in section 01 — plus
`Enter` and `Escape` on every dialog, and additionally `Y` and `N` on a yes/no
message box only.

**Why it cannot move:** a phone has no keyboard.

**What Android must decide:** drop the accelerators. Two of the behaviours they
reach are not keyboard-specific and must survive by other means: refreshing all
screens, and confirming or dismissing a dialog (the system back gesture).

## 14 · The `Saved Games` folder and shell launching

**Desktop:** a navigation-rail button opens the Windows *Saved Games* special
folder, and several actions open a folder in Explorer. Both go through a guard
that refuses anything that is not an existing directory. URLs go through a
matching guard that refuses anything that is not an absolute HTTPS URI.

**Why it partly cannot move:** there is no *Saved Games* folder and no file
explorer to hand a path to.

**What Android must decide:** drop the `Saved Games` button, and decide whether
"open the game folder" and "open the help folder" mean anything. The **HTTPS-only
guard on outbound links must be kept** — it is a security property, not a
Windows detail, and it applies to the social links, the game page link, the
repository link and every link parsed out of news text and FAQ answers.

## 15 · Non-localized literals

Three strings are deliberately outside the catalogue, and the reasons differ:

| Literal | Where | Reason |
| --- | --- | --- |
| `"Lostie Launcher"` | window title, tray tooltip, the welcome dialog's brand block | a product name, not copy |
| `"Saved Games"` | the navigation rail tooltip | it names a Windows folder by its Windows name; it disappears with entry 14 |
| the fatal-error title and message | the pre-startup crash box | it must work before the string catalogue exists |

The first survives the port. The second goes. The third's *reason* survives even
if its mechanism does not: whatever the Android port shows when it fails before
its resources are ready cannot depend on those resources.

---

## Summary — settings that may not survive

| Setting | Status on Android |
| --- | --- |
| `Language` | keep |
| `Theme` | keep |
| `DownloadDirectory` | re-decide entirely (entry 7) |
| `HasSeenWelcome` | keep |
| `AutoUpdate` (games) | keep |
| `StartWithWindows` | drop (entry 2) |
| `StartMinimized` | drop (entry 3) |

Dropping a setting means dropping its row in the Settings screen, its string key
in **all eight** languages, and its persisted field — not leaving a dead key
behind.
