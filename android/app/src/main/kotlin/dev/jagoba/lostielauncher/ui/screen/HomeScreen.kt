package dev.jagoba.lostielauncher.ui.screen

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.NewsItem
import dev.jagoba.lostielauncher.model.NotificationItem
import dev.jagoba.lostielauncher.ui.LocalStrings
import dev.jagoba.lostielauncher.ui.component.EmptyState
import dev.jagoba.lostielauncher.ui.component.NewsCard
import dev.jagoba.lostielauncher.ui.component.NewsCardSkeleton
import dev.jagoba.lostielauncher.ui.component.NotificationCard
import dev.jagoba.lostielauncher.ui.component.NotificationCardSkeleton
import dev.jagoba.lostielauncher.ui.theme.FixedColors
import dev.jagoba.lostielauncher.ui.theme.LauncherBorders
import dev.jagoba.lostielauncher.ui.theme.LauncherRadii
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LauncherType
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.ui.viewmodel.HomeUiState
import dev.jagoba.lostielauncher.ui.viewmodel.HomeViewModel

private const val SKELETON_COUNT = 4

@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state = state, onOpenLink = viewModel::openLink, modifier = modifier)
}

@Composable
internal fun HomeContent(state: HomeUiState, onOpenLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    if (state.isEmpty) {
        EmptyState(
            icon = R.drawable.ic_offline,
            message = if (state.isContentStale) strings.homeContentUnavailable else strings.homeNoContent,
            modifier = modifier,
        )
        return
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .padding(LauncherSpacing.Screen),
    ) {
        if (maxWidth >= LauncherSizes.HomeColumnMinWidth * 2 + LauncherSpacing.Screen) {
            HomeColumns(state, onOpenLink)
        } else {
            HomeSingleColumn(state, onOpenLink)
        }
    }
}

@Composable
private fun HomeColumns(state: HomeUiState, onOpenLink: (String) -> Unit) {
    val strings = LocalStrings.current
    Column(Modifier.fillMaxSize()) {
        HomeBanner(state)
        Row(Modifier.weight(1f)) {
            Column(Modifier.weight(1f)) {
                HomeHeader(strings.homeNews)
                when {
                    state.isLoading -> SkeletonColumn { NewsCardSkeleton(CardSpacing) }
                    state.isNewsEmpty -> HomeColumnEmpty(R.drawable.ic_home_newspaper)
                    else -> LazyColumn { newsItems(state.news, onOpenLink) }
                }
            }
            Spacer(Modifier.width(LauncherSpacing.Screen))
            Column(Modifier.weight(1f)) {
                HomeHeader(strings.homeNotifications)
                when {
                    state.isLoading -> SkeletonColumn { NotificationCardSkeleton(CardSpacing) }
                    state.isNotificationsEmpty -> HomeColumnEmpty(R.drawable.ic_home_bell_off)
                    else -> LazyColumn { notificationItems(state.notifications) }
                }
            }
        }
    }
}

@Composable
private fun HomeSingleColumn(state: HomeUiState, onOpenLink: (String) -> Unit) {
    val strings = LocalStrings.current
    LazyColumn(Modifier.fillMaxSize()) {
        item { HomeBanner(state) }
        item { HomeHeader(strings.homeNews) }
        when {
            state.isLoading -> items(SKELETON_COUNT) { NewsCardSkeleton(CardSpacing) }
            state.isNewsEmpty -> item { HomeColumnEmpty(R.drawable.ic_home_newspaper) }
            else -> newsItems(state.news, onOpenLink)
        }
        item { HomeHeader(strings.homeNotifications, Modifier.padding(top = LauncherSpacing.MediumLarge)) }
        when {
            state.isLoading -> items(SKELETON_COUNT) { NotificationCardSkeleton(CardSpacing) }
            state.isNotificationsEmpty -> item { HomeColumnEmpty(R.drawable.ic_home_bell_off) }
            else -> notificationItems(state.notifications)
        }
    }
}

private val CardSpacing = Modifier.padding(bottom = LauncherSpacing.MediumLarge)

private fun LazyListScope.newsItems(news: List<NewsItem>, onOpenLink: (String) -> Unit) {
    items(news) { item ->
        NewsCard(
            tag = item.tag,
            date = item.date,
            title = item.title,
            description = item.description,
            onOpenLink = onOpenLink,
            modifier = CardSpacing,
        )
    }
}

private fun LazyListScope.notificationItems(notifications: List<NotificationItem>) {
    items(notifications) { item ->
        NotificationCard(
            title = item.title,
            message = item.message,
            type = item.type,
            date = item.date,
            modifier = CardSpacing,
        )
    }
}

@Composable
private fun ColumnScope.SkeletonColumn(skeleton: @Composable () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clipToBounds(),
    ) { repeat(SKELETON_COUNT) { skeleton() } }
}

@Composable
private fun HomeHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalLauncherColors.current.secondaryFg,
        fontSize = LauncherType.TitleSize,
        fontWeight = LauncherType.SemiBold,
        modifier = modifier.padding(bottom = LauncherSpacing.ExtraLarge),
    )
}

@Composable
private fun HomeColumnEmpty(@DrawableRes icon: Int) {
    EmptyState(icon = icon, message = LocalStrings.current.homeNoContent, compact = true)
}

@Composable
private fun HomeBanner(state: HomeUiState) {
    val strings = LocalStrings.current
    when {
        state.isLoading -> Unit

        state.isOffline -> HomeBanner(
            R.drawable.ic_offline,
            strings.offlineModeLabel,
            strings.serverMaintenanceNotificationMessage,
        )

        state.isOutOfDateWarningVisible -> HomeBanner(
            R.drawable.ic_home_cloud_alert,
            strings.contentOutOfDateLabel,
            strings.contentOutOfDateMessage,
        )
    }
}

@Composable
private fun HomeBanner(@DrawableRes icon: Int, title: String, message: String) {
    val colors = LocalLauncherColors.current
    val shape = RoundedCornerShape(LauncherRadii.Medium)
    Row(
        Modifier
            .padding(bottom = LauncherSpacing.Card)
            .fillMaxWidth()
            .background(colors.overlaySubtle, shape)
            .border(LauncherBorders.Thin, FixedColors.Warning, shape)
            .padding(LauncherSpacing.ExtraLarge),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = FixedColors.Warning,
            modifier = Modifier.size(LauncherSizes.HomeBannerIcon),
        )
        Spacer(Modifier.width(LauncherSpacing.Large))
        Column {
            Text(
                text = title,
                color = colors.secondaryFg,
                fontSize = LauncherType.SectionSize,
                fontWeight = LauncherType.SemiBold,
            )
            Text(
                text = message,
                color = colors.secondaryFgDim,
                fontSize = LauncherType.CaptionSize,
                modifier = Modifier.padding(top = LauncherSpacing.ExtraSmall),
            )
        }
    }
}
