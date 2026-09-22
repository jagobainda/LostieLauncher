package dev.jagoba.lostielauncher.service.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppSettings
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import dev.jagoba.lostielauncher.model.SettingsOptions
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
internal class DataStoreSettingsStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
    dispatchers: DispatcherProvider,
    private val options: SettingsOptions,
) : SettingsStore {
    private val scope = CoroutineScope(dispatchers.io + SupervisorJob())
    private val gate = Mutex()
    private val pendingSettings = MutableStateFlow<AppSettings?>(null)
    private var flushJob: Job? = null

    private val storedSettings = dataStore.data
        .catch { cause ->
            if (cause is CancellationException) throw cause
            logger.error("Could not read the settings; using the defaults", cause)
            emit(emptyPreferences())
        }
        .map(::toSettings)

    override val settings: Flow<AppSettings> = combine(storedSettings, pendingSettings) { stored, pending ->
        pending ?: stored
    }.distinctUntilChanged()

    override val appearance: Flow<Appearance> = settings
        .map { it.appearance }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }

    override suspend fun setLanguage(language: AppLanguage) = update { it.copy(language = language) }

    override suspend fun setHasSeenWelcome(hasSeenWelcome: Boolean) = update {
        it.copy(hasSeenWelcome = hasSeenWelcome)
    }

    override suspend fun flush() {
        val scheduled = gate.withLock {
            flushJob?.cancel()
            flushJob.also { flushJob = null }
        }
        scheduled?.join()
        persistPending()
    }

    private suspend fun update(transform: (AppSettings) -> AppSettings) {
        gate.withLock {
            val current = pendingSettings.value ?: storedSettings.first()
            pendingSettings.value = transform(current)
            flushJob?.cancel()
            flushJob = scope.launch {
                delay(options.saveDebounce.inWholeMilliseconds)
                persistPending()
            }
        }
    }

    private suspend fun persistPending() {
        val snapshot = gate.withLock { pendingSettings.value } ?: return
        try {
            dataStore.edit { preferences ->
                preferences[ThemeKey] = snapshot.theme.name
                preferences[LanguageKey] = snapshot.language.name
                preferences[HasSeenWelcomeKey] = snapshot.hasSeenWelcome
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: IOException) {
            logger.error("Could not persist the settings", cause)
        }
        gate.withLock {
            if (pendingSettings.value == snapshot) pendingSettings.value = null
        }
    }

    private fun toSettings(preferences: Preferences) = AppSettings(
        theme = AppTheme.fromNameOrDefault(preferences[ThemeKey]),
        language = AppLanguage.fromNameOrDefault(preferences[LanguageKey]),
        hasSeenWelcome = preferences[HasSeenWelcomeKey] ?: false,
    )

    private companion object {
        val ThemeKey = stringPreferencesKey("appearance.theme")
        val LanguageKey = stringPreferencesKey("appearance.language")
        val HasSeenWelcomeKey = booleanPreferencesKey("onboarding.hasSeenWelcome")
    }
}
