package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.model.GameHelpAvailability
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.LocalGame
import dev.jagoba.lostielauncher.model.NewsItem
import dev.jagoba.lostielauncher.model.NotificationItem
import dev.jagoba.lostielauncher.model.NotificationType
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.GamesUiState
import dev.jagoba.lostielauncher.ui.viewmodel.HomeUiState
import dev.jagoba.lostielauncher.ui.viewmodel.InstalledGameUiState
import java.time.LocalDateTime
import java.util.UUID

private val SampleDate: LocalDateTime = LocalDateTime.of(2026, 9, 26, 18, 30)

private val SampleNews = listOf(
    NewsItem(null, "Pokémon Añil v4.13.0", "Nueva ruta y correcciones.", "Novedad", SampleDate, null),
    NewsItem(null, "Torneo de la comunidad", "Inscripciones abiertas.", "Evento", SampleDate.minusDays(3), null),
)

private val SampleNotifications = listOf(
    NotificationItem(
        null,
        "Nueva versión",
        "Pokémon Añil v4.13.0 ya disponible.",
        NotificationType.INFO,
        SampleDate,
        null,
    ),
    NotificationItem(null, "Mantenimiento", "Domingo de 10:00 a 12:00.", NotificationType.WARNING, SampleDate, null),
)

private val SampleHome = HomeUiState(news = SampleNews, notifications = SampleNotifications, isLoading = false)

private val SampleRemote = GameInfo(
    id = UUID.fromString("0f8f4c52-3a4e-4a55-9a8f-6f0e6b1f2a10"),
    name = "Pokémon Añil",
    version = "v4.13.0",
    sizeGb = 0.574,
    description = "",
    pageUrl = "",
    logoUrl = "https://ericlostie-launcher.jagoba.dev/public/imgs/4.png",
    relativePath = "/pokemon-anil.zip",
    sha256 = "a".repeat(64),
)

private val SampleInstalled = InstalledGameUiState(
    game = LocalGame(SampleRemote.id!!, SampleRemote.name, "v4.12.0", null),
    remote = SampleRemote,
    hasUpdate = true,
    playtimeMinutes = 754,
    helpAvailability = GameHelpAvailability.AVAILABLE,
    isUpdating = false,
    isUninstalling = false,
)

private val SampleGames = GamesUiState(
    isLoading = false,
    installedStateKnown = true,
    games = listOf(
        SampleInstalled,
        SampleInstalled.copy(
            game = LocalGame(UUID.randomUUID(), "Pokémon Añil Nuzlocke", "v4.12.0", "NUZLOCKE"),
            helpAvailability = GameHelpAvailability.NOT_FOUND,
        ),
    ),
)

private enum class ScreenSample(val label: String) {
    HOME_LOADING("Home · loading"),
    HOME_LIST("Home · list"),
    HOME_OFFLINE("Home · offline banner"),
    HOME_STALE("Home · stale banner"),
    HOME_COLUMN_EMPTY("Home · news empty"),
    HOME_EMPTY("Home · empty"),
    HOME_UNAVAILABLE("Home · empty and stale"),
    GAMES_LOADING("My Games · loading"),
    GAMES_LIST("My Games · list"),
    GAMES_LIST_UNSUPPORTED("My Games · list, runtime not supported"),
    GAMES_EMPTY("My Games · empty"),
    GAMES_UNSUPPORTED("My Games · installed state not supported (today's Android)"),
}

private val NoGamesActions = GamesActions({}, {}, {}, {}, {}, {}, {})

@Composable
fun ScreenCatalogScreen(modifier: Modifier = Modifier) {
    var sample by rememberSaveable { mutableStateOf(ScreenSample.HOME_LIST) }
    Column(
        modifier
            .fillMaxSize()
            .background(LocalLauncherColors.current.primaryBg),
    ) {
        FlowRow(
            Modifier
                .fillMaxWidth()
                .padding(LauncherSpacing.Medium),
            horizontalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(LauncherSpacing.Small),
        ) {
            ScreenSample.entries.forEach { candidate ->
                Chip(label = candidate.label, selected = candidate == sample, onClick = { sample = candidate })
            }
        }
        Box(Modifier.weight(1f)) {
            when (sample) {
                ScreenSample.HOME_LOADING -> HomeContent(HomeUiState(), onOpenLink = {})

                ScreenSample.HOME_LIST -> HomeContent(SampleHome, onOpenLink = {})

                ScreenSample.HOME_OFFLINE -> HomeContent(SampleHome.copy(isOffline = true), onOpenLink = {})

                ScreenSample.HOME_STALE -> HomeContent(SampleHome.copy(isContentStale = true), onOpenLink = {})

                ScreenSample.HOME_COLUMN_EMPTY -> HomeContent(SampleHome.copy(news = emptyList()), onOpenLink = {})

                ScreenSample.HOME_EMPTY -> HomeContent(HomeUiState(isLoading = false), onOpenLink = {})

                ScreenSample.HOME_UNAVAILABLE ->
                    HomeContent(HomeUiState(isLoading = false, isContentStale = true), onOpenLink = {})

                ScreenSample.GAMES_LOADING -> GamesContent(GamesUiState(), NoGamesActions)

                ScreenSample.GAMES_LIST -> GamesContent(SampleGames, NoGamesActions)

                ScreenSample.GAMES_LIST_UNSUPPORTED ->
                    GamesContent(SampleGames.copy(runtimeSupported = false), NoGamesActions)

                ScreenSample.GAMES_EMPTY ->
                    GamesContent(GamesUiState(isLoading = false, installedStateKnown = true), NoGamesActions)

                ScreenSample.GAMES_UNSUPPORTED -> GamesContent(GamesUiState(isLoading = false), NoGamesActions)
            }
        }
    }
}
