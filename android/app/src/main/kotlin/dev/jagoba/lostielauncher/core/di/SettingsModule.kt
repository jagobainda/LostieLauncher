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
import dev.jagoba.lostielauncher.service.settings.AppearanceStore
import dev.jagoba.lostielauncher.service.settings.DataStoreAppearanceStore
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Where the launcher's settings are stored.
 *
 * The desktop keeps one JSON file under `%APPDATA%`; this keeps a Preferences
 * DataStore in the application's own data directory, which is the same idea
 * with the platform's answer for where. The file name is here rather than in
 * the store for the usual reason: no path inside the type that does the work.
 *
 * **Port plan step 07 owns settings** and will add the rest of them. It should
 * extend this DataStore rather than introduce a second mechanism beside it.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsModule {
    private const val SETTINGS_FILE = "settings"

    /**
     * The scope DataStore does its file work on.
     *
     * `SupervisorJob` so one failed write cannot cancel the scope and take
     * every later write with it, and the dispatcher comes from the injected
     * [DispatcherProvider] because that is the only place in the application
     * allowed to name one.
     */
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // A file that cannot be parsed is replaced with an empty one, so the
        // user gets the default theme and language instead of a launcher that
        // will not start. Same choice as the content layer's: degrade, do not
        // propagate.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(dispatchers.io + SupervisorJob()),
        produceFile = { context.preferencesDataStoreFile(SETTINGS_FILE) },
    )
}

/**
 * Interface-to-implementation bindings for the settings area.
 *
 * Separate from [SettingsModule] because Hilt will not take `@Binds` and
 * `@Provides` in the same object — the first needs an abstract class.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SettingsBindingsModule {
    @Binds
    @Singleton
    abstract fun bindAppearanceStore(impl: DataStoreAppearanceStore): AppearanceStore
}
