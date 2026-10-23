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

These shared files survive launcher uninstall. Archives use the separate,
app-specific `StorageLocations.downloadsRoot/downloads`, which is removed with
the app. Logs use private `noBackupFilesDir` storage.

`InstallDownloadedFileHandoff`, in `service/download/DownloadedFileHandoff.kt`,
resolves the catalogue GUID, expected hash and special-version type when a
transfer completes. The installer rejects a missing/invalid hash or mismatch,
extracts under `.tmp` with path-containment checks, moves a previous installation
to a unique `.old-<UUID>` directory, and cleans old directories in the background.
A `.nomedia` marker keeps game images out of media scanning.

`KirinGameLibrary` merges entries by path. Kirin launches with the configured
action, activity and game-path extra in `KirinOptions`. The launcher neither
executes `Game.exe` itself nor installs each game as an Android package.

## Permissions and runtime setup

The implementation checks shared-storage access through `StorageAccess`:
all-files access on Android 11+, or legacy write permission on older supported
versions. User-started install/update waits for that access. Notification
permission affects progress visibility and is a separate request.

`KirinSetupService` checks server requirements and the installed Kirin version.
`KirinRequirement` blocks the shell for missing/outdated Kirin; a recommended
update is nonblocking. Kirin and launcher updates use separate instances of the
shared APK installer engine, followed by Android's system installation UI.

## Remaining limitations

- Installation phases and terminal results are held in memory, not replayed
  from durable storage after process recreation.
- The Room download row does not persist the catalogue GUID, expected SHA-256
  or resolved special-version type as an immutable enqueue-time snapshot.
- Per-game installed lookup, running detection and blocker opening remain
  unsupported as shown above.
- Installation failure and retry behavior still need a complete recovery
  contract; a durable `COMPLETED` transfer is not proof of installation.
- Registry entries provide the installed-list projection; they are not a
  filesystem validation of every game directory.

The former document referred to eleven numbered runtime TODO markers. Those
markers are no longer present in production source; use the bindings and the
limitations above rather than treating that old count as an active checklist.
