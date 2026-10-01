package dev.jagoba.lostielauncher.util.format

import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource

class CardDateFormatterTest {
    private val march = LocalDateTime.of(2026, 3, 5, 23, 30)
    private val april = LocalDateTime.of(2026, 4, 5, 8, 0)

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        "ESP, 05 mar 2026, 05 abr 2026, 05 abr",
        "ENG, 05 Mar 2026, 05 Apr 2026, 05 Apr",
        "CAT, 05 març 2026, 05 abr. 2026, 05 abr.",
        "EUS, 05 mar. 2026, 05 api. 2026, 05 api.",
        "GAL, 05 mar. 2026, 05 abr. 2026, 05 abr.",
        "POR, 05 mar. 2026, 05 abr. 2026, 05 abr.",
        "VAL, 05 març 2026, 05 abr. 2026, 05 abr.",
        "FRA, 05 mars 2026, 05 avr. 2026, 05 avr.",
    )
    fun `formats news and notification dates in every launcher language`(
        language: AppLanguage,
        newsMarch: String,
        newsApril: String,
        notificationApril: String,
    ) {
        CardDateFormatter.news(march, language) shouldBe newsMarch
        CardDateFormatter.news(april, language) shouldBe newsApril
        CardDateFormatter.notification(april, language) shouldBe notificationApril
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AppLanguage::class)
    fun `every month is day, bare month name and year, with no genitive preposition`(language: AppLanguage) {
        (1..12).forEach { month ->
            CardDateFormatter.news(LocalDateTime.of(2026, month, 5, 12, 0), language) shouldMatch
                Regex("""05 [^\s\d']+ 2026""")
        }
    }

    @Test
    fun `the written wall clock is formatted without a zone shift`() {
        CardDateFormatter.news(LocalDateTime.of(2026, 12, 31, 23, 59), AppLanguage.ENG) shouldBe "31 Dec 2026"
    }

    @Test
    fun `valencian resolves to the valencian catalan locale`() {
        val locale = CardDateFormatter.localeFor(AppLanguage.VAL)

        locale.language shouldBe "ca"
        locale.variant shouldBe "valencia"
    }

    @Test
    fun `every other language uses its wire code`() {
        AppLanguage.entries.filter { it != AppLanguage.VAL }.forEach {
            CardDateFormatter.localeFor(it).language shouldBe it.code
        }
    }
}
