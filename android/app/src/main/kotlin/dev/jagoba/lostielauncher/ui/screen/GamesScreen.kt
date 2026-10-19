package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.GameCardSkeleton
import dev.jagoba.lostielauncher.ui.component.InstalledCardActionsReserve
import dev.jagoba.lostielauncher.ui.component.InstalledGameCard
import dev.jagoba.lostielauncher.ui.component.InstalledGameCardState
import dev.jagoba.lostielauncher.ui.dialog.DialogButton
import dev.jagoba.lostielauncher.ui.dialog.DialogButtonStyle
import dev.jagoba.lostielauncher.ui.theme.LauncherMotion
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.GamesViewModel
import dev.jagoba.lostielauncher.ui.viewmodel.InstalledGameUiState
import kotlinx.coroutines.delay

private const val GAMES_SKELETON_COUNT = 5

private val UninstalledCardFade = tween<Float>(
    LauncherMotion.TakeoverFade.inWholeMilliseconds.toInt(),
    delayMillis = LauncherMotion.EjectLoop.inWholeMilliseconds.toInt(),
)

internal class GamesActions(
    val play: (String) -> Unit,
    val update: (String) -> Unit,
    val switchSpecialVersion: (String) -> Unit,
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
            switchSpecialVersion = viewModel::switchToSpecialVersion,
            uninstall = viewModel::requestUninstall,
            goToLibrary = viewModel::navigateToLibrary,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun GamesContent(state: GamesUiState, actions: GamesActions, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val games = rememberGamesWithDepartures(state.games)
    val present = remember(state.games) { state.games.mapTo(HashSet()) { it.game.name } }
    when {
        state.isListVisible || games.size > state.games.size -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(LauncherSpacing.Screen),
        ) {
            items(games, key = { it.game.name }) { row ->
                val name = row.game.name
                val alpha by animateFloatAsState(
                    if (name in present) 1f else 0f,
                    UninstalledCardFade,
                    label = "uninstalled-card",
                )
                InstalledGameCard(
                    state = row.toCardState(),
                    onPlay = { actions.play(name) },
                    onUpdate = { actions.update(name) },
                    onSwitchSpecialVersion = { actions.switchSpecialVersion(name) },
                    onUninstall = { actions.uninstall(name) },
                    modifier = Modifier
                        .animateItem()
                        .graphicsLayer { this.alpha = alpha }
                        .padding(bottom = LauncherSpacing.MediumLarge),
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

        else -> GameCardSkeletons(GAMES_SKELETON_COUNT, InstalledCardActionsReserve, modifier)
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
private fun rememberGamesWithDepartures(games: List<InstalledGameUiState>): List<InstalledGameUiState> {
    var shown by remember { mutableStateOf(games) }
    LaunchedEffect(games) {
        shown = withDepartingGames(shown, games)
        if (shown.size > games.size) {
            delay(LauncherMotion.EjectLoop + LauncherMotion.TakeoverFade)
            shown = games
        }
    }
    return shown
}

internal fun withDepartingGames(
    shown: List<InstalledGameUiState>,
    games: List<InstalledGameUiState>,
): List<InstalledGameUiState> {
    val names = games.mapTo(HashSet()) { it.game.name }
    val result = games.toMutableList()
    shown.forEachIndexed { index, row ->
        if (row.isUninstalling && row.game.name !in names) result.add(minOf(index, result.size), row)
    }
    return result
}

@Composable
internal fun GameCardSkeletons(count: Int, actionsReserve: Dp, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LauncherSpacing.Screen),
    ) {
        repeat(count) { GameCardSkeleton(actionsReserve, Modifier.padding(bottom = LauncherSpacing.MediumLarge)) }
    }
}

internal fun InstalledGameUiState.toCardState() = InstalledGameCardState(
    title = game.name,
    logoUrl = remote?.logoUrl,
    installedVersion = game.version,
    specialType = game.type,
    updateVersion = remote?.version?.takeIf { hasUpdate },
    canPlay = canPlay,
    canUpdate = canUpdate,
    canSwitchSpecialVersion = canSwitchSpecialVersion,
    canUninstall = canUninstall,
    isUninstalling = isUninstalling,
)
