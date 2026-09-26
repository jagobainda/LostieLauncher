package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.FaqEntry
import dev.jagoba.lostielauncher.content.faqsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.service.link.ExternalLinkService
import dev.jagoba.lostielauncher.service.settings.SettingsStore
import dev.jagoba.lostielauncher.util.text.SearchMatcher
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FaqRow(val index: Int, val entry: FaqEntry, val isExpanded: Boolean)

data class FaqsUiState(
    val searchText: String = "",
    val language: AppLanguage? = null,
    val rows: List<FaqRow> = emptyList(),
) {
    val hasNoResults: Boolean get() = rows.isEmpty()
    val canClearSearch: Boolean get() = searchText.isNotEmpty()
}

@HiltViewModel
class FaqsViewModel @Inject constructor(settings: SettingsStore, private val externalLinks: ExternalLinkService) :
    ViewModel() {
    private val search = MutableStateFlow("")
    private val expansion = MutableStateFlow<Pair<AppLanguage?, Map<Int, Boolean>>>(null to emptyMap())
    val state: StateFlow<FaqsUiState> = combine(search, settings.settings, expansion) { term, stored, expanded ->
        val query = term.trim()
        val overrides = expanded.second.takeIf { expanded.first == stored.language }.orEmpty()
        val rows = faqsFor(stored.language).withIndex().asSequence()
            .filter { (_, entry) ->
                query.isEmpty() || SearchMatcher.contains(entry.question, query) ||
                    SearchMatcher.contains(entry.answer, query)
            }
            .map { (index, entry) -> FaqRow(index, entry, overrides[index] ?: query.isNotEmpty()) }
            .toList()
        FaqsUiState(term, stored.language, rows)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FaqsUiState())

    fun setSearchText(text: String) {
        expansion.value = null to emptyMap()
        search.value = text
    }

    fun clearSearch() {
        setSearchText("")
    }

    fun toggleFaq(index: Int) {
        val row = state.value.rows.firstOrNull { it.index == index } ?: return
        expansion.value = state.value.language to
            (expansion.value.second + (index to !row.isExpanded))
    }

    fun openLink(url: String) {
        externalLinks.openUrl(url)
    }
}
