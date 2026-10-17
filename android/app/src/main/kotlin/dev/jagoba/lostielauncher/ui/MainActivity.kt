package dev.jagoba.lostielauncher.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.lostielauncher.core.lifecycle.ApplicationLifecycleObserver
import dev.jagoba.lostielauncher.model.SettingsOptions
import dev.jagoba.lostielauncher.ui.theme.LostieLauncherTheme
import dev.jagoba.lostielauncher.ui.viewmodel.SettingsViewModel
import dev.jagoba.lostielauncher.util.policy.StartupWindowPolicy
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    @Inject
    internal lateinit var settingsOptions: SettingsOptions

    @Inject
    internal lateinit var lifecycleObserver: ApplicationLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        keepStartingWindowUntilSettingsLoad()
        setContent {
            val state by settingsViewModel.state.collectAsStateWithLifecycle()
            val appearance = state ?: return@setContent

            LostieLauncherTheme(theme = appearance.theme) {
                CompositionLocalProvider(
                    LocalStrings provides appearance.strings,
                    LocalAppLanguage provides appearance.language,
                ) {
                    StartSurface(
                        theme = appearance.theme,
                        onThemeSelected = settingsViewModel::selectTheme,
                        onLanguageSelected = settingsViewModel::selectLanguage,
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleObserver.flushPendingSettings()
    }

    private fun keepStartingWindowUntilSettingsLoad() {
        val content = window.decorView
        val startedAt = SystemClock.uptimeMillis()
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val shouldKeep = StartupWindowPolicy.shouldKeep(
                        settingsLoaded = settingsViewModel.state.value != null,
                        elapsedMilliseconds = SystemClock.uptimeMillis() - startedAt,
                        timeout = settingsOptions.startupLoadTimeout,
                    )
                    if (shouldKeep) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
