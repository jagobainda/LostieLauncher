package dev.jagoba.lostielauncher.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.SettingsOptions
import dev.jagoba.lostielauncher.service.settings.AppearanceStore
import dev.jagoba.lostielauncher.service.settings.DataStoreSettingsStore
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsModule {
    private const val SETTINGS_FILE = "settings"
    private const val SAVE_DEBOUNCE_MILLISECONDS = 500L
    private const val STARTUP_LOAD_TIMEOUT_MILLISECONDS = 2_000L

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(dispatchers.io + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) },
    )

    @Provides
    @Singleton
    internal fun provideSettingsOptions(): SettingsOptions = SettingsOptions(
        saveDebounce = SAVE_DEBOUNCE_MILLISECONDS.milliseconds,
        startupLoadTimeout = STARTUP_LOAD_TIMEOUT_MILLISECONDS.milliseconds,
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindSettingsStore(impl: DataStoreSettingsStore): SettingsStore

    @Binds
    @Singleton
    internal abstract fun bindAppearanceStore(impl: DataStoreSettingsStore): AppearanceStore
}
