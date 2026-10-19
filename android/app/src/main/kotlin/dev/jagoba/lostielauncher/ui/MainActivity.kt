package dev.jagoba.lostielauncher.ui

import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.lostielauncher.core.lifecycle.ApplicationLifecycleObserver
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.SettingsOptions
import dev.jagoba.lostielauncher.ui.screen.LauncherShell
import dev.jagoba.lostielauncher.ui.theme.LostieLauncherTheme
import dev.jagoba.lostielauncher.ui.theme.splashThemeFor
import dev.jagoba.lostielauncher.ui.viewmodel.SettingsViewModel
import dev.jagoba.lostielauncher.util.policy.StartupWindowPolicy
import javax.inject.Inject

private const val SPLASH_EXIT_DURATION_MILLISECONDS = 500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    internal lateinit var settingsOptions: SettingsOptions

    @Inject
    internal lateinit var lifecycleObserver: ApplicationLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepSplashScreenUntilSettingsLoad(splash)
        splash.setOnExitAnimationListener(::animateSplashScreenExit)
        setContent {
            val state by settingsViewModel.state.collectAsStateWithLifecycle()
            val appearance = state ?: return@setContent

            LaunchedEffect(appearance.theme) { rememberSplashScreenTheme(appearance.theme) }
            LostieLauncherTheme(theme = appearance.theme) {
                CompositionLocalProvider(
                    LocalStrings provides appearance.strings,
                    LocalAppLanguage provides appearance.language,
                ) {
                    LauncherShell(onLanguageSelected = settingsViewModel::selectLanguage)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleObserver.flushPendingSettings()
    }

    private fun keepSplashScreenUntilSettingsLoad(splashScreen: SplashScreen) {
        val startedAt = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition {
            StartupWindowPolicy.shouldKeep(
                settingsLoaded = settingsViewModel.state.value != null,
                elapsedMilliseconds = SystemClock.uptimeMillis() - startedAt,
                timeout = settingsOptions.startupLoadTimeout,
            )
        }
    }

    private fun rememberSplashScreenTheme(theme: AppTheme) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(splashThemeFor(theme))
        }
    }

    private fun animateSplashScreenExit(provider: SplashScreenViewProvider) {
        provider.iconView.animate()
            .translationY(-provider.iconView.bottom.toFloat())
            .setInterpolator(AnticipateInterpolator())
            .setDuration(SPLASH_EXIT_DURATION_MILLISECONDS)
            .start()
        provider.view.animate()
            .alpha(0f)
            .setDuration(SPLASH_EXIT_DURATION_MILLISECONDS)
            .withEndAction(provider::remove)
            .start()
    }
}
