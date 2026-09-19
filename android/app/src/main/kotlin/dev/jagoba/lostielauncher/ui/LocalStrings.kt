package dev.jagoba.lostielauncher.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.strings.EspStrings
import dev.jagoba.lostielauncher.model.AppLanguage

/**
 * The active text catalogue, readable anywhere in the tree as
 * `LocalStrings.current`.
 *
 * It is how a composable gets text without every screen threading a `Strings`
 * parameter down to every label, and it is the direct analogue of the desktop's
 * binding to `SettingsViewModel.Instance.Strings`. `AppearanceViewModel` is
 * what provides it; changing the language swaps the whole object and everything
 * reading through here recomposes with the new text.
 *
 * `staticCompositionLocalOf` rather than `compositionLocalOf`, for the same
 * reason as the palette: the value changes rarely, and when it does, every
 * label below has to be redrawn anyway.
 *
 * The default is Spanish so that a `@Preview` renders real copy instead of
 * throwing. Nothing in the running application relies on it.
 */
val LocalStrings: ProvidableCompositionLocal<Strings> = staticCompositionLocalOf { EspStrings }

/**
 * The selected language itself, for the two things that need more than the
 * catalogue: the FAQ list (`faqsFor`) and the date formatting that
 * `spec/10-windows-only.md` says must follow the launcher's language rather
 * than the device locale.
 */
val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> =
    staticCompositionLocalOf { AppLanguage.Default }
