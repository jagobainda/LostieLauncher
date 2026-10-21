package dev.jagoba.lostielauncher.model

import java.io.File

data class LauncherUpdateOptions(val packageName: String, val feedUrl: String, val installerFile: File)
