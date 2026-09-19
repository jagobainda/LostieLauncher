package dev.jagoba.lostielauncher.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.jagoba.lostielauncher.ui.screen.EmptyScreen
import dev.jagoba.lostielauncher.ui.theme.LostieLauncherTheme

/**
 * The single activity the application runs in.
 *
 * It does what the desktop's `MainWindow` code-behind is allowed to do and no
 * more: set up the window and hand over to the composition. Anything with a
 * decision in it belongs in a ViewModel or a `util` policy.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LostieLauncherTheme {
                EmptyScreen()
            }
        }
    }
}
