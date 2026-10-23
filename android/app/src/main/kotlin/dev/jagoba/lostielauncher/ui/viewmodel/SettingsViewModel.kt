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
import dev.jagoba.lostielauncher.model.hasActiveDownload
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.GameInstallationService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.service.update.LauncherUpdateCheckResult
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.version.VersionUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: AppTheme,
    val language: AppLanguage,
    val strings: Strings,
    val hasSeenWelcome: Boolean,
    val version: String,
    val autoUpdate: Boolean,
    val autoUpdateSupported: Boolean,
    val isCheckingForUpdates: Boolean = false,
    val notice: SettingsNotice? = null,
)

enum class SettingsNotice { UPDATE_CHECK_BUSY, UP_TO_DATE, UPDATE_CHECK_FAILED }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val version: AppVersion,
    installation: GameInstallationService,
    private val downloads: DownloadManager,
    private val launcherUpdate: LauncherUpdateService,
    private val logger: Logger,
) : ViewModel() {
    private val mutableState = MutableStateFlow<SettingsUiState?>(null)
    val state: StateFlow<SettingsUiState?> = mutableState.asStateFlow()
    private val checking = MutableStateFlow(false)
    private val notice = MutableStateFlow<SettingsNotice?>(null)

    init {
        viewModelScope.launch {
            combine(
                store.settings,
                installation.installedGames,
                checking,
                notice,
            ) { settings, installed, checking, notice ->
                SettingsUiState(
                    theme = settings.theme,
                    language = settings.language,
                    strings = stringsFor(settings.language),
                    hasSeenWelcome = settings.hasSeenWelcome,
                    version = VersionUtils.formatDisplayVersion(version.name),
                    autoUpdate = settings.autoUpdate,
                    autoUpdateSupported = installed !is InstalledGamesState.NotSupportedYet,
                    isCheckingForUpdates = checking,
                    notice = notice,
                )
            }.collect { mutableState.value = it }
        }
    }

    fun selectTheme(theme: AppTheme) {
        logger.debug("Theme changed to: $theme.")
        viewModelScope.launch { store.setTheme(theme) }
    }

    fun selectLanguage(language: AppLanguage) {
        logger.debug("Language changed to: $language.")
        viewModelScope.launch { store.setLanguage(language) }
    }

    fun markWelcomeSeen() {
        viewModelScope.launch { store.setHasSeenWelcome(true) }
    }

    fun setAutoUpdate(enabled: Boolean) {
        logger.info("Auto update: ${if (enabled) "enabled" else "disabled"}.")
        viewModelScope.launch { store.setAutoUpdate(enabled) }
    }

    fun checkForUpdates() {
        if (checking.value) return
        checking.value = true
        logger.info("Manual update check initiated.")
        viewModelScope.launch {
            notice.value = if (downloads.downloads.first().hasActiveDownload) {
                logger.info("Manual update check blocked: a download is in progress.")
                SettingsNotice.UPDATE_CHECK_BUSY
            } else {
                when (launcherUpdate.check()) {
                    LauncherUpdateCheckResult.UpToDate -> SettingsNotice.UP_TO_DATE
                    LauncherUpdateCheckResult.Failed -> SettingsNotice.UPDATE_CHECK_FAILED
                    is LauncherUpdateCheckResult.Available -> null
                }
            }
            checking.value = false
        }
    }

    fun dismissNotice() {
        notice.value = null
    }
}
