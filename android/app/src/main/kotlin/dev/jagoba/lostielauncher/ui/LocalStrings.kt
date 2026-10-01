package dev.jagoba.lostielauncher.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import dev.jagoba.lostielauncher.content.Strings
import dev.jagoba.lostielauncher.content.strings.EspStrings
import dev.jagoba.lostielauncher.model.AppLanguage

val LocalStrings: ProvidableCompositionLocal<Strings> = staticCompositionLocalOf { EspStrings }

val LocalAppLanguage: ProvidableCompositionLocal<AppLanguage> =
    staticCompositionLocalOf { AppLanguage.Default }
