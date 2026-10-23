# Desktop themes

Apply the [shared theme rules](../../.agents/localization-and-themes.md).

- [Themes/](../LostieLauncher/Themes/) holds one resource dictionary per theme.
  Keep identical color and brush key sets in all dictionaries.
- Add or rename a role across every theme. A missing dictionary resource is a
  runtime failure; compilation alone does not prove completeness.
- Reference colors and brushes by resource key in XAML, including reusable
  styles and components.
- A new theme needs an `AppTheme` member, a complete dictionary, and an update
  to the product's theme list.
- `SettingsViewModel.ApplyTheme` resolves
  `pack://application:,,,/Themes/{theme}.xaml`. Keep enum names and filenames
  synchronized.
