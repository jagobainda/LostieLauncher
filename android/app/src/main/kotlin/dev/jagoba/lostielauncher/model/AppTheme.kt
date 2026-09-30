package dev.jagoba.lostielauncher.model

enum class AppTheme {
    Volcarona,
    Zoroark,
    Infernape,
    Torterra,
    Empoleon,
    Mewtwo,
    Cefireon,
    Sylveon,
    Astrem,
    Auretoskos,
    ;

    companion object {
        val Default = Volcarona

        fun fromNameOrDefault(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: Default
    }
}
