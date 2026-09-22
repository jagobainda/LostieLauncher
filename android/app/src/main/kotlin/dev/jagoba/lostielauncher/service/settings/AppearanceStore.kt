package dev.jagoba.lostielauncher.service.settings

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.Appearance
import kotlinx.coroutines.flow.Flow

interface AppearanceStore {
    val appearance: Flow<Appearance>

    suspend fun setTheme(theme: AppTheme)

    suspend fun setLanguage(language: AppLanguage)
}
