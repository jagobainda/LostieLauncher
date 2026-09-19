package dev.jagoba.lostielauncher.model

/**
 * The ten themes the launcher ships with.
 *
 * Member for member, and in order, the desktop's `Models/AppTheme.cs`. Each one
 * names a resource dictionary in `desktop/LostieLauncher/Themes/`; here it
 * names a palette in `ui/theme/Palettes.kt`, and the mapping is exhaustive, so
 * an eleventh theme is a compile error rather than a screen with no colours.
 *
 * The enum carries no colour itself: this is a `model/` type, the palette is a
 * `ui/theme/` one, and nothing in the service or ViewModel layers should have
 * to reference a `Color` to talk about the setting.
 */
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
        /**
         * The theme a fresh install starts in, and the one the desktop falls
         * back to when a selected theme fails to load.
         */
        val Default = Volcarona

        /**
         * [name] back to a member, or [Default] when it names nothing. See
         * [AppLanguage.fromNameOrDefault] for why the persisted form is the
         * name rather than the desktop's ordinal.
         */
        fun fromNameOrDefault(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: Default
    }
}
