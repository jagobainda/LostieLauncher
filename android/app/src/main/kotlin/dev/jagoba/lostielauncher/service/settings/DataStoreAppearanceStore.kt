package dev.jagoba.lostielauncher.service.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [AppearanceStore] on Preferences DataStore.
 *
 * Two key-value pairs, holding the **enum member name**. The desktop serializes
 * `AppSettings` to JSON and writes the enum's ordinal; this side stores the name
 * because the storage is key-value rather than a serialized object anyway, and
 * because a name survives a member being inserted into the middle of the enum,
 * which is exactly the migration the desktop's ordinals could not take. Reading
 * a name nothing matches yields the default, which is the desktop's own
 * fallback behaviour — `spec/04-data-model.md` states both of them.
 *
 * The `catch` on the read is not decoration: DataStore surfaces a corrupt or
 * unreadable file as an [IOException] in the flow, and the launcher's rule is
 * to log and degrade rather than propagate. Anything that is not an
 * [IOException] is a programming error and is left to fail.
 */
internal class DataStoreAppearanceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
) : AppearanceStore {
    override val appearance: Flow<Appearance> = dataStore.data
        .catch { cause ->
            if (cause !is IOException) throw cause
            logger.error("Could not read the appearance settings; using the defaults", cause)
            emit(emptyPreferences())
        }
        .map { preferences ->
            Appearance(
                theme = AppTheme.fromNameOrDefault(preferences[ThemeKey]),
                language = AppLanguage.fromNameOrDefault(preferences[LanguageKey]),
            )
        }

    override suspend fun setTheme(theme: AppTheme) = write(ThemeKey, theme.name, "theme")

    override suspend fun setLanguage(language: AppLanguage) = write(LanguageKey, language.name, "language")

    private suspend fun write(key: Preferences.Key<String>, value: String, what: String) {
        try {
            dataStore.edit { it[key] = value }
        } catch (cause: IOException) {
            // A failed write emits nothing, so the UI keeps the previous
            // selection rather than showing one that was not stored. Logged and
            // swallowed: the launcher does not stop working because a
            // preferences file could not be written.
            logger.error("Could not persist the $what selection", cause)
        }
    }

    private companion object {
        val ThemeKey = stringPreferencesKey("appearance.theme")
        val LanguageKey = stringPreferencesKey("appearance.language")
    }
}
