package dev.jagoba.lostielauncher.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.withArgs
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.LauncherSection
import dev.jagoba.lostielauncher.ui.LocalAppLanguage
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.ShellNavigationBar
import dev.jagoba.lostielauncher.ui.component.ShellNavigationEntry
import dev.jagoba.lostielauncher.ui.component.ShellNavigationRail
import dev.jagoba.lostielauncher.ui.component.ShellPillContent
import dev.jagoba.lostielauncher.ui.component.ShellTopBar
import dev.jagoba.lostielauncher.ui.component.ShellTopBarAction
import dev.jagoba.lostielauncher.ui.dialog.WelcomeDialog
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.MainUiState
import dev.jagoba.lostielauncher.ui.viewmodel.MainViewModel

@Composable
fun LauncherShell(
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.canNavigateBack, onBack = viewModel::navigateBack)

    KirinRequirement(modifier) {
        LauncherShellLayout(
            state = state,
            onNavigate = viewModel::navigate,
            onRefresh = viewModel::refresh,
            onOpenLink = viewModel::openExternalLink,
            onUpdateKirin = viewModel::updateKirin,
            modifier = modifier,
        ) { section ->
            when (section) {
                LauncherSection.HOME -> HomeScreen()
                LauncherSection.GAMES -> GamesScreen()
                LauncherSection.LIBRARY -> LibraryScreen(state.pendingLibraryGameId, viewModel::consumeLibraryGame)
                LauncherSection.FAQS -> FaqsScreen()
                LauncherSection.SETTINGS -> SettingsScreen()
            }
        }
    }

    LibraryDialogs()
    GamesDialogs()
    if (state.isWelcomeVisible) {
        WelcomeDialog(
            language = LocalAppLanguage.current,
            onLanguageSelected = onLanguageSelected,
            onOpenRepository = { viewModel.openExternalLink(ExternalLink.GITHUB) },
            onDismiss = viewModel::dismissWelcome,
        )
    }
}

@Composable
fun LauncherShellLayout(
    state: MainUiState,
    onNavigate: (LauncherSection) -> Unit,
    onRefresh: () -> Unit,
    onOpenLink: (ExternalLink) -> Unit,
    onUpdateKirin: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (LauncherSection) -> Unit,
) {
    val colors = LocalLauncherColors.current
    val strings = LocalStrings.current
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val top = shellDestinations(TOP_SECTIONS, state, strings, onNavigate)
    val bottom = shellDestinations(BOTTOM_SECTIONS, state, strings, onNavigate)
    val actions = state.contextLinks.map { link ->
        ShellTopBarAction(
            link.name,
            link.brandName,
            painterResource(link.iconRes()),
            isLive = link == ExternalLink.TWITCH && state.isTwitchLive,
            onClick = { onOpenLink(link) },
        )
    }
    val offline = if (state.isOffline) {
        ShellPillContent(
            label = strings.offlineModeLabel,
            tooltip = strings.serverMaintenanceNotificationTitle,
            icon = painterResource(R.drawable.ic_offline),
        )
    } else {
        null
    }
    val kirinUpdate = state.kirinUpdate?.let { update ->
        ShellPillContent(
            label =
            update.downloadPercent?.let { "${strings.statusDownloading} $it%" } ?: strings.kirinUpdateRecommended,
            tooltip = strings.kirinUpdateRecommendedMessage.withArgs(
                update.installedVersion,
                update.latestVersion,
            ),
            icon = painterResource(R.drawable.ic_card_update),
            onClick = onUpdateKirin,
        )
    }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.primaryBg)) {
        val verticalInsets = with(LocalDensity.current) { (insets.getTop(this) + insets.getBottom(this)).toDp() }
        val availableHeight = maxHeight - verticalInsets - LauncherSizes.TitleBarHeight
        val layout = shellNavigationLayout(maxWidth, availableHeight, top.size + bottom.size)
        val pagerState = rememberPagerState(SECTIONS.indexOf(state.section)) { SECTIONS.size }
        val selection = { pagerState.currentPage + pagerState.currentPageOffsetFraction }
        Column(Modifier.fillMaxSize()) {
            ShellTopBar(
                title = state.title,
                logo = painterResource(R.drawable.launcher_logo),
                pills = listOfNotNull(offline, kirinUpdate),
                actions = actions,
                modifier = Modifier.windowInsetsPadding(
                    insets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                ),
            )
            if (layout is ShellNavigationLayout.Rail) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                ) {
                    ShellNavigationRail(
                        top = top,
                        bottom = bottom,
                        itemHeight = layout.itemHeight,
                        selection = selection,
                        modifier = Modifier.padding(
                            horizontal = LauncherSizes.ShellInset,
                            vertical = layout.verticalInset,
                        ),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(
                                top = layout.verticalInset,
                                end = LauncherSizes.ShellInset,
                                bottom = layout.verticalInset,
                            ),
                    ) { SectionPager(pagerState, state, onNavigate, onRefresh, content) }
                }
            } else {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal))
                        .padding(LauncherSizes.ShellInset),
                ) { SectionPager(pagerState, state, onNavigate, onRefresh, content) }
                ShellNavigationBar(
                    entries = top + bottom,
                    selection = selection,
                    modifier = Modifier
                        .background(colors.secondaryBg)
                        .windowInsetsPadding(insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                )
            }
        }
    }
}

private val TOP_SECTIONS = listOf(LauncherSection.HOME, LauncherSection.GAMES, LauncherSection.LIBRARY)
private val BOTTOM_SECTIONS = listOf(LauncherSection.FAQS, LauncherSection.SETTINGS)
private val SECTIONS = TOP_SECTIONS + BOTTOM_SECTIONS

@Composable
private fun SectionPager(
    pagerState: PagerState,
    state: MainUiState,
    onNavigate: (LauncherSection) -> Unit,
    onRefresh: () -> Unit,
    content: @Composable (LauncherSection) -> Unit,
) {
    val section by rememberUpdatedState(state.section)
    val navigate by rememberUpdatedState(onNavigate)
    LaunchedEffect(state.section) { pagerState.animateScrollToPage(SECTIONS.indexOf(state.section)) }
    LaunchedEffect(pagerState) {
        snapshotFlow { SECTIONS[pagerState.settledPage] }.collect { if (it != section) navigate(it) }
    }
    HorizontalPager(pagerState) { page ->
        SectionContent(SECTIONS[page], state, onRefresh) { content(SECTIONS[page]) }
    }
}

@Composable
private fun SectionContent(
    section: LauncherSection,
    state: MainUiState,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (section !in TOP_SECTIONS) {
        content()
        return
    }
    val colors = LocalLauncherColors.current
    val pullState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = colors.secondaryBg,
                color = colors.primaryFg,
            )
        },
    ) { content() }
}

@Composable
private fun shellDestinations(
    sections: List<LauncherSection>,
    state: MainUiState,
    strings: Strings,
    onNavigate: (LauncherSection) -> Unit,
): List<ShellNavigationEntry> = sections.map { section ->
    ShellNavigationEntry(
        key = section.name,
        label = section.label(strings),
        icon = painterResource(section.iconRes()),
        selected = state.section == section,
        onClick = { onNavigate(section) },
    )
}

private fun LauncherSection.label(strings: Strings): String = when (this) {
    LauncherSection.HOME -> strings.titleHome
    LauncherSection.GAMES -> strings.titleGames
    LauncherSection.LIBRARY -> strings.titleLibrary
    LauncherSection.FAQS -> strings.titleFaqs
    LauncherSection.SETTINGS -> strings.titleSettings
}

private fun LauncherSection.iconRes(): Int = when (this) {
    LauncherSection.HOME -> R.drawable.ic_nav_home
    LauncherSection.GAMES -> R.drawable.ic_nav_games
    LauncherSection.LIBRARY -> R.drawable.ic_nav_library
    LauncherSection.FAQS -> R.drawable.ic_nav_faqs
    LauncherSection.SETTINGS -> R.drawable.ic_nav_settings
}

private fun ExternalLink.iconRes(): Int = when (this) {
    ExternalLink.GITHUB -> R.drawable.ic_social_github
    ExternalLink.TWITCH -> R.drawable.ic_social_twitch
    ExternalLink.YOUTUBE -> R.drawable.ic_social_youtube
    ExternalLink.TWITTER -> R.drawable.ic_social_x
}
