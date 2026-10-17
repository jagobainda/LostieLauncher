package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.dialog.LauncherMessageBox
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxButtons
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxIcon
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.viewmodel.GamesNotice
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.GamesViewModel

internal enum class GamesConfirmation { ACKNOWLEDGE, UNINSTALL, OPEN_BLOCKING_LOCATION, DOWNLOAD_MISSING }

internal data class GamesMessage(val spec: MessageBoxSpec, val confirmation: GamesConfirmation)

@Composable
fun GamesDialogs(viewModel: GamesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val message = gamesMessage(state, LocalStrings.current) ?: return
    LauncherMessageBox(
        spec = message.spec,
        onConfirm = {
            when (message.confirmation) {
                GamesConfirmation.ACKNOWLEDGE -> viewModel.dismissPrompt()

                GamesConfirmation.UNINSTALL -> viewModel.confirmUninstall()

                GamesConfirmation.DOWNLOAD_MISSING -> viewModel.acceptMissingLocationDownload()

                GamesConfirmation.OPEN_BLOCKING_LOCATION -> {
                    viewModel.dismissPrompt()
                    viewModel.openBlockingLocation()
                }
            }
        },
        onDismiss = viewModel::dismissPrompt,
    )
}

internal fun gamesMessage(state: GamesUiState, strings: Strings): GamesMessage? {
    state.pendingUninstall?.let { game ->
        val spec = if (state.notice == GamesNotice.POSSIBLY_RUNNING) {
            MessageBoxSpec(
                strings.uninstallGameRunningTitle,
                strings.uninstallMaybeRunningMessage.withArgs(game.name),
                MessageBoxButtons.YES_NO,
                MessageBoxIcon.ERROR,
            )
        } else {
            MessageBoxSpec(
                strings.uninstallConfirmTitle,
                strings.uninstallConfirmMessage.withArgs(game.name),
                MessageBoxButtons.YES_NO,
                MessageBoxIcon.INFORMATION,
            )
        }
        return GamesMessage(spec, GamesConfirmation.UNINSTALL)
    }
    val name = state.noticeGameName.orEmpty()
    val path = state.blockingLocation?.displayName.orEmpty()
    return when (state.notice ?: return null) {
        GamesNotice.NOT_SUPPORTED_YET -> acknowledge(
            MessageBoxSpec(
                strings.statusNotSupportedYet,
                strings.notSupportedYetMessage,
                icon = MessageBoxIcon.INFORMATION,
            ),
        )

        GamesNotice.GAME_NOT_FOUND, GamesNotice.LAUNCH_FAILED ->
            acknowledge(errorMessageBox(strings.gameExeNotFoundTitle, strings.gameExeNotFoundMessage))

        GamesNotice.GAME_RUNNING -> acknowledge(
            errorMessageBox(strings.uninstallGameRunningTitle, strings.uninstallGameRunningMessage.withArgs(name)),
        )

        GamesNotice.POSSIBLY_RUNNING -> null

        GamesNotice.FILES_NOT_FOUND -> acknowledge(
            MessageBoxSpec(
                strings.uninstallNotFoundTitle,
                strings.uninstallNotFoundMessage,
                icon = MessageBoxIcon.INFORMATION,
            ),
        )

        GamesNotice.FILES_LEFT_BEHIND -> GamesMessage(
            errorMessageBox(
                strings.uninstallErrorTitle,
                strings.uninstallErrorMessage.withArgs(name, path),
                MessageBoxButtons.YES_NO,
            ),
            GamesConfirmation.OPEN_BLOCKING_LOCATION,
        )

        GamesNotice.NOTHING_DELETED -> GamesMessage(
            errorMessageBox(
                strings.uninstallBlockedTitle,
                strings.uninstallBlockedMessage.withArgs(name, path),
                MessageBoxButtons.YES_NO,
            ),
            GamesConfirmation.OPEN_BLOCKING_LOCATION,
        )

        GamesNotice.LOCATION_NOT_FOUND -> GamesMessage(
            MessageBoxSpec(
                strings.folderNotFoundTitle,
                strings.folderNotFoundMessage,
                MessageBoxButtons.YES_NO,
                MessageBoxIcon.INFORMATION,
            ),
            GamesConfirmation.DOWNLOAD_MISSING,
        )

        GamesNotice.LOCATION_NO_HANDLER ->
            acknowledge(errorMessageBox(strings.locationNoHandlerTitle, strings.locationNoHandlerMessage))
    }
}

private fun acknowledge(spec: MessageBoxSpec) = GamesMessage(spec, GamesConfirmation.ACKNOWLEDGE)
