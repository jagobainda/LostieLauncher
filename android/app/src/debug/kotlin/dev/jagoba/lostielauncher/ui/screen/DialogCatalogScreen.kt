package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.GameLocationReference
import dev.jagoba.lostielauncher.model.GameTarget
import dev.jagoba.lostielauncher.ui.LocalAppLanguage
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.dialog.DownloadConfirmDialog
import dev.jagoba.lostielauncher.ui.dialog.DownloadConfirmState
import dev.jagoba.lostielauncher.ui.dialog.LauncherMessageBox
import dev.jagoba.lostielauncher.ui.dialog.MessageBoxSpec
import dev.jagoba.lostielauncher.ui.dialog.SpecialVersionDialog
import dev.jagoba.lostielauncher.ui.dialog.WelcomeDialog
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.GamesNotice
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.LibraryNotice
import dev.jagoba.lostielauncher.util.format.FreeSpaceFormatter

private const val SAMPLE_GAME = "Pokémon Añil"
private const val SAMPLE_LOGO = "https://ericlostie-launcher.jagoba.dev/public/imgs/4.png"
private const val SAMPLE_PATH = "/storage/emulated/0/Android/data/dev.jagoba.lostielauncher/files/games/downloads"
private val SampleLocation = GameLocationReference("sample", "$SAMPLE_PATH/$SAMPLE_GAME/save/Game.rxdata")
private val SampleTarget = GameTarget(null, SAMPLE_GAME)

private val SampleDownload = DownloadConfirmState(
    title = SAMPLE_GAME,
    description = "Nueva ruta, gimnasio rediseñado y correcciones de la región de Kanto.",
    logoUrl = SAMPLE_LOGO,
    hasPage = true,
    path = SAMPLE_PATH,
    size = "588 MB",
    freeSpace = FreeSpaceFormatter.format(12L * 1024 * 1024 * 1024),
)

private sealed interface CatalogDialog {
    data class Message(val spec: (Strings) -> MessageBoxSpec?) : CatalogDialog

    data class Download(val state: DownloadConfirmState) : CatalogDialog

    data object SpecialVersion : CatalogDialog

    data object Welcome : CatalogDialog
}

private fun games(state: GamesUiState) = CatalogDialog.Message { gamesMessage(state, it)?.spec }

private val Entries: List<Pair<String, CatalogDialog>> =
    LibraryNotice.entries.map { notice -> "Library · $notice" to CatalogDialog.Message { notice.messageBox(it) } } +
        listOf(
            "Library · cancel download" to CatalogDialog.Message(::cancelDownloadMessageBox),
            "Library · download confirmation" to CatalogDialog.Download(SampleDownload),
            "Library · download confirmation, no page, no description, free space unknown" to
                CatalogDialog.Download(
                    SampleDownload.copy(
                        description = "",
                        logoUrl = null,
                        hasPage = false,
                        freeSpace = FreeSpaceFormatter.format(null),
                    ),
                ),
            "Library · special version" to CatalogDialog.SpecialVersion,
            "My Games · uninstall confirmation" to games(GamesUiState(pendingUninstall = SampleTarget)),
            "My Games · uninstall, possibly running" to
                games(GamesUiState(pendingUninstall = SampleTarget, notice = GamesNotice.POSSIBLY_RUNNING)),
        ) +
        GamesNotice.entries.filter { it != GamesNotice.POSSIBLY_RUNNING }.map { notice ->
            "My Games · $notice" to games(
                GamesUiState(notice = notice, noticeGameName = SAMPLE_GAME, blockingLocation = SampleLocation),
            )
        } +
        listOf("App · welcome" to CatalogDialog.Welcome)

@Composable
fun DialogCatalogScreen(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    var open by rememberSaveable { mutableIntStateOf(-1) }
    var lastResult by rememberSaveable { mutableStateOf("") }
    val close: (String) -> Unit = { result ->
        lastResult = "${Entries[open].first}: $result"
        open = -1
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryBg),
        contentPadding = PaddingValues(vertical = LauncherSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
    ) {
        item { AppearancePickers(theme, onThemeSelected, onLanguageSelected) }
        item { SectionHeader("Dialogs · tap one to open it") }
        item { Caption("last result: ${lastResult.ifEmpty { "none" }}") }
        itemsIndexed(Entries) { index, (name, _) ->
            Text(
                text = name,
                color = colors.secondaryFg,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.secondaryBg, RoundedCornerShape(LauncherRadii.Small))
                    .clickable { open = index }
                    .padding(LauncherSpacing.MediumLarge),
            )
        }
    }

    when (val dialog = Entries.getOrNull(open)?.second) {
        is CatalogDialog.Message -> dialog.spec(LocalStrings.current)?.let { spec ->
            LauncherMessageBox(spec, onConfirm = { close("confirmed") }, onDismiss = { close("dismissed") })
        }

        is CatalogDialog.Download -> DownloadConfirmDialog(
            state = dialog.state,
            onViewPage = { lastResult = "view page" },
            onConfirm = { close("download, key \"$it\"") },
            onDismiss = { close("dismissed") },
        )

        CatalogDialog.SpecialVersion -> SpecialVersionDialog(
            onConfirm = { close("key \"$it\"") },
            onDismiss = { close("dismissed") },
        )

        CatalogDialog.Welcome -> WelcomeDialog(
            language = LocalAppLanguage.current,
            onLanguageSelected = onLanguageSelected,
            onOpenRepository = { lastResult = "repository" },
            onDismiss = { close("continue") },
        )

        null -> Unit
    }
}
