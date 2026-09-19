package dev.jagoba.lostielauncher.model

/**
 * The eight languages the launcher ships in.
 *
 * The member order is the desktop's `Models/AppLanguage.cs` order, deliberately:
 * the setting is persisted as an ordinal there, and the language picker lists
 * them in this order on both sides.
 *
 * [code] is the key the CDN uses inside a localized field of the home content —
 * the desktop's `ContentService.GetLanguageCode`. Note that Valencian is `val`,
 * not a two-letter code.
 *
 * [displayName] is the desktop's `[Description]` attribute on the same member:
 * each language's own endonym, and the one piece of text in the launcher that
 * is deliberately **not** translated — the Settings picker shows all eight at
 * once, so a reader has to be able to find their own in a language they cannot
 * read. That is why it lives here and not in the string catalogue.
 */
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
        /**
         * The language a fresh install starts in, and the one a stored value
         * falls back to when it does not name a member. The desktop's
         * `AppSettings.Language` default and its `SettingsService` fallback.
         */
        val Default = ESP

        /**
         * [name] back to a member, or [Default] when it names nothing.
         *
         * The persisted form is the member name, not the ordinal the desktop
         * writes: this side stores settings as key-value pairs rather than as a
         * serialized object, and a name survives a member being inserted in the
         * middle of the enum, which an ordinal does not.
         */
        fun fromNameOrDefault(name: String?): AppLanguage = entries.firstOrNull { it.name == name } ?: Default
    }
}
