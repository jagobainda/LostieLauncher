package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.FaqEntry
import dev.jagoba.lostielauncher.content.kirinFaqFor
import dev.jagoba.lostielauncher.model.ApkDownloadState
import dev.jagoba.lostielauncher.model.KirinStatus
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KirinUiState(
    val status: KirinStatus? = null,
    val download: ApkDownloadState = ApkDownloadState.Checking,
    val faq: FaqEntry? = null,
) {
    val isBlocking: Boolean get() = status is KirinStatus.Missing || status is KirinStatus.Outdated
}

@HiltViewModel
class KirinViewModel @Inject constructor(
    private val kirin: KirinSetupService,
    settings: SettingsStore,
    private val externalLinks: ExternalLinkService,
) : ViewModel() {
    val state: StateFlow<KirinUiState> = combine(kirin.status, kirin.download, settings.settings) {
            status,
            download,
            stored,
        ->
        KirinUiState(status, download, kirinFaqFor(stored.language))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, KirinUiState())

    fun install() {
        viewModelScope.launch { kirin.install() }
    }

    fun dismissDownloadFailure() {
        kirin.acknowledgeFailure()
    }

    fun openLink(url: String) {
        externalLinks.openUrl(url)
    }
}
