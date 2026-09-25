package dev.jagoba.lostielauncher.service.settings

import dev.jagoba.lostielauncher.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsStore : AppearanceStore {
    val settings: Flow<AppSettings>

    suspend fun setHasSeenWelcome(hasSeenWelcome: Boolean)

    suspend fun setAutoUpdate(autoUpdate: Boolean)

    suspend fun flush()
}
