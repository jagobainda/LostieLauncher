# Android UI

Apply [architecture.md](architecture.md) and the shared
[localization/theme rules](../../.agents/localization-and-themes.md).

- Only screen roots and dialog hosts obtain ViewModels with `hiltViewModel()`.
  Components and dialogs receive immutable state, values and callbacks.
- `LauncherShell` hosts section content, independent dialog hosts and the
  welcome dialog. Keep notices visible even when their originating section is
  not selected or the Kirin requirement screen is showing.
- Preserve `NavigationStore` as the section owner. The shell's horizontal pager
  synchronizes settled pages and explicit navigation; do not add a second
  navigation history alongside it.
- Map ViewModel state to card/dialog contracts with pure functions. Formatting
  belongs in `util/format/`; UI mappings do not decide download or game outcomes.
- Use measured available space for navigation, cards, Home columns and dialogs.
  Reuse the existing layout functions and their JVM tests rather than branching
  on a phone/tablet label.
- Read sizes, spacing, motion and type from `ui/theme/Tokens.kt`; colors come
  from `LocalLauncherColors`. Use `sp` for font sizes and `dp` for layout.
- Provide localized labels for icon-only controls and preserve heading,
  selection and switch semantics.
- Keep desktop interaction styling, including hover for pointers and touch
  press/long-press feedback. Preserve documented Android adaptations.
- Open validated HTTPS links through `ExternalLinkService`. `LinkText` maps
  gestures and accessibility actions; it must not start platform intents itself.
- A regex change in link parsing needs an Android run: JVM regex acceptance
  does not prove ICU accepts the same pattern.
- Keep placeholders and loading skeletons stable across state transitions.
  Installation failure, unsupported capability and waiting for a network are
  separate states, with localized feedback.
- Preserve the distinction between notification permission (notification
  visibility) and shared-storage access (a prerequisite for installation).
  Read [game-runtime.md](game-runtime.md) before changing those prompts.

Current behavior and reasons for Android adaptations:
[UI behavior](../docs/ui-behavior.md).
