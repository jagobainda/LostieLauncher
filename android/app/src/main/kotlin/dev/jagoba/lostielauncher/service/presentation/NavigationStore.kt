package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.LauncherSection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NavigationState(
    val section: LauncherSection = LauncherSection.HOME,
    val pendingLibraryGameId: String? = null,
    val libraryAction: LibraryNavigationAction? = null,
)

enum class LibraryNavigationAction {
    DOWNLOAD,
    UPDATE,
    SPECIAL_VERSION,
}

@Singleton
class NavigationStore @Inject constructor() {
    private val mutableState = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = mutableState.asStateFlow()

    fun navigate(section: LauncherSection, gameId: String? = null, action: LibraryNavigationAction? = null) {
        mutableState.value = NavigationState(section, if (section == LauncherSection.LIBRARY) gameId else null, action)
    }

    fun consumeLibraryGame(gameId: String) {
        mutableState.update { current ->
            if (current.pendingLibraryGameId ==
                gameId
            ) {
                current.copy(pendingLibraryGameId = null, libraryAction = null)
            } else {
                current
            }
        }
    }

    fun consumeLibraryAction(gameId: String) {
        mutableState.update { current ->
            if (current.pendingLibraryGameId == gameId) current.copy(libraryAction = null) else current
        }
    }
}
