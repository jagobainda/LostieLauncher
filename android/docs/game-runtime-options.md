# Android game installation and launch: open decisions

Step 09 defines contracts and inert adapters. It does not install, execute, remove,
or inspect a game. The existing CDN catalogue describes ZIP archives containing
Windows games; the desktop expects `Game.exe`. An Android runtime or Android game
artifact has not been identified. A completed download currently stays an archive
in the app's download directory, and its Room download row remains `COMPLETED`.
That state means the transfer finished, not that the game is playable.

The searchable marker `TODO-ANDROID-GAME-RUNTIME` has exactly eleven numbered
questions. Each marker occurs once in code, beside the deferred method or handoff.
The contracts live in `service/game/`, their result types in
`model/GameOperations.kt`, and Hilt binds the pending implementations in
`core/di/GameOperationsModule.kt`. Every pending operation logs its invocation
and returns `NotSupportedYet`; none touches the archive, registry, packages, or
playtime. `InstallDownloadedFileHandoff` is the only completed-download entry
point. Future presentation code must call these services for game lifecycle
operations and handle `NotSupportedYet` explicitly.

## Desktop behavior that the seam preserves

| Concern | Desktop behavior | Android contract |
| --- | --- | --- |
| Installation | `LibraryViewModel.HandleDownloadSuccessAsync` chooses the catalogue hash or special-version config hash, requires 64 hex characters, hashes the ZIP, deletes it on failure, then marks verification/extraction states. `GameArchiveInstaller` extracts with a path-containment check into `.tmp`, swaps the old folder into `.old`, and registers the GUID, name, version and optional type after extraction. | `GameInstallationService.install(GameInstallationRequest)` distinguishes missing archive, invalid hash, mismatch, extraction failure, registry failure, success and unsupported. The request carries the archive, catalogue GUID, expected hash and variant separately. |
| Installed lookup | `ContentService.GetLocalGamesAsync` reads the local registry, deduplicates by GUID or case-insensitive name and returns the stored version. `GamesViewModel` joins it to the catalogue to mark updates, including games no longer in the catalogue. | `GameInstallationService.findInstalled(GameTarget)` returns a single installed record with version, absent, or unsupported. `installedGames` exposes the full observable list, including off-catalogue games; the pending adapter emits `NotSupportedYet`. |
| Uninstall | `GamesViewModel.GetRunningSignal` blocks a tracked live process and warns on a locked executable. `UninstallCoreAsync` retries folder deletion. If nothing was deleted, registration remains; if deletion was partial, registration is removed and the blocking path is reported; a missing folder has its own outcome. | `GameInstallationService.uninstall` retains the five desktop outcomes plus unsupported and an optional opaque blocking location that can be passed to `GameLocationService.openBlockingLocation`. |
| Launch and running state | `GamesViewModel.Play` checks `Game.exe`, starts it in its own working directory and minimizes the launcher. It tracks the `Process`; a file-lock probe gives a weaker maybe-running signal after the tracked process is gone. | `GameLaunchService.launch` distinguishes launched, missing, failed and unsupported. `runningSignal` preserves stopped, tracked, possible and unsupported as separate values. `activeSessions` exposes a flow for global state; the pending adapter emits `NotSupportedYet` rather than an empty set. |
| Playtime | On process exit the desktop computes whole elapsed UTC minutes, ignores nonpositive durations and empty GUIDs, records positive minutes through `ContentService.AddPlaytimeAsync`, updates both cards and restores the window. | `PlaySessionService.recordCompletedSession` takes typed GUID and endpoints and returns recorded minutes, ignored or unsupported. The existing `LocalLibraryStore` is the eventual ledger; no direct caller writes it for game sessions yet. |
| Locations | `GamesViewModel.OpenFolderAsync` opens the game directory, offering a download if it is absent. `OpenHelpFolder` looks for a case-insensitive `ayuda` child; its availability controls the help button. `FolderLauncher` refuses missing directories before asking Explorer to open them. | `GameLocationService.helpAvailability` distinguishes available, absent and unsupported. `open` identifies game or help, while `openBlockingLocation` handles an opaque uninstall location. The offer to download remains a presentation decision. |

The desktop implementation is the authority for these details:
`desktop/LostieLauncher/ViewModels/LibraryViewModel.cs`,
`desktop/LostieLauncher/ViewModels/GamesViewModel.cs`,
`desktop/LostieLauncher/Utils/GameArchiveInstaller.cs`,
`desktop/LostieLauncher/Utils/FileLockProbe.cs`, and
`desktop/LostieLauncher/Utils/FolderLauncher.cs`.

The installation phase is also observable: `observeInstallation(gameId)` emits
`NotStarted`, `VerifyingIntegrity`, `Extracting` or `Finished(result)`. The
pending adapter always emits `Finished(NotSupportedYet)`, so a screen can show
the unsupported outcome without depending on a transient worker log or a
particular activity lifetime. Presentation must only show that state for a game
with a `COMPLETED` download row; the pending adapter cannot infer whether a
download was attempted from a game id alone. A later real adapter must replay
its latest phase and terminal result after process recreation.

## The eleven open items

1. **TODO-ANDROID-GAME-RUNTIME-01 — artifact and installation.** Is the CDN to
   publish Android APKs, game data consumed by a bundled Android runtime, or
   merely a link to another distribution channel? The present Windows ZIP cannot
   become an Android app by extraction. Any archive strategy still needs the
   desktop's expected-hash check and traversal protection, an atomic or
   recoverable update, sufficient space for archive plus extracted data, and a
   defined cleanup policy. If games are APKs, package installation is a system
   transaction with user participation; it is not a folder swap.
2. **TODO-ANDROID-GAME-RUNTIME-02 — uninstall.** For app-owned game data,
   decide whether to remove files and the Room record together, and what to
   report after partial deletion. For a separately installed package, decide
   whether the launcher requests system uninstall or simply removes its own
   association. Package removal can require the user's action and may be denied.
3. **TODO-ANDROID-GAME-RUNTIME-03 — installed identity and version.** Decide
   whether the Room record, files, Android `PackageManager`, or a combination is
   authoritative. A stale Room entry cannot prove an APK or files still exist.
   The catalogue GUID/name and Android package name/version need an explicit
   mapping. Package queries are filtered on Android 11+ unless appropriate
   visibility is declared.
4. **TODO-ANDROID-GAME-RUNTIME-04 — launch.** Decide whether Play opens an
   installed package via a user-driven intent, enters a runtime bundled with
   the launcher, or opens an external listing. An arbitrary `Game.exe` has no
   Android launch target. A worker finishing a download in the background must
   not silently start an activity; the user should initiate or resume the flow.
5. **TODO-ANDROID-GAME-RUNTIME-05 — running signal.** A process handle and
   exclusive executable lock do not exist for another Android app. Decide if a
   launcher-tracked session is sufficient, if an optional usage-access grant is
   warranted for a weaker cross-app signal, or if the middle signal disappears.
   Never report `NOT_RUNNING` simply because visibility or permission is absent.
6. **TODO-ANDROID-GAME-RUNTIME-06 — playtime.** Decide what marks session start
   and end when Android can kill the launcher while another app is foreground.
   Activity callbacks alone do not prove the game exited. Usage events require
   user-granted access and only have a limited retention window. If there is no
   reliable signal, do not write invented minutes into `LocalLibraryStore`.
7. **TODO-ANDROID-GAME-RUNTIME-07 — game and help locations.** Decide whether
   these buttons expose app-owned files through a scoped `content://` URI and a
   document handler, show an in-app help viewer, or disappear. A Windows-style
   absolute path cannot be passed to an arbitrary Android file manager. If the
   library moves to a user-selected document tree, access is URI based and its
   persistence and provider behavior need a separate design.
8. **TODO-ANDROID-GAME-RUNTIME-08 — durable handoff metadata.** The download
   table currently stores the slug, display name, version, key and archive path,
   but not the catalogue GUID, expected SHA-256 or special-version label. The
   handoff therefore passes these three inputs as `null` to the pending installer.
   Decide how to persist a verified immutable snapshot before enqueuing, recover
   it after process death, and retry an installation failure without treating a
   completed download as an installed game. Any future Room schema change needs
   a migration and exported schema, as step 08 established.
9. **TODO-ANDROID-GAME-RUNTIME-09 — installation state.** The contract now
   exposes verification, extraction and terminal outcomes. Decide where a real
   installer stores and replays them because the download worker may finish
   while no screen is alive. The pending adapter can replay unsupported without
   storage; a real adapter must retain per-game terminal state across process
   recreation.
10. **TODO-ANDROID-GAME-RUNTIME-10 — help availability.** Decide whether an
    Android game can contain browsable help files or has help inside its app,
    because the desktop's `ayuda` directory cannot be assumed for a package.
    The result must drive the card's help action without probing storage from
    presentation.
11. **TODO-ANDROID-GAME-RUNTIME-11 — partial-uninstall location.** Decide how
    to identify and expose a blocker after partial removal because it might be
    an app-owned file, a document URI or a package. The UI receives an opaque
    `GameLocationReference` and returns it to `GameLocationService`; it does
    not interpret or open a raw path.

## Approaches and consequences

| Approach | Permissions and platform limits | Storage and distribution consequences |
| --- | --- | --- |
| Publish each game as an Android package | A launcher that requests APK installation needs the platform's installation flow and, for applicable intents, `REQUEST_INSTALL_PACKAGES`; user consent is required. Querying installed packages requires declared visibility. Launch uses an activity intent, not `Process.Start`. | The archive format, package identity, signing, version mapping, update ownership and uninstall flow all change. Google Play restricts the install permission to qualifying core functionality and prohibits downloading executable dex/JAR/native code outside Play. Store eligibility must be established before choosing this. |
| Bundle an Android-capable game runtime; download data only | The runtime is part of the app; downloaded data is not executable code. No package-install permission is implied. Session tracking can be inside the app, though lifecycle and process death still matter. | Games must be ported or expressed as data the bundled runtime understands. The existing app-specific external `gamesRoot` needs no storage permission but is deleted on launcher uninstall and may be unavailable when external storage is unmounted. Play distribution of the runtime and downloaded content needs review against the actual payload. |
| Use another store, website, or companion app | A user-driven HTTPS link or intent can transfer control without package-install permission. Detecting whether the companion is installed may need package visibility. | The launcher cannot claim that a CDN ZIP is installed. Updates, uninstall and playtime may belong to the other app or store; local download could be unnecessary. Association between catalogue entries and package/listing IDs must be defined. |
| Keep downloads as archives only | No new permissions or Android execution mechanism. | Step 08 continues to provide a real transfer, but Play, installed status, game uninstall, running detection and automatic playtime remain unsupported. The UI must say the archive is downloaded only. |

The current step 07 storage decision remains in force until intentionally changed:
`StorageLocations.gamesRoot` is app-specific external storage with an internal
fallback; there is no picker. App-specific files are removed when the launcher is
uninstalled. Shared documents selected with the Storage Access Framework can
survive uninstall, but that would be a separate storage migration rather than
an incidental consequence of installation.

## Platform sources checked on 2026-09-23

- [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific) and [shared documents](https://developer.android.com/training/data-storage/shared/documents-files): permissions, availability, URI-based sharing and uninstall behavior.
- [PackageInstaller](https://developer.android.com/reference/android/content/pm/PackageInstaller), [alternative distribution](https://developer.android.com/distribute/marketing-tools/alternative-distribution) and [Google Play install-package permission policy](https://support.google.com/googleplay/android-developer/answer/16558241): installation, user choice and distribution limits.
- [Google Play device and network abuse policy](https://support.google.com/googleplay/android-developer/answer/16559646): limits on executable code downloaded outside Play.
- [Package visibility](https://developer.android.com/training/package-visibility/declaring), [package launch intents](https://developer.android.com/reference/android/content/pm/PackageManager) and [background activity launch restrictions](https://developer.android.com/guide/components/activities/secure-bal): discovery and launch limits.
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager): user-granted usage access and event retention.
