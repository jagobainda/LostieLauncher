package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.model.NewsItem
import dev.jagoba.lostielauncher.model.NotificationItem
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.presentation.LauncherDataCoordinator
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val news: List<NewsItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val isContentStale: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && news.isEmpty() && notifications.isEmpty()
    val isListVisible: Boolean get() = !isLoading && !isEmpty
    val isNewsEmpty: Boolean get() = !isLoading && news.isEmpty()
    val isNotificationsEmpty: Boolean get() = !isLoading && notifications.isEmpty()
    val isOutOfDateWarningVisible: Boolean get() = isContentStale && !isOffline
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val coordinator: LauncherDataCoordinator,
    private val settings: SettingsStore,
    private val externalLinks: ExternalLinkService,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = coordinator.home.map { data ->
        HomeUiState(
            news = data.content.news,
            notifications = data.content.notifications,
            isLoading = data.isLoading,
            isOffline = data.isOffline,
            isContentStale = data.content.isStale,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    fun refresh() {
        viewModelScope.launch { coordinator.refreshHome(settings.settings.first().language) }
    }

    fun openLink(url: String) {
        externalLinks.openUrl(url)
    }
}
