# Android code style

[.editorconfig](../.editorconfig) holds IDE settings. The explicit
`ktlintSettings` map in [build.gradle.kts](../build.gradle.kts) supplies
Spotless overrides; keep the duplicated settings aligned. Use
`spotlessApply` to format and `spotlessCheck` to verify.

## Kotlin

- Prefer `val`, immutable domain `data class` values and read-only collection
  signatures. Keep serialization representations separate from domain models.
- Model nullability with safe calls, Elvis expressions or exhaustive branches.
  Do not use `!!` to silence a missing null case.
- Use `sealed interface` for closed states/results. Preserve enum display order
  when it is part of the product; settings persist names rather than ordinals.
- Prefer expression bodies when readable, extension functions where appropriate,
  and the surrounding code's established pure utility objects.
- Keep visibility narrow, starting with `private` then `internal`. Omit redundant
  `public`. Tests already see module internals.
- Keep explicit, alphabetically ordered imports without wildcard groups.
  Run the format gate as well as the compiler to catch import problems.
- Use the configured `intellij_idea` style and 120-character line limit.
  `@Composable` functions are exempted from lowercase function naming.

## Compose

- Composable UI functions use PascalCase, return `Unit`, and take
  `modifier: Modifier = Modifier` as the first optional parameter.
- Hoist state to its appropriate owner; `remember` is for local disposable state.
- Follow [ui.md](ui.md) for ViewModel boundaries, layout and visual tokens.
  Read [localization.md](localization.md) for catalogue text and `withArgs`.

## Comments and documentation

- Keep comments and KDoc only when they explain a necessary constraint or
  decision. Remove redundant narration when editing; preserve useful rationale
  rather than deleting comments merely because the file changed.
- Longer explanations belong in `docs/`. Short build-script explanations may
  record constraints the configuration itself does not explain.
- Do not leave commented-out code or obsolete runtime TODO markers behind.
  Current limitations live in [game runtime](../docs/game-runtime-options.md).
