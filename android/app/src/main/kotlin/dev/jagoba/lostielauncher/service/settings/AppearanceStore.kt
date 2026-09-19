package dev.jagoba.lostielauncher.service.settings

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the theme and the language.
 *
 * The desktop's `ISettingsService` is one interface over the whole settings
 * file; this is deliberately only the appearance half, because that is what
 * port plan step 05 delivers and a half-built settings service pretending to be
 * a whole one is worse than a narrow one that says what it is. **Step 07 owns
 * the rest** and decides then whether these two join the wider interface.
 *
 * [appearance] is a `Flow`, not a getter, and that is the mechanism behind the
 * requirement the desktop states as behaviour: picking a theme or a language
 * repaints and re-labels the running UI without restarting anything. A write
 * lands in storage and comes back out of this flow, so there is exactly one
 * source of truth and no in-memory copy to get out of step with the file.
 *
 * It never fails. A storage error is logged and read as the defaults — Spanish
 * and Volcarona — for the same reason the content service degrades rather than
 * propagating: a corrupt preferences file must not be able to stop the launcher
 * starting.
 */
interface AppearanceStore {
    /** The current theme and language, re-emitted on every change. */
    val appearance: Flow<Appearance>

    suspend fun setTheme(theme: AppTheme)

    suspend fun setLanguage(language: AppLanguage)
}
