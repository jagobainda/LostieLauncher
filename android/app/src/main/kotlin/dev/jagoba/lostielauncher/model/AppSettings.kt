package dev.jagoba.lostielauncher.model

data class AppSettings(
    val theme: AppTheme = AppTheme.Default,
    val language: AppLanguage = AppLanguage.Default,
    val hasSeenWelcome: Boolean = false,
    val autoUpdate: Boolean = false,
) {
    val appearance: Appearance
        get() = Appearance(theme, language)
}
