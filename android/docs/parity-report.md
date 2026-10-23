# Android parity report

Current capabilities reviewed against source during the agent-documentation
reorganization. This replaces the end-of-port report's obsolete claim that
installation and launch are entirely pending. It does not certify a fresh
build, device acceptance run or a fixed test total.

The desktop is the product reference. Android-specific decisions are described
below and in [UI behavior](ui-behavior.md) and [game runtime](game-runtime-options.md).

## Implemented areas

| Area | Android implementation | Relevant difference |
| --- | --- | --- |
| Catalogue and Home | `ContentService`, `LauncherDataCoordinator`, screens | Duplicate slugs are dropped; foreground lifecycle owns loading/polling |
| Maintenance/offline | maintenance API and `NetworkMonitor` | No validated internet is a separate offline signal; content reloads on reconnect |
| Downloads | Room, WorkManager, OkHttp | Resumable foreground work survives activity recreation; network interruption requeues work |
| Special versions | `SpecialVersionService`, policies and dialogs | Lookup remains separate from the archive transfer |
| Install/update | `ArchiveGameInstallationService` | Verifies ZIPs and registers extracted games for Kirin in shared storage |
| Installed list and uninstall | archive installer and local registry | Shared registry survives launcher uninstall; individual lookup is still pending |
| Launch | `KirinGameLaunchService` | Uses Kirin's explicit shortcut intent |
| Game auto-update | `GameAutoUpdateCoordinator` | Startup preference drives sequential updates of regular outdated games |
| Settings | DataStore and `SettingsViewModel` | In-app appearance state, immediate observation and debounced persistence |
| Themes/text | Kotlin palettes and language catalogues | Runtime switching without activity recreation; Android-specific FAQ and permission text |
| Navigation and dialogs | `NavigationStore`, shell pager and dialog hosts | Swipe navigation, Android back, adaptive layout and independent notice hosts |
| Search | `SearchMatcher`, FAQ ViewModel | Desktop corpus equivalence was measured during the port; see [search parity](search-parity.md) |
| Launcher/Kirin updates | shared APK installer engine | Asks before launcher download, validates package/version, opens system installer |
| Logging | injected file/Logcat logger | Private logs, retention maintenance and uncaught-exception logging |

The catalogue and build files are the source of truth for dependency versions,
supported language/theme implementations and CI tasks. Validation commands are
in [build-and-validation.md](../.agents/build-and-validation.md).

## Partial capabilities

- Individual installed lookup, running/session detection and blocker opening
  still use pending implementations. The installed list, uninstall and launch
  do not: read the [operation table](game-runtime-options.md#current-operations).
- Installation phases are in-memory and the durable download row lacks a
  resolved enqueue-time hash/catalogue snapshot. Recovery across process death
  remains a separate concern.
- Games auto-update is implemented, but stops on a server-action block without
  the interactive notice shown by manual actions.
- Open prompts and notices are held by ViewModels; they are not a durable
  process-death record.

## Deliberate omissions

| Desktop feature | Android decision |
| --- | --- |
| Tray, startup/minimized settings, window chrome and single-instance mutex | Use Android activity/task lifecycle |
| Download-folder picker and OneDrive warning | Fixed archive root; installed games use the configured shared root |
| Saved Games rail action, game-folder/help buttons | Omitted; no equivalent Explorer flow |
| Playtime | Not tracked because no reliable supported session-end signal exists |
| Exit warning | Durable background transfer replaces the Windows shutdown concern |
| WPF converters and dispatcher-driven bindings | Pure formatting functions and lifecycle-aware flow collection |
| Permanent desktop scrollbar | Omitted from the touch UI |

## Visual decisions and recorded acceptance

The original port review recorded emulator runs for a 411 x 914 dp phone at
100%/200% font scale and a 360 x 640 dp phone at 130%, in both orientations;
tablets were covered in earlier port steps. Those are historical observations,
not evidence that subsequent UI or runtime changes passed the same checks.

Retained design decisions:

- Adaptive navigation, cards, Home columns and bounded scrolling dialogs.
- Spoken names for icon-only controls and headings/selection semantics.
- The desktop-sized card buttons remain a touch-target compromise; do not
  describe the whole interface as having uniform minimum touch targets.
- Light-theme inputs get an outline without changing shared palette values.
  Other contrast findings remain in [palette review](palette-review.md).
- Pressed/hover feedback is retained alongside touch/long-press interactions.

Manual acceptance still matters for storage grants, Kirin launch, APK updates,
background transfers, ICU regexes, rotation and UI at varied sizes/font scales.
Use [testing.md](../.agents/testing.md) to distinguish those checks from JVM CI.
