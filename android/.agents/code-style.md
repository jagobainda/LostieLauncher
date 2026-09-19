# Code style

Part of the agent guidelines — see [AGENTS.md](../AGENTS.md) for the index and
the rules that always apply. Read this before writing or editing any `.kt` or
`.kts` file.

The formatter decides layout. `./gradlew spotlessApply` rewrites it,
`spotlessCheck` is the CI gate, and arguing with either is not a use of your
time. Listed below is what a formatter cannot catch.

## Where the formatting rules actually live

Two files, on purpose, and they must be kept in step:

- [`../.editorconfig`](../.editorconfig) — what the IDE reads. Inherits the
  monorepo baseline at [`../../.editorconfig`](../../.editorconfig) (UTF-8,
  CRLF, 4-space indent, final newline) and adds the Kotlin rules.
- The `ktlintSettings` map in [`../build.gradle.kts`](../build.gradle.kts) —
  what the gate reads. Spotless' ktlint step does not pick up
  `android/.editorconfig`, with or without `setEditorConfigPath`; it silently
  falls back to ktlint's defaults. The map is the workaround, and it is
  commented as one.

Settings worth knowing: ktlint's `intellij_idea` code style (so the IDE's
default formatting is accepted as-is), `max_line_length = 120`, and
`@Composable` functions exempted from the lowercase-function-name rule.

## Language usage

- **Immutability by default.** `val` unless mutation is the point, `data class`
  for models, read-only `List`/`Map` in public signatures. The domain models are
  immutable and the serialization is separate from them.
- **Nullability is modelled, not silenced.** No `!!`. Use `?.`, `?:`, `let`, or
  a `when` that handles null. A `!!` in a diff is a review comment.
- `sealed interface` for a closed set of states or results; `enum class` where
  the desktop has an enum, keeping the member order identical so persisted
  ordinals and combo-box orders line up.
- Expression bodies where they genuinely read better, block bodies where the
  expression would need wrapping.
- Extension functions over utility classes, kept in the package of the type they
  extend.
- Visibility is the narrowest that compiles: `private` first, then `internal`.
  `public` is not written out — it is the default and ktlint's style omits it.
  **Never widen visibility to make something testable**; unit tests live in the
  same module and see `internal`.
- Explicit imports only. No wildcard imports, and no unused ones. Note **which**
  gate catches which: the Kotlin compiler does not report an unused import at
  all, so `allWarningsAsErrors` never sees one — `spotlessCheck` fails it,
  through ktlint. `allWarningsAsErrors` is what turns a deprecation or an
  unchecked cast into a build break. Running only `assembleDebug` therefore
  proves nothing about imports; run the formatting gate too.

## Compose

- Composables are `PascalCase`, return `Unit`, and take `modifier: Modifier =
  Modifier` as their first optional parameter.
- A composable receives **state and lambdas**, not a ViewModel. Only a screen
  root may call `hiltViewModel()`; everything below it is stateless and
  previewable.
- Hoist state to the lowest common owner. `remember` is for what is genuinely
  local and disposable.
- **Never inline a colour, a dimension or a font size.** Colours come from
  `LocalLauncherColors.current`; the rest of the token set arrives with port
  plan step 05. A literal hex in a composable does not follow the active theme,
  which is the same bug the desktop's XAML rule prevents.
- No user-visible literal in a composable. The text catalogue is step 05's.

## Comments and docs

- Comments explain **why**, not what. A comment earns its place by recording a
  constraint or a decision the code cannot show — a desktop behaviour being
  matched, a spec section, a workaround and its reason.
- A KDoc on every interface and every seam, saying what it exists to isolate,
  and on anything whose correct use is not obvious from the signature.
- Do not narrate obvious code, and do not leave commented-out code behind.
- Everything written in English, comments and KDoc included.
