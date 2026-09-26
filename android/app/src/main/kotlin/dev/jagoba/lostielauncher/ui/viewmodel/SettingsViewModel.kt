package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.AppVersion
import dev.jagoba.lostielauncher.model.InstalledGamesState
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.version.VersionUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: AppTheme,
    val language: AppLanguage,
    val strings: Strings,
    val hasSeenWelcome: Boolean,
    val version: String,
    val autoUpdate: Boolean,
    val autoUpdateSupported: Boolean,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val version: AppVersion,
    installation: GameInstallationService,
) : ViewModel() {
    private val mutableState = MutableStateFlow<SettingsUiState?>(null)
    val state: StateFlow<SettingsUiState?> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(store.settings, installation.installedGames) { settings, installed ->
                SettingsUiState(
                    theme = settings.theme,
                    language = settings.language,
                    strings = stringsFor(settings.language),
                    hasSeenWelcome = settings.hasSeenWelcome,
                    version = VersionUtils.formatDisplayVersion(version.name),
                    autoUpdate = settings.autoUpdate,
                    autoUpdateSupported = installed !is InstalledGamesState.NotSupportedYet,
                )
            }.collect { mutableState.value = it }
        }
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch { store.setTheme(theme) }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch { store.setLanguage(language) }
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { store.setHasSeenWelcome(true) }
    }

    fun setAutoUpdate(enabled: Boolean) {
        viewModelScope.launch { store.setAutoUpdate(enabled) }
    }
}
