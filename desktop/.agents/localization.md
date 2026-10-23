# Desktop localization

Apply the [shared localization rules](../../.agents/localization-and-themes.md).

- [Content/Strings.cs](../LostieLauncher/Content/Strings.cs) defines `IStrings`
  and the implementations `Esp`, `Eng`, `Cat`, `Eus`, `Gal`, `Por`, `Val`,
  `Fra`. Add each new member to all implementations.
- [Content/Faqs.cs](../LostieLauncher/Content/Faqs.cs) does the same for
  `IFaqs`; `Faqs.For(language)` resolves the catalogue.
- Views and ViewModels obtain text through `SettingsViewModel.Strings`.
  React to changes to `Strings` or `Language`, following existing subscriptions.
- Substitute placeholders with `string.Format(strings.SomeMessage, arg)`.
- A new language requires an `AppLanguage` member with its display-name
  `Description` attribute and complete string and FAQ implementations.
- The existing Windows-only `Saved Games` tooltip and startup fatal-error
  fallback are legacy exceptions; do not copy them into new UI text.
