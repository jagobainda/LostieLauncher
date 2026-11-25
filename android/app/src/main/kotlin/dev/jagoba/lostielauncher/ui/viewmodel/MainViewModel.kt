package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.model.hasActiveDownload
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KirinUpdateUiState(
    val installedVersion: String,
    val latestVersion: String,
    val downloadPercent: Int? = null,
    val isInstalling: Boolean = false,
)

data class MainUiState(
    val section: LauncherSection = LauncherSection.HOME,
    val title: String = "",
    val isOffline: Boolean = false,
    val isTwitchLive: Boolean = false,
    val isRefreshing: Boolean = false,
    val canRefresh: Boolean = false,
    val pendingLibraryGameId: String? = null,
    val isWelcomeVisible: Boolean = false,
    val kirinUpdate: KirinUpdateUiState? = null,
    val launcherUpdatePrompt: String? = null,
    val launcherUpdatePermissionRequired: Boolean = false,
    val launcherUpdateCancelled: Boolean = false,
    val launcherUpdatePercent: Int? = null,
    val launcherUpdateFailed: Boolean = false,
) {
    val contextLinks: List<ExternalLink> get() = contextLinksFor(section)
    val canNavigateBack: Boolean get() = section != LauncherSection.HOME
}

private data class UpdatesState(
    val kirin: KirinUpdateUiState?,
    val launcherOffer: String?,
    val launcherDownload: ApkDownloadState,
)

private fun contextLinksFor(section: LauncherSection): List<ExternalLink> = when (section) {
    LauncherSection.HOME -> listOf(ExternalLink.TWITCH, ExternalLink.YOUTUBE, ExternalLink.TWITTER)
    LauncherSection.SETTINGS -> listOf(ExternalLink.GITHUB)
    LauncherSection.GAMES, LauncherSection.LIBRARY, LauncherSection.FAQS -> emptyList()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navigation: NavigationStore,
    private val coordinator: LauncherDataCoordinator,
    private val settings: SettingsStore,
    downloads: DownloadManager,
    private val externalLinks: ExternalLinkService,
    private val kirin: KirinSetupService,
    private val launcherUpdate: LauncherUpdateService,
    private val logger: Logger,
) : ViewModel() {
    private val welcome = MutableStateFlow(false)
    private val kirinUpdate = combine(kirin.status, kirin.download) { status, download ->
        val offered = download == ApkDownloadState.Available ||
            download == ApkDownloadState.PermissionRequired ||
            download is ApkDownloadState.Downloading ||
            download == ApkDownloadState.Installing ||
            download == ApkDownloadState.Cancelled ||
            download == ApkDownloadState.Failed
        (status as? KirinStatus.UpdateRecommended)?.takeIf { offered }?.let {
            KirinUpdateUiState(
                installedVersion = it.installedVersion,
                latestVersion = it.latestVersion,
                downloadPercent = (download as? ApkDownloadState.Downloading)?.percent,
                isInstalling = download == ApkDownloadState.Installing,
            )
        }
    }
    private val updates = combine(kirinUpdate, launcherUpdate.offeredVersion, launcherUpdate.download, ::UpdatesState)
    private val shell = combine(
        navigation.state,
        settings.settings,
        coordinator.home,
        coordinator.catalogue,
    ) { navigation, settings, home, catalogue ->
        val strings = stringsFor(settings.language)
        MainUiState(
            section = navigation.section,
            title = when (navigation.section) {
                LauncherSection.HOME -> strings.titleHome
                LauncherSection.GAMES -> strings.titleGames
                LauncherSection.LIBRARY -> strings.titleLibrary
                LauncherSection.FAQS -> strings.titleFaqs
                LauncherSection.SETTINGS -> strings.titleSettings
            },
            isOffline = home.isOffline,
            isTwitchLive = home.isTwitchLive,
            canRefresh = !home.isLoading && !catalogue.isLoading,
            pendingLibraryGameId = navigation.pendingLibraryGameId,
        )
    }

    val state: StateFlow<MainUiState> = combine(
        shell,
        coordinator.isRefreshing,
        downloads.downloads,
        welcome,
        updates,
    ) { shell, refreshing, rows, welcomeVisible, updates ->
        val downloading = rows.hasActiveDownload
        val launcherDownload = updates.launcherDownload
        shell.copy(
            isRefreshing = refreshing,
            canRefresh = shell.canRefresh && !refreshing && !downloading,
            isWelcomeVisible = welcomeVisible,
            kirinUpdate = updates.kirin.takeIf { shell.section == LauncherSection.GAMES },
            launcherUpdatePrompt = updates.launcherOffer.takeIf {
                launcherDownload == ApkDownloadState.Available && !downloading && !welcomeVisible
            },
            launcherUpdatePermissionRequired = updates.launcherOffer != null &&
                launcherDownload == ApkDownloadState.PermissionRequired &&
                !welcomeVisible,
            launcherUpdateCancelled = updates.launcherOffer != null &&
                launcherDownload == ApkDownloadState.Cancelled &&
                !downloading &&
                !welcomeVisible,
            launcherUpdatePercent = (launcherDownload as? ApkDownloadState.Downloading)?.percent,
            launcherUpdateFailed = launcherDownload == ApkDownloadState.Failed,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            if (settings.settings.first().hasSeenWelcome) return@launch
            logger.info("First launch detected - navigating to library and showing welcome dialog.")
            navigate(LauncherSection.LIBRARY)
            welcome.value = true
        }
    }

    fun dismissWelcome() {
        if (!welcome.value) return
        welcome.value = false
        viewModelScope.launch { settings.setHasSeenWelcome(true) }
    }

    fun navigate(section: LauncherSection) {
        logger.debug("Navigating to $section.")
        navigation.navigate(section)
    }

    fun navigateBack() {
        if (navigation.state.value.section != LauncherSection.HOME) navigate(LauncherSection.HOME)
    }

    fun consumeLibraryGame(gameId: String) {
        navigation.consumeLibraryGame(gameId)
    }

    fun refresh() {
        if (!state.value.canRefresh) return
        logger.debug("Data refresh started.")
        viewModelScope.launch {
            coordinator.refreshAll(settings.settings.first().language)
            logger.debug("Data refresh completed.")
        }
    }

    fun updateKirin() {
        logger.info("Kirin update requested from My Games.")
        viewModelScope.launch { kirin.install() }
    }

    fun acceptLauncherUpdate() {
        viewModelScope.launch { launcherUpdate.install() }
    }

    fun onLauncherInstallPermissionResult() {
        viewModelScope.launch { launcherUpdate.onInstallPermissionResult() }
    }

    fun dismissLauncherInstallPermission() {
        launcherUpdate.dismissPermissionRequest()
    }

    fun declineLauncherUpdate() {
        launcherUpdate.decline()
    }

    fun dismissLauncherUpdateFailure() {
        launcherUpdate.acknowledgeFailure()
    }

    fun openExternalLink(link: ExternalLink) {
        externalLinks.open(link)
    }
}
