# Android localization

Apply the [shared localization rules](../../.agents/localization-and-themes.md).

- [content/Strings.kt](../app/src/main/kotlin/dev/jagoba/lostielauncher/content/Strings.kt)
  is the interface; `content/strings/<Lang>Strings.kt` supplies all eight objects.
  `stringsFor(language)` resolves the implementation.
- `content/Faqs.kt` resolves FAQ entries with `faqsFor(language)`. Kirin has
  an Android-specific entry used by the runtime requirement screen.
- Composables read `LocalStrings.current`; ViewModels derive text from current
  appearance/state rather than retaining a resolved string across language changes.
- Use `String.withArgs` from `content/WithArgs.kt` for `{0}` placeholders.
  Do not rename it to `format`: an omitted import could resolve to Kotlin's
  standard formatting behavior. `WithArgsResolutionTest` checks the call site
  outside the catalogue package.
- `AppLanguage.displayName` holds endonyms; `AppLanguage.code` holds wire codes
  for remote content. They are separate values.
- In-app language changes use Kotlin state, not resource qualifier switching.
  Platform-owned startup text remains in `res/values/strings.xml`.
- `StringsTest` checks required/removed keys and placeholders across languages;
  `FaqsTest` checks Android FAQ content. Update them for an intentional change
  instead of treating old fixed key counts as the contract.
- `SettingsStore` persists enum names and exposes appearance changes without
  recreating the activity. Preserve the first-settings-snapshot handling.
