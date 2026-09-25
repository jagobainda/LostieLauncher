package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import dev.jagoba.lostielauncher.ui.theme.LauncherSizes

internal sealed interface ShellNavigationLayout {
    data object Bar : ShellNavigationLayout

    data class Rail(val itemHeight: Dp, val verticalInset: Dp) : ShellNavigationLayout
}

internal fun shellNavigationLayout(width: Dp, availableHeight: Dp, itemCount: Int): ShellNavigationLayout {
    if (width < LauncherSizes.NavigationRailBreakpoint || itemCount <= 0) return ShellNavigationLayout.Bar
    val minimum = LauncherSizes.MinimumTouchTarget * itemCount
    if (availableHeight < minimum) return ShellNavigationLayout.Bar
    val inset = ((availableHeight - minimum) / 2).coerceIn(0.dp, LauncherSizes.ShellInset)
    val itemHeight = min((availableHeight - inset * 2) / itemCount, LauncherSizes.NavigationItemSize)
    return ShellNavigationLayout.Rail(itemHeight, inset)
}
