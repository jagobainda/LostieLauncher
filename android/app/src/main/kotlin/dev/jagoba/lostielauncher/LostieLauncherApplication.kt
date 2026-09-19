package dev.jagoba.lostielauncher

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The application entry point, and the root of the dependency graph.
 *
 * The desktop builds its container in `App.OnStartup` and then does a fair
 * amount besides: a single-instance mutex, global exception hooks, log
 * retention, a silent update check, a tray icon. Almost none of that has an
 * Android counterpart — `spec/10-windows-only.md` lists what and why — so this
 * class stays empty until a later step has a reason to put something in it.
 */
@HiltAndroidApp
class LostieLauncherApplication : Application()
