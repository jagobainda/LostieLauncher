package dev.jagoba.lostielauncher.util.format

import dev.jagoba.lostielauncher.model.AppLanguage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object CardDateFormatter {
    private val DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("dd")
    private val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL")
    private const val VALENCIAN_TAG = "ca-ES-valencia"

    fun news(date: LocalDateTime, language: AppLanguage): String = "${notification(date, language)} ${date.year}"

    fun notification(date: LocalDateTime, language: AppLanguage): String {
        val locale = localeFor(language)
        val month = MONTH.withLocale(locale).format(date)
        return "${DAY.format(date)} ${if (language == AppLanguage.ENG) month else month.lowercase(locale)}"
    }

    internal fun localeFor(language: AppLanguage): Locale =
        Locale.forLanguageTag(if (language == AppLanguage.VAL) VALENCIAN_TAG else language.code)
}
