package dev.jagoba.lostielauncher.service.game

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

internal interface KirinMinimumVersionStore {
    suspend fun read(): String?

    suspend fun write(version: String)
}

@Singleton
internal class DataStoreKirinMinimumVersionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
) : KirinMinimumVersionStore {
    override suspend fun read(): String? = try {
        dataStore.data.first()[MinimumVersionKey]
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: IOException) {
        logger.error("Could not read the cached Kirin minimum version.", error)
        null
    }

    override suspend fun write(version: String) {
        try {
            dataStore.edit { preferences ->
                preferences[MinimumVersionKey] = version
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            logger.error("Could not persist the Kirin minimum version.", error)
        }
    }

    private companion object {
        val MinimumVersionKey = stringPreferencesKey("kirin.minimumVersion")
    }
}
