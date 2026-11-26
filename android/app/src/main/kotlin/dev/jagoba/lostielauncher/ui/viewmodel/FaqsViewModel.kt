package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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
}

@HiltViewModel
class FaqsViewModel @Inject constructor(
    settings: SettingsStore,
    private val externalLinks: ExternalLinkService,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val search = savedState.getStateFlow(SEARCH_KEY, "")
    private val expansionLanguage = savedState.getStateFlow<String?>(EXPANSION_LANGUAGE_KEY, null)
    private val expandedIndices = savedState.getStateFlow(EXPANDED_KEY, intArrayOf())
    private val collapsedIndices = savedState.getStateFlow(COLLAPSED_KEY, intArrayOf())

    val state: StateFlow<FaqsUiState> = combine(
        search,
        settings.settings,
        expansionLanguage,
        expandedIndices,
        collapsedIndices,
    ) { term, stored, languageName, expanded, collapsed ->
        val query = term.trim()
        val matchesLanguage = languageName == stored.language.name
        val expandedSet = if (matchesLanguage) expanded.toSet() else emptySet()
        val collapsedSet = if (matchesLanguage) collapsed.toSet() else emptySet()
        val rows = faqsFor(stored.language).withIndex().asSequence()
            .filter { (_, entry) ->
                query.isEmpty() || SearchMatcher.contains(entry.question, query) ||
                    SearchMatcher.contains(entry.answer, query)
            }
            .map { (index, entry) ->
                val isExpanded = when (index) {
                    in expandedSet -> true
                    in collapsedSet -> false
                    else -> query.isNotEmpty()
                }
                FaqRow(index, entry, isExpanded)
            }
            .toList()
        FaqsUiState(term, stored.language, rows)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, FaqsUiState(searchText = search.value))

    fun setSearchText(text: String) {
        if (text == search.value) return
        savedState[EXPANSION_LANGUAGE_KEY] = null
        savedState[EXPANDED_KEY] = intArrayOf()
        savedState[COLLAPSED_KEY] = intArrayOf()
        savedState[SEARCH_KEY] = text
    }

    fun clearSearch() {
        setSearchText("")
    }

    fun toggleFaq(index: Int) {
        val current = state.value
        val row = current.rows.firstOrNull { it.index == index } ?: return
        val languageName = current.language?.name
        val sameLanguage = expansionLanguage.value == languageName
        val expanded = if (sameLanguage) expandedIndices.value.toMutableSet() else mutableSetOf()
        val collapsed = if (sameLanguage) collapsedIndices.value.toMutableSet() else mutableSetOf()
        if (row.isExpanded) {
            expanded -= index
            collapsed += index
        } else {
            collapsed -= index
            expanded += index
        }
        savedState[EXPANSION_LANGUAGE_KEY] = languageName
        savedState[EXPANDED_KEY] = expanded.toIntArray()
        savedState[COLLAPSED_KEY] = collapsed.toIntArray()
    }

    fun openLink(url: String) {
        externalLinks.openUrl(url)
    }

    private companion object {
        const val SEARCH_KEY = "faqs.search"
        const val EXPANSION_LANGUAGE_KEY = "faqs.expansion.language"
        const val EXPANDED_KEY = "faqs.expansion.expanded"
        const val COLLAPSED_KEY = "faqs.expansion.collapsed"
    }
}
