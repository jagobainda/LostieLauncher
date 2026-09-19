package dev.jagoba.lostielauncher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.stringsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.service.settings.AppearanceStore
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the shell needs in order to draw anything: which palette to use and
 * which catalogue to read text from.
 *
 * `null` until the stored settings have been read once. The distinction matters
 * on the first frame after a cold start: the defaults are Volcarona and
 * Spanish, so rendering them while the real values are still being read would
 * show every user who chose otherwise a visible flash of the wrong theme.
 */
data class AppearanceUiState(val theme: AppTheme, val language: AppLanguage, val strings: Strings)

/**
 * Owns the theme and the language for the whole application.
 *
 * This is the part of the desktop's `SettingsViewModel` that everything else
 * observes. The desktop reaches it through a static `Instance` because XAML
 * cannot inject; here it is a normal injected ViewModel held at the root of the
 * composition, and `spec/01-overview.md` is explicit that the static must not
 * be reproduced.
 *
 * It resolves the catalogue rather than exposing only the enum, because that is
 * what makes a language change a single reference swap: every `Text` reads
 * `strings.something` through the state it is already collecting, and nothing
 * anywhere caches a resolved string in a field. That rule is what the hot
 * switch rests on — `spec/05-localization.md` says so for the desktop and it
 * holds identically here.
 *
 * Nothing is kept in memory beside the store. A selection is written, comes
 * back out of [AppearanceStore.appearance], and only then reaches the screen,
 * so there is no second copy that can disagree with the file.
 *
 * **Port plan step 10 owns the ViewModels** and ports the desktop's set with
 * their tests. It inherits this one: either as it stands, or folded into the
 * `SettingsViewModel` it writes. What it may not do is give the launcher a
 * second, separate idea of what the current theme is.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(private val store: AppearanceStore) : ViewModel() {
    private val _state = MutableStateFlow<AppearanceUiState?>(null)

    /** `null` until the stored settings have been read for the first time. */
    val state: StateFlow<AppearanceUiState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.appearance.collect { appearance ->
                _state.value = AppearanceUiState(
                    theme = appearance.theme,
                    language = appearance.language,
                    strings = stringsFor(appearance.language),
                )
            }
        }
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch { store.setTheme(theme) }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch { store.setLanguage(language) }
    }
}
