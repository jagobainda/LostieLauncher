# Localization and themes

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before adding any user-visible text or
touching the theme.

> **Partial.** The text catalogue and the other nine themes arrive with port
> plan step 05. What is written here is what already binds today, plus the shape
> step 05 has to fill so that it is not designed from scratch. Everything in
> this file is subordinate to [`spec/05-localization.md`](../../spec/05-localization.md)
> and [`spec/06-design-tokens.md`](../../spec/06-design-tokens.md), which carry
> the actual values.

Both areas work the same way on the desktop and must here: a fixed set of
parallel implementations that has to stay complete, switchable at runtime, with
the choice persisted. Half-done work here is the most common reason a PR gets
sent back on the other side.

## Localization — 8 languages, no exceptions

The desktop keeps `IStrings` and `IFaqs` in `Content/`, with one implementation
per language: `Esp`, `Eng`, `Cat`, `Eus`, `Gal`, `Por`, `Val`, `Fra`. 118 string
keys and 6 FAQ entries, so 944 strings and 48 FAQ pairs in total, all of them
reproduced verbatim in `spec/05-localization.md`.

What binds now:

- **The catalogue goes in `content/`, in Kotlin — not in `res/values-xx/`.**
  The desktop switches language in-place, without restarting, and Android's
  resource qualifiers follow the system locale rather than an in-app setting.
  `app/src/main/res/values/strings.xml` therefore holds only what the platform
  itself reads before any Kotlin runs — the launcher label — and says so.
- **No user-visible literal outside the catalogue**, in a composable or a
  ViewModel. This applies today, even though the catalogue is not there yet: if
  you need text before step 05, that is a sign the work belongs to a later step.
- Placeholders keep the same count and order in all eight languages. Eight keys
  carry one; `spec/05-localization.md` names them.
- A new string means the key **and all eight translations**. English left in the
  other seven is a review rejection.
- The language selection is persisted and applied without restarting the
  application.

## Themes — 10 palettes, identical key sets

The desktop has ten resource dictionaries — Volcarona, Zoroark, Infernape,
Torterra, Empoleon, Mewtwo, Cefireon, Sylveon, Astrem, Auretoskos — each
defining the same 28 keys (14 colours and 14 brushes). Compose has no separate
brush concept for a flat fill, so the port carries the 14 colours.

What exists now:

- [`LauncherColors`](../app/src/main/kotlin/dev/jagoba/lostielauncher/ui/theme/LauncherColors.kt)
  — one property per desktop colour key, same names, same meanings.
- `VolcaronaColors` — the default and fallback palette, values exactly as the
  desktop has them, guarded by `LauncherColorsTest`.
- `LostieLauncherTheme` — provides the palette through `LocalLauncherColors` and
  projects it onto the Material 3 scheme.

What binds now, and what step 05 must preserve:

- **All ten palettes, exact values, no unification and no "improvements."** If a
  colour fails contrast on Android, it goes on a list to review at the end; it
  does not get changed on the way past.
- **Never inline a colour.** Read it from `LocalLauncherColors.current`. A
  literal will not follow the active theme.
- Adding a colour role means adding it to **all ten** palettes. Unlike the
  desktop — where a key missing from one dictionary is a runtime binding failure
  nobody sees until that theme is selected — here it is a compile error, because
  `LauncherColors` is a data class. Keep it that way.
- Theme changes apply without restarting and persist between runs.
- `LauncherColorsTest` is the pattern for the parity guard: a table of the
  desktop's own values, compared exactly. Step 05 extends it to ten palettes.
