package dev.jagoba.lostielauncher.ui.theme

import androidx.annotation.StyleRes
import dev.jagoba.lostielauncher.R
import dev.jagoba.lostielauncher.model.AppTheme

@StyleRes
fun splashThemeFor(theme: AppTheme): Int = when (theme) {
    AppTheme.Volcarona -> R.style.Theme_LostieLauncher_Splash
    AppTheme.Zoroark -> R.style.Theme_LostieLauncher_Splash_Zoroark
    AppTheme.Infernape -> R.style.Theme_LostieLauncher_Splash_Infernape
    AppTheme.Torterra -> R.style.Theme_LostieLauncher_Splash_Torterra
    AppTheme.Empoleon -> R.style.Theme_LostieLauncher_Splash_Empoleon
    AppTheme.Mewtwo -> R.style.Theme_LostieLauncher_Splash_Mewtwo
    AppTheme.Cefireon -> R.style.Theme_LostieLauncher_Splash_Cefireon
    AppTheme.Sylveon -> R.style.Theme_LostieLauncher_Splash_Sylveon
    AppTheme.Astrem -> R.style.Theme_LostieLauncher_Splash_Astrem
    AppTheme.Auretoskos -> R.style.Theme_LostieLauncher_Splash_Auretoskos
}
