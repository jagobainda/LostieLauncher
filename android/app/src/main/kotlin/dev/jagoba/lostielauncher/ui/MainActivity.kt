package dev.jagoba.lostielauncher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.lostielauncher.ui.theme.LostieLauncherTheme
import dev.jagoba.lostielauncher.ui.viewmodel.AppearanceViewModel

/**
 * The single activity the application runs in.
 *
 * It does what the desktop's `MainWindow` code-behind is allowed to do and no
 * more: set up the window and hand over to the composition. Anything with a
 * decision in it belongs in a ViewModel or a `util` policy.
 *
 * Note what it does **not** do: react to a theme or a language change. Both are
 * ordinary state here, so a change recomposes and the activity never restarts —
 * which is the behaviour `spec/05-localization.md` and `spec/06-design-tokens.md`
 * require, and the reason neither setting uses an Android resource qualifier.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppearanceViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Null until the stored settings have been read once. Rendering
            // the defaults meanwhile would draw a whole Volcarona page and then
            // replace it, so instead nothing is composed for the frame or two
            // the read takes.
            //
            // That is not the same as showing nothing: `themes.xml` has already
            // painted the window `volcarona_primary_bg`, because the platform
            // reads it before any Kotlin runs and therefore cannot know which
            // theme was chosen. A user on one of the three light themes still
            // gets a brief dark grey frame. Fixing that properly means reading
            // the setting before the first frame — a splash-screen keep-on
            // condition, or a synchronous first read — and it belongs with the
            // rest of settings in port plan step 07.
            val appearance = state ?: return@setContent

            LostieLauncherTheme(theme = appearance.theme) {
                CompositionLocalProvider(
                    LocalStrings provides appearance.strings,
                    LocalAppLanguage provides appearance.language,
                ) {
                    StartSurface(
                        theme = appearance.theme,
                        onThemeSelected = viewModel::selectTheme,
                        onLanguageSelected = viewModel::selectLanguage,
                    )
                }
            }
        }
    }
}
