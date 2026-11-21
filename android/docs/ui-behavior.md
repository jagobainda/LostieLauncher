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
Icon-only close controls have localized spoken labels. Key fields use saveable
UI state, and pending Library/My Games prompts, the post-storage-access action,
the un-dismissed welcome dialog and FAQ search/expansion state survive process
death via `SavedStateHandle` or DataStore. Transient update-check notices stay
in memory.

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
There is no Windows startup setting, tray setting or download-directory picker.
The auto-update row can toggle as a whole and preserves switch semantics.

## Android visual adaptations

- Layout uses dp and text uses sp; equal numeric values are not a claim of
  equal physical size to WPF DIPs.
- Hover colors apply to pointers; touch adds press/long-press feedback.
- Theme-wide typography avoids unwanted Material spacing.
- Light-theme inputs use `inputSurface` outlines when input/page backgrounds
  match. See [palette review](palette-review.md).
- Icon-only controls have spoken labels, long titles ellipsize, and text scale
  must be exercised alongside narrow layouts.

Storage access, Kirin and the installation pipeline are documented in
[game runtime](game-runtime-options.md), including the difference between
app-specific archive storage and shared installed-game storage.
