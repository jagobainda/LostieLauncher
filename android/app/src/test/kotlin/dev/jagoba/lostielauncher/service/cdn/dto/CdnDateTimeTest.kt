package dev.jagoba.lostielauncher.service.cdn.dto

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.TimeZone
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CdnDateTime")
class CdnDateTimeTest {
    @Test
    fun `takes a timestamp with no offset as already being CET`() {
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00")

        parsed.asWritten shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
    }

    @Test
    fun `converts a UTC timestamp into CET`() {
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00Z")

        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 14, 0)
        parsed.asWritten shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
    }

    @Test
    fun `converts a UTC timestamp into CET across the winter offset too`() {
        CdnDateTime.parse("2026-01-15T12:00:00Z").inCet shouldBe LocalDateTime.of(2026, 1, 15, 13, 0)
    }

    @Test
    fun `converts an explicit offset into CET`() {
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00-05:00")

        parsed.inCet shouldBe OffsetDateTime.parse("2026-06-19T12:00:00-05:00")
            .atZoneSameInstant(CdnDateTime.CET)
            .toLocalDateTime()
        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 19, 0)
    }

    @Test
    fun `does not fall back to the device time zone`() {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        try {
            CdnDateTime.parse("2026-06-19T12:00:00Z").inCet shouldBe LocalDateTime.of(2026, 6, 19, 14, 0)
            CdnDateTime.parse("2026-06-19T12:00:00").inCet shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `refuses text that is not a timestamp`() {
        shouldThrow<DateTimeParseException> { CdnDateTime.parse("last Tuesday") }
    }

    @Test
    fun `fails the payload rather than dropping a malformed timestamp`() {
        val json = """{"news":[{"date":"nope"}],"notifications":[]}"""

        shouldThrow<Exception> { Json.decodeFromString<HomeContentDto>(json) }
    }
}
