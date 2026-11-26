# Android game runtime

This reference describes the current implementation, checked against
[GameOperationsModule.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/core/di/GameOperationsModule.kt)
and its adapters. Installation and launch are implemented through Kirin; they
are no longer an undecided choice among APKs, bundled runtimes and archives.
This is a source review, not evidence of a fresh device run.

## Current operations

| Operation | Implementation | Current behavior |
| --- | --- | --- |
| Install/update | `ArchiveGameInstallationService` | Validates SHA-256, extracts a ZIP while hashing extracted files, swaps directories, writes an internal integrity manifest, and registers in Kirin and the local library |
| Installed list | `ArchiveGameInstallationService.installedGames` | Reconciles `local_games.json` with internal integrity manifests (preferring manifest metadata over tampered shared entries while keeping legacy unmanifested entries); refreshes at startup, on foreground entry and after install/uninstall |
| Uninstall | `ArchiveGameInstallationService.uninstall` | Removes or moves the game directory aside, removes the internal manifest, updates both registries, reports missing/blocked/partial removal |
| Individual lookup | inherited `PendingGameInstallationService.findInstalled` | Returns `NotSupportedYet` |
| Launch | `KirinGameLaunchService` and `AndroidKirinShortcut` | Checks the game directory, Kirin's pinned signer, and full SHA-256 integrity of all manifest files (ignoring extra runtime files such as saves; allowing legacy installations without a manifest), then starts Kirin's explicit shortcut intent |
| Running detection | inherited `PendingGameLaunchService` | Reports unsupported, not a known stopped state |
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

These shared files survive launcher uninstall. Special-version builds are
extracted into the same `LostieLauncher/<slug>` directory so Kirin can open
them by path; once installed, their game files live outside the app's private
sandbox and can be read by any app with shared-storage access. The
special-version download key itself is never written to shared storage, archive
names (which use a truncated SHA-256 token) or `local_games.json` (which stores
only the variant type), and is cleared from Room once the transfer ends or is
cancelled. Archives use private
`noBackupFilesDir` storage at `StorageLocations.downloadsRoot/downloads`, which
is removed with the app and isolated from other apps on all supported Android
versions; any legacy external cache is discarded during maintenance or before
reusing a pre-upgrade row. Logs and per-game integrity manifests
(`noBackupFilesDir/manifests/<gameId>.json`) also use private `noBackupFilesDir`
storage.

During extraction, `ArchiveGameInstallationService` hashes the exact bytes
written to disk and stores each file's size and SHA-256 alongside the trusted
`id`, `name`, `version` and `variant` in the internal manifest. When building
the installed-games list, internal manifest metadata takes precedence over
`local_games.json` so modifying shared registry fields (such as setting a higher
version or a fake variant) cannot suppress mandatory updates or automatic
updates; entries without an internal manifest (installed before this change or
surviving a launcher reinstall) continue to fall back to `local_games.json`
until updated or reinstalled. Before handing a game to Kirin,
`KirinGameLaunchService` verifies every file recorded in the manifest by size
and full SHA-256 (ignoring extra files created later such as saves or runtime
caches), blocks launch on any mismatch or unreadable manifest, and offers to
reinstall the game from the Library. Legacy installations without a manifest
remain launchable. Because Kirin still opens games from shared storage by path,
this check leaves a residual TOCTOU window between the launcher's verification
and Kirin reading the files; closing that window completely would require a
Kirin-side verified handoff or private runtime import.

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
separate request. Automatic game updates observe the auto-update setting:
enabling it checks for outdated regular games and enqueues them with an
unmetered-network WorkManager constraint (`NetworkType.UNMETERED`), while
disabling it stops remaining updates and cancels any automatic update in
progress without affecting manual downloads. Manual downloads, resumes and
retries use `NetworkType.CONNECTED`.

`KirinSetupService` checks server requirements and the installed Kirin version.
The last valid minimum version received from the server is persisted in
DataStore; when requirements cannot be fetched, the launcher falls back to the
higher of the persisted minimum and the built-in default. Version comparisons
rank pre-releases (such as `0.4.0-beta8`) below the matching stable release.
Only stable, non-draft GitHub releases are accepted, requests to the
third-party GitHub release feed wait until the welcome dialog has been
completed (and trigger automatically once it is dismissed), retries against the
GitHub feed are spaced out after failures or unusable responses, and the APK
asset is selected according to `Build.SUPPORTED_ABIS` (with universal fallback
and a clear incompatible-device notice before downloading when no asset
matches).
`KirinRequirement` blocks the shell when Kirin is missing, outdated or signed
with another certificate. In the last case the screen asks the user to
uninstall that copy in Android settings before installing the official release.
The launcher checks the installed signer again before handing a game to Kirin.
A recommended update is nonblocking. Kirin and launcher updates use separate instances of the
shared APK installer engine, followed by Android's system installation UI.
Before downloading or opening the installer, the engine checks
`PackageManager.canRequestPackageInstalls()`; if missing, the UI explains the
requirement and opens Android's unknown-sources settings, resuming the
installation automatically when permission is granted.
The engine checks the package name, version and current signing certificates of
both cached and newly downloaded APKs, then targets the resolved system package
installer explicitly with `setPackage` (failing closed if none resolves).
Kirin must match its single pinned certificate. A launcher update must match the
complete signer set of the running launcher. An APK with missing signing
information is rejected. Opening the installer is not treated as success: when
the activity resumes, the engine rechecks the installed version so cancelled or
incomplete installations stay visible and retryable from the cached APK while
completed installations clear the offer and delete the cached APK.

## Android version differences

Supported SDK levels are declared in
[libs.versions.toml](../gradle/libs.versions.toml). Keeping support down to
Android 8.0 (`minSdk` 26) retains compatibility with older releases that no
longer receive platform security patches and changes several storage and
platform boundaries:

| Area | Lower API levels | Newer API levels |
| --- | --- | --- |
| Shared game storage (`LostieLauncher/`, `Kirin/game-library.json`) | API 26–29: accessed via `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="29"` in [AndroidManifest.xml](../app/src/main/AndroidManifest.xml)); any other app with that standard permission can read or modify those shared paths | API 30+: accessed via the special *All files access* grant (`MANAGE_EXTERNAL_STORAGE`); other apps need that special access to read or modify them |
| Private staging, manifests and logs (`noBackupFilesDir`) | API 26+: isolated in private internal storage and excluded from backups on every supported version | Same |
| APK signing verification ([ApkPackages.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/apk/ApkPackages.kt)) | API 26–27: reads certificates through `PackageManager.GET_SIGNATURES` | API 28+: reads signing history through `PackageManager.GET_SIGNING_CERTIFICATES` |
| Overlay / tapjacking defense ([LauncherDialog.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/ui/dialog/LauncherDialog.kt)) | API 26–28: filters `FLAG_WINDOW_IS_OBSCURED`; API 29–30 adds `FLAG_WINDOW_IS_PARTIALLY_OBSCURED` | API 31+: also hides non-system overlays with `Window.setHideOverlayWindows(true)` |
| Backup and transfer rules ([backup_rules.xml](../app/src/main/res/xml/backup_rules.xml), [data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml)) | API 26–30: governed by `fullBackupContent` (restricted to `datastore/`); `adb backup` remains available to USB debugging | API 31+: governed by `dataExtractionRules` (restricted to `datastore/`); `adb backup` excludes non-debuggable apps |
| Notifications and foreground workers ([NotificationAccess.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/download/NotificationAccess.kt), [GameDownloadWorker.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/service/download/GameDownloadWorker.kt)) | API 26–30: no runtime notification prompt and background `dataSync` foreground starts are permitted; API 31–32 restricts background foreground starts | API 33+: requires runtime `POST_NOTIFICATIONS` permission in addition to the API 31+ background foreground-service restrictions |
| TLS defaults | API 26–28: TLS 1.3 is not enabled by default (TLS 1.2 is still negotiated and cleartext is disabled) | API 29+: TLS 1.3 enabled by default |

## Remaining limitations

- Installed special-version builds reside in shared storage outside the app's
  private sandbox so Kirin can launch them by path.

- Installation phases and terminal results are held in memory. After process
  recreation the row still says whether an installation is pending or failed,
  but not why it failed, so the failure notice is not shown again.
- The Room download row does not persist the catalogue GUID as an immutable
  enqueue-time snapshot. Rows migrated from schema 1 also lack the pinned
  expected SHA-256 and special-version type stored for new downloads.
- Per-game installed lookup, running detection and blocker opening remain
  unsupported as shown above.
- Registry entries (reconciled against internal manifests when present) provide
  the installed-list projection; full file integrity verification runs on
  launch rather than scanning every installed game directory at list time.
- Shared-storage interoperability with Kirin leaves a residual TOCTOU window
  between pre-launch manifest verification and Kirin opening the game files.

The former document referred to eleven numbered runtime TODO markers. Those
markers are no longer present in production source; use the bindings and the
limitations above rather than treating that old count as an active checklist.
