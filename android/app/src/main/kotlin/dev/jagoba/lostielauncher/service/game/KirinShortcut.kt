package dev.jagoba.lostielauncher.service.game

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.model.KirinOptions
import java.io.File
import javax.inject.Inject

internal interface KirinShortcut {
    fun open(gameDirectory: File)
}

internal class AndroidKirinShortcut @Inject constructor(
    @ApplicationContext private val context: Context,
    private val options: KirinOptions,
) : KirinShortcut {
    override fun open(gameDirectory: File) {
        context.startActivity(
            Intent(options.launchAction)
                .setClassName(options.packageName, options.launchActivity)
                .putExtra(options.gamePathExtra, gameDirectory.absolutePath)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
