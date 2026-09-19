package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors

/**
 * The whole user interface, for now: the window background, in the theme's
 * colour, and nothing on it.
 *
 * It is what port plan step 03 asks the scaffold to show. Step 11 replaces it
 * with the navigation shell, and step 14 with the five real screens.
 */
@Composable
fun EmptyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalLauncherColors.current.primaryBg),
    )
}
