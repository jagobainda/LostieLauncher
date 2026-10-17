package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.GameCardSkeleton
import dev.jagoba.lostielauncher.ui.component.InstalledGameCard
import dev.jagoba.lostielauncher.ui.component.InstalledGameCardState
import dev.jagoba.lostielauncher.ui.dialog.DialogButton
import dev.jagoba.lostielauncher.ui.dialog.DialogButtonStyle
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.GamesViewModel
import dev.jagoba.lostielauncher.ui.viewmodel.InstalledGameUiState
import dev.jagoba.lostielauncher.util.format.PlaytimeFormatter

private const val GAMES_SKELETON_COUNT = 5

internal class GamesActions(
    val play: (String) -> Unit,
    val update: (String) -> Unit,
    val openHelp: (String) -> Unit,
    val switchSpecialVersion: (String) -> Unit,
    val openFolder: (String) -> Unit,
    val uninstall: (String) -> Unit,
    val goToLibrary: () -> Unit,
)

@Composable
fun GamesScreen(modifier: Modifier = Modifier, viewModel: GamesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GamesContent(
        state = state,
        actions = GamesActions(
            play = viewModel::play,
            update = viewModel::update,
            openHelp = viewModel::openHelpLocation,
            switchSpecialVersion = viewModel::switchToSpecialVersion,
            openFolder = viewModel::openGameLocation,
            uninstall = viewModel::requestUninstall,
            goToLibrary = viewModel::navigateToLibrary,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun GamesContent(state: GamesUiState, actions: GamesActions, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    when {
        state.isListVisible -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(LauncherSpacing.Screen),
        ) {
            items(state.games, key = { it.game.name }) { row ->
                val name = row.game.name
                InstalledGameCard(
                    state = row.toCardState(state.runtimeSupported),
                    onPlay = { actions.play(name) },
                    onUpdate = { actions.update(name) },
                    onOpenHelp = { actions.openHelp(name) },
                    onSwitchSpecialVersion = { actions.switchSpecialVersion(name) },
                    onOpenFolder = { actions.openFolder(name) },
                    onUninstall = { actions.uninstall(name) },
                    modifier = Modifier.padding(bottom = LauncherSpacing.MediumLarge),
                )
            }
        }

        state.isEmpty -> EmptyState(
            icon = R.drawable.ic_games_gamepad,
            message = strings.gamesNoContent,
            modifier = modifier,
            action = { GoToLibraryButton(actions.goToLibrary) },
        )

        state.isInstalledStateUnsupported -> EmptyState(
            icon = R.drawable.ic_games_gamepad,
            message = strings.statusNotSupportedYet,
            detail = strings.notSupportedYetMessage,
            modifier = modifier,
            action = { GoToLibraryButton(actions.goToLibrary) },
        )

        else -> GameCardSkeletons(GAMES_SKELETON_COUNT, modifier)
    }
}

@Composable
private fun GoToLibraryButton(onClick: () -> Unit) {
    DialogButton(
        label = LocalStrings.current.gamesGoToLibrary,
        style = DialogButtonStyle.ACCENT,
        onClick = onClick,
        horizontalPadding = LauncherSpacing.Screen,
    )
}

@Composable
internal fun GameCardSkeletons(count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LauncherSpacing.Screen),
    ) {
        repeat(count) { GameCardSkeleton(Modifier.padding(bottom = LauncherSpacing.MediumLarge)) }
    }
}

internal fun InstalledGameUiState.toCardState(runtimeSupported: Boolean) = InstalledGameCardState(
    title = game.name,
    logoUrl = remote?.logoUrl,
    installedVersion = game.version,
    specialType = game.type,
    updateVersion = remote?.version?.takeIf { hasUpdate },
    playtime = PlaytimeFormatter.format(playtimeMinutes),
    showHelp = canOpenHelpLocation,
    canPlay = canPlay,
    canUpdate = canUpdate,
    canSwitchSpecialVersion = canSwitchSpecialVersion,
    canOpenFolder = canOpenGameLocation,
    canUninstall = canUninstall,
    isUninstalling = isUninstalling,
    runtimeSupported = runtimeSupported,
)
