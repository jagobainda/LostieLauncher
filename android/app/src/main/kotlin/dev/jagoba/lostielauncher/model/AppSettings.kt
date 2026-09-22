package dev.jagoba.lostielauncher.model

data class AppSettings(
    val theme: AppTheme = AppTheme.Volcarona,
    val language: AppLanguage = AppLanguage.ESP,
    val hasSeenWelcome: Boolean = false,
) {
    val appearance: Appearance
        get() = Appearance(theme, language)
}
