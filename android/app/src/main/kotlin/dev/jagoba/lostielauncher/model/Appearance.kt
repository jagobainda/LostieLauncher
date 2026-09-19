package dev.jagoba.lostielauncher.model

/**
 * The two settings that decide what the launcher looks like and what language
 * it speaks.
 *
 * They are the desktop's `AppSettings.Theme` and `AppSettings.Language`, split
 * out from the rest of that record deliberately: they are the only two settings
 * every screen reacts to, they are the two the port plan's step 05 delivers,
 * and they change what is on screen rather than what the launcher does.
 *
 * Port plan step 07 owns the full settings surface — the download directory,
 * auto-update, whether the welcome dialog has been seen. When it lands, this
 * either becomes part of that record or stays beside it, but the observable
 * behaviour must not change: a new value repaints and re-labels the running UI
 * and is written back to disk.
 */
data class Appearance(val theme: AppTheme = AppTheme.Default, val language: AppLanguage = AppLanguage.Default)
