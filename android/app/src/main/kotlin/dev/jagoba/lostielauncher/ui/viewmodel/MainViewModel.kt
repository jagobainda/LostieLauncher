package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.DownloadStatus
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.service.download.DownloadManager
import dev.jagoba.lostielauncher.service.link.ExternalLinkResult
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.presentation.NavigationStore
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val section: LauncherSection = LauncherSection.HOME,
    val title: String = "",
    val isOffline: Boolean = false,
    val isRefreshing: Boolean = false,
    val canRefresh: Boolean = false,
    val pendingLibraryGameId: String? = null,
    val externalLinkNotice: ExternalLinkNotice? = null,
    val isWelcomeVisible: Boolean = false,
) {
    val contextLinks: List<ExternalLink> get() = contextLinksFor(section)
    val canNavigateBack: Boolean get() = section != LauncherSection.HOME
}

private fun contextLinksFor(section: LauncherSection): List<ExternalLink> = when (section) {
    LauncherSection.HOME -> listOf(ExternalLink.TWITCH, ExternalLink.YOUTUBE, ExternalLink.TWITTER)
    LauncherSection.SETTINGS -> listOf(ExternalLink.GITHUB)
    LauncherSection.GAMES, LauncherSection.LIBRARY, LauncherSection.FAQS -> emptyList()
}

data class ExternalLinkNotice(val link: ExternalLink, val result: ExternalLinkResult)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navigation: NavigationStore,
    private val coordinator: LauncherDataCoordinator,
    private val settings: SettingsStore,
    downloads: DownloadManager,
    private val externalLinks: ExternalLinkService,
) : ViewModel() {
    private val linkNotice = MutableStateFlow<ExternalLinkNotice?>(null)
    private val welcome = MutableStateFlow(false)
    private val shell = combine(
        navigation.state,
        settings.settings,
        coordinator.home,
        coordinator.catalogue,
        coordinator.isGamesLoading,
    ) { navigation, settings, home, catalogue, gamesLoading ->
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
            canRefresh = !home.isLoading && !catalogue.isLoading && !gamesLoading,
            pendingLibraryGameId = navigation.pendingLibraryGameId,
        )
    }

    val state: StateFlow<MainUiState> = combine(
        shell,
        coordinator.isRefreshing,
        downloads.downloads,
        linkNotice,
        welcome,
    ) { shell, refreshing, rows, notice, welcomeVisible ->
        val downloading = rows.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.DOWNLOADING }
        shell.copy(
            isRefreshing = refreshing,
            canRefresh = shell.canRefresh && !refreshing && !downloading,
            externalLinkNotice = notice,
            isWelcomeVisible = welcomeVisible,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            if (settings.settings.first().hasSeenWelcome) return@launch
            navigation.navigate(LauncherSection.LIBRARY)
            settings.setHasSeenWelcome(true)
            welcome.value = true
        }
    }

    fun dismissWelcome() {
        welcome.value = false
    }

    fun navigate(section: LauncherSection) {
        navigation.navigate(section)
    }

    fun navigateBack() {
        if (navigation.state.value.section != LauncherSection.HOME) navigation.navigate(LauncherSection.HOME)
    }

    fun consumeLibraryGame(gameId: String) {
        navigation.consumeLibraryGame(gameId)
    }

    fun refresh() {
        if (!state.value.canRefresh) return
        viewModelScope.launch { coordinator.refreshAll(settings.settings.first().language) }
    }

    fun openExternalLink(link: ExternalLink) {
        val result = externalLinks.open(link)
        linkNotice.value = if (result == ExternalLinkResult.OPENED) null else ExternalLinkNotice(link, result)
    }

    fun clearExternalLinkNotice() {
        linkNotice.value = null
    }
}
