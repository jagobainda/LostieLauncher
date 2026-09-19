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
 * Only the wire code lives here. The display names and the text catalogue
 * itself arrive with port plan step 05.
 */
enum class AppLanguage(val code: String) {
    ESP("es"),
    ENG("en"),
    CAT("ca"),
    EUS("eu"),
    GAL("gl"),
    POR("pt"),
    VAL("val"),
    FRA("fr"),
}
