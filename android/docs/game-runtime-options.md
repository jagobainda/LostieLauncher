# Android game runtime

This reference describes the current implementation, checked against
[GameOperationsModule.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/GameOperationsModule.kt)
and its adapters. Installation and launch are implemented through Kirin; they
are no longer an undecided choice among APKs, bundled runtimes and archives.
This is a source review, not evidence of a fresh device run.

## Current operations

| Operation | Implementation | Current behavior |
| --- | --- | --- |
| Install/update | `ArchiveGameInstallationService` | Validates SHA-256, extracts a ZIP, swaps directories, registers in Kirin and the local library |
| Installed list | `ArchiveGameInstallationService.installedGames` | Publishes the local registry; refreshes at startup, on foreground entry and after install/uninstall |
| Uninstall | `ArchiveGameInstallationService.uninstall` | Removes or moves the game directory aside, updates both registries, reports missing/blocked/partial removal |
| Individual lookup | inherited `PendingGameInstallationService.findInstalled` | Returns `NotSupportedYet` |
| Launch | `KirinGameLaunchService` and `AndroidKirinShortcut` | Checks the game directory and starts Kirin's explicit shortcut intent |
| Running detection / active sessions | inherited `PendingGameLaunchService` | Reports unsupported, not a known stopped state |
| Open a partial-uninstall blocker | `PendingGameLocationService` | Reports unsupported |
| Playtime | no accounting implementation | Not tracked; there is no reliable supported session-end signal |

Pending classes remain as delegated fallbacks. Their existence does not mean
all operations are pending. In particular, installed-list loading and individual
lookup have different implementations.

## Storage and handoff

[KirinModule.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/KirinModule.kt)
resolves the current user's shared storage root and configures:

- games: `LostieLauncher/<slug>`;
- local registry: `LostieLauncher/local_games.json`;
- Kirin registry: `Kirin/game-library.json`.

These shared files survive launcher uninstall. Archives use private
`noBackupFilesDir` storage at `StorageLocations.downloadsRoot/downloads`, which
is removed with the app and isolated from other apps on all supported Android
versions; any legacy external cache is discarded during maintenance or before
reusing a pre-upgrade row. Logs use private `noBackupFilesDir` storage.

`InstallDownloadedFileHandoff`, in `service/download/DownloadedFileHandoff.kt`,
resolves the catalogue GUID when a transfer completes. New downloads persist
the expected hash and special-version type in Room when they are enqueued;
rows migrated from schema 1 retain the former handoff-time lookup. The
installer rejects a missing/invalid hash or mismatch,
extracts under `.tmp` with path-containment checks, moves a previous installation
to a unique `.old-<UUID>` directory, and cleans old directories in the background.
A `.nomedia` marker keeps game images out of media scanning.

The Room row records the installation durably. A finished transfer becomes
`INSTALLING` before the handoff and turns `COMPLETED` only once the game is
installed; any other result turns it `INSTALLATION_FAILED`. A worker that finds
an `INSTALLING` row, for example after the process died during extraction,
installs the kept archive again instead of returning without work. The installer
refuses to start without shared-storage access and reports that cause. A failed
installation keeps its archive, except after a hash failure. The card offers
Retry, which installs the kept archive or downloads it again when it is gone,
and Cancel, which deletes it; the cache purge removes an abandoned archive. The
worker ends with a notification of the result: installed, failed or missing
storage access. When a worker starts in the background on Android 12+ (for
example after a reboot on Android 15+, where a `dataSync` foreground service
cannot start from `BOOT_COMPLETED`), it posts a regular progress notification
instead of promoting immediately, upgrades to a foreground service if the
launcher enters the foreground while it is still running, and clears the
fallback notification when it stops. Desktop instead returns the card to
Download and has no final notification.

Entry names are resolved by `ZipEntryNames` and follow desktop: a name flagged
as UTF-8 is read as UTF-8, and the Info-ZIP Unicode Path extra field replaces
the header name. Android differs in two details. It ignores a Unicode Path whose
checksum does not match the header name, as the ZIP specification asks. It reads
an unflagged name without a usable Unicode Path as code page 850, the encoding
the game archives are packed with; desktop decodes those bytes as UTF-8.

`KirinGameLibrary` merges entries by path. Kirin launches with the configured
action, activity and game-path extra in `KirinOptions`. The launcher neither
executes `Game.exe` itself nor installs each game as an Android package.

## Permissions and runtime setup

The implementation checks shared-storage access through `StorageAccess`:
all-files access on Android 11+, or legacy write permission on older supported
versions. User-started install/update and a retried installation wait for that
access. Notification permission affects progress and result visibility and is a
separate request.

`KirinSetupService` checks server requirements and the installed Kirin version.
The last valid minimum version received from the server is persisted in
DataStore; when requirements cannot be fetched, the launcher falls back to the
higher of the persisted minimum and the built-in default. Version comparisons
rank pre-releases (such as `0.4.0-beta8`) below the matching stable release.
Only stable, non-draft GitHub releases are accepted, retries against the GitHub
feed are spaced out after failures or unusable responses, and the APK asset is
selected according to `Build.SUPPORTED_ABIS` (with universal fallback and a
clear incompatible-device notice before downloading when no asset matches).
`KirinRequirement` blocks the shell when Kirin is missing, outdated or signed
with another certificate. In the last case the screen asks the user to
uninstall that copy in Android settings before installing the official release.
The launcher checks the installed signer again before handing a game to Kirin.
A recommended update is nonblocking. Kirin and launcher updates use separate instances of the
shared APK installer engine, followed by Android's system installation UI.
The engine checks the package name, version and current signing certificates of
both cached and newly downloaded APKs. Kirin must match its single pinned
certificate. A launcher update must match the complete signer set of the
running launcher. An APK with missing signing information is rejected.

## Remaining limitations

- Installation phases and terminal results are held in memory. After process
  recreation the row still says whether an installation is pending or failed,
  but not why it failed, so the failure notice is not shown again.
- The Room download row does not persist the catalogue GUID as an immutable
  enqueue-time snapshot. Rows migrated from schema 1 also lack the pinned
  expected SHA-256 and special-version type stored for new downloads.
- Per-game installed lookup, running detection and blocker opening remain
  unsupported as shown above.
- Registry entries provide the installed-list projection; they are not a
  filesystem validation of every game directory.

The former document referred to eleven numbered runtime TODO markers. Those
markers are no longer present in production source; use the bindings and the
limitations above rather than treating that old count as an active checklist.
