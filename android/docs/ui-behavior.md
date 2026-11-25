# Android UI behavior

The desktop provides the product behavior and visual starting point. This
document records Android adaptations; actionable code rules are in
[ui.md](../.agents/ui.md). It describes source behavior, not a new device audit.

## Shell and sections

`LauncherShell` renders the selected section through a horizontal pager.
Explicit navigation and settled swipe pages synchronize with `NavigationStore`.
Back returns a non-Home section to Home; at Home it is left to the system.

Navigation uses a rail when measured width and height allow it, otherwise a
bottom bar. Layout functions choose from available space rather than a device
category. Home similarly switches between two columns and a combined list.

Kirin's requirement screen replaces section content while Kirin is missing or
too old. Welcome and notice hosts remain outside that gate. A recommended Kirin
update is a title-bar action; launcher APK progress is also visible in the shell.
Social labels are brand names; Twitch live status is a separate state signal.

## Cards and text

Game cards reserve action width before choosing side-by-side or stacked layout.
The status/footer and skeleton reserves reduce movement when content changes.
During an active update, Android's My Games card shows an indeterminate progress
indicator; desktop only disables the affected actions. In the stacked layout,
the indicator occupies its own full-width row above the actions, so the card
grows while the update is active.
Install/uninstall takeover animations respond to transitions, including a
departing-game row so removal does not skip the visible exit feedback.

Dates follow the selected language through `CardDateFormatter`; transfer speed
formatting is invariant. Android has no playtime item. Logos use Coil after
HTTPS validation, with the Pokeball placeholder while missing/loading/failed.

Links support tap, long press and accessibility actions. The caller opens the
canonical HTTPS URL through `ExternalLinkService`. Android's ICU regex engine
needs device verification when the parser changes.

Progress distinguishes transfer, integrity verification, extraction, an
installation waiting for its worker, terminal failure and unsupported fallback.
A pending installation offers Cancel; a failed one offers Retry and Cancel.
`INSTALLATION_UNSUPPORTED` remains a fallback contract; it no longer describes
every normal completed game download.

## Dialogs and settings

Reusable dialogs take state and callbacks, not ViewModels. Their hosts translate
notices/prompts through pure mappings and remain composed regardless of the
selected section. A host prioritizes its notice over its prompt; separate hosts
can each have a visible modal.

Dialog dimensions are capped to available space, with scrollable bodies. Back
dismisses or declines and input actions follow the on-screen confirmation.
Icon-only close controls have localized spoken labels. Pending Library/My Games
prompts, non-keyed post-storage-access actions, the un-dismissed welcome dialog
and FAQ search/expansion state survive process death via `SavedStateHandle` or
DataStore. Special-version keys stay in ViewModel memory only (they survive
rotation while the dialog is open, are never written to saved activity state,
and a keyed download waiting for storage/notification permission is discarded
across process death); transient update-check notices also stay in memory.

Key dialogs set `SecureFlagPolicy.SecureOn` on their own window. Key fields
mask input by default with a localized show/hide toggle, request password input
with suggestions and personalized learning disabled, and opt out of autofill.
Destructive confirmations (game uninstall, download cancel), key dialogs,
launcher update confirmations and permission prompts protect against overlays
by filtering obscured and partially obscured touches (API 29+) and hiding
non-system overlays on API 31+ (`HIDE_OVERLAY_WINDOWS`).

Key fields support Android's fullscreen keyboard editor on short landscape
screens. The keyboard decides whether to use it. Done submits the current
key and closing the keyboard returns to the dialog. When a partial keyboard
leaves little height, the title scrolls with the body and the actions stay
visible. FAQ search keeps its
results above the keyboard and collapses the shell title when space is short.
Leaving the section clears focus and dismisses the keyboard.

The launcher asks before downloading its own APK and then opens the system
installer. No holds for the process unless a manual update check resets the
offer. The download-progress pill is visible across sections.

Settings keeps appearance, games auto-update and launcher version/update checks.
The auto-update row can toggle as a whole and preserves switch semantics.

## Desktop features not ported

These omissions are deliberate. Do not port the Windows mechanism to Android.

| Desktop feature | Android decision |
| --- | --- |
| Tray, start with Windows/minimized, window chrome and single-instance mutex | Android activity and task lifecycle |
| Download-folder picker and OneDrive warning | Fixed archive root; installed games use the configured shared root |
| Saved Games rail action, game-folder and help buttons | Omitted; there is no equivalent Explorer flow |
| Playtime | Not tracked; see [game runtime](game-runtime-options.md#current-operations) |
| Exit warning | Downloads continue as durable background work |
| Permanent scrollbar | Omitted from the touch UI |

## Android visual adaptations

- Layout uses dp and text uses sp; equal numeric values are not a claim of
  equal physical size to WPF DIPs.
- Hover colors apply to pointers; touch adds press/long-press feedback.
- Theme-wide typography avoids unwanted Material spacing.
- Light-theme inputs use `inputSurface` outlines when input/page backgrounds
  match. See [palette review](palette-review.md).
- Icon-only controls have spoken labels, long titles ellipsize, and text scale
  must be exercised alongside narrow layouts.
- Card buttons keep desktop dimensions as a touch-target compromise; do not
  describe the interface as having uniform minimum touch targets.

Storage access, Kirin and the installation pipeline are documented in
[game runtime](game-runtime-options.md), including the difference between
app-specific archive storage and shared installed-game storage.
