package dev.jagoba.lostielauncher.model

enum class AppLanguage(val code: String, val displayName: String) {
    ESP("es", "Español"),
    ENG("en", "English"),
    CAT("ca", "Català"),
    EUS("eu", "Euskera"),
    GAL("gl", "Galego"),
    POR("pt", "Português"),
    VAL("val", "Valencià"),
    FRA("fr", "Français"),
    ;

    companion object {
        val Default = ESP

        fun fromNameOrDefault(name: String?): AppLanguage = entries.firstOrNull { it.name == name } ?: Default
    }
}
