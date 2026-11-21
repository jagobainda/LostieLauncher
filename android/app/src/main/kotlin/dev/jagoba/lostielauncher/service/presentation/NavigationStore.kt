package dev.jagoba.lostielauncher.service.presentation

import dev.jagoba.lostielauncher.model.LauncherSection
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PendingLibraryAction(val gameId: String, val action: LibraryNavigationAction)

data class NavigationState(
    val section: LauncherSection = LauncherSection.HOME,
    val pendingLibraryGameId: String? = null,
    val pendingLibraryAction: PendingLibraryAction? = null,
)

enum class LibraryNavigationAction {
    UPDATE,
    SPECIAL_VERSION,
}

@Singleton
class NavigationStore @Inject constructor() {
    private val mutableState = MutableStateFlow(NavigationState())
    val state: StateFlow<NavigationState> = mutableState.asStateFlow()

    fun navigate(section: LauncherSection, gameId: String? = null, action: LibraryNavigationAction? = null) {
        val target = gameId.takeIf { section == LauncherSection.LIBRARY }
        mutableState.value = NavigationState(
            section = section,
            pendingLibraryGameId = target,
            pendingLibraryAction = if (target != null && action != null) PendingLibraryAction(target, action) else null,
        )
    }

    fun consumeLibraryGame(gameId: String) {
        mutableState.update { current ->
            if (current.pendingLibraryGameId == gameId) current.copy(pendingLibraryGameId = null) else current
        }
    }

    fun consumeLibraryAction(gameId: String): Boolean {
        var consumed = false
        mutableState.update { current ->
            if (current.pendingLibraryAction?.gameId == gameId) {
                consumed = true
                current.copy(pendingLibraryAction = null)
            } else {
                consumed = false
                current
            }
        }
        return consumed
    }
}
