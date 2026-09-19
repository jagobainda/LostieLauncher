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

/**
 * Ported from the `NormalizeToCet` cases in the desktop's
 * `Services/ContentServiceTests.cs`.
 *
 * The desktop splits on `DateTime.Kind`, which the wire does not carry: what it
 * actually splits on is whether the published text has an offset. The three
 * cases map one for one — UTC-kind is a `Z` suffix, local-kind is an explicit
 * offset, unspecified-kind is no offset at all.
 */
@DisplayName("CdnDateTime")
class CdnDateTimeTest {
    @Test
    fun `takes a timestamp with no offset as already being CET`() {
        // Arrange — this is what the real payload always looks like.
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00")

        // Assert
        parsed.asWritten shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
    }

    @Test
    fun `converts a UTC timestamp into CET`() {
        // Arrange — June, so CET is at summer time and two hours ahead of UTC.
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00Z")

        // Assert
        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 14, 0)
        parsed.asWritten shouldBe LocalDateTime.of(2026, 6, 19, 12, 0)
    }

    @Test
    fun `converts a UTC timestamp into CET across the winter offset too`() {
        // Arrange — January, so CET is one hour ahead of UTC, not two. A fixed
        // offset would pass the summer case and fail this one.
        CdnDateTime.parse("2026-01-15T12:00:00Z").inCet shouldBe LocalDateTime.of(2026, 1, 15, 13, 0)
    }

    @Test
    fun `converts an explicit offset into CET`() {
        // Arrange — the desktop's "local kind" branch: an offset the machine
        // would have resolved against its own zone first. Converting directly
        // gives the same instant, and therefore the same CET time.
        val parsed = CdnDateTime.parse("2026-06-19T12:00:00-05:00")

        // Assert
        parsed.inCet shouldBe OffsetDateTime.parse("2026-06-19T12:00:00-05:00")
            .atZoneSameInstant(CdnDateTime.CET)
            .toLocalDateTime()
        parsed.inCet shouldBe LocalDateTime.of(2026, 6, 19, 19, 0)
    }

    @Test
    fun `does not fall back to the device time zone`() {
        // Arrange — the whole point of the rule: a phone in Auckland must read
        // an expiry the same way a phone in Madrid does.
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
        try {
            // Act / Assert
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
        // Arrange — a bad date must not be coerced away, because the content
        // service answers an unreadable payload by keeping the last good one.
        val json = """{"news":[{"date":"nope"}],"notifications":[]}"""

        // Act / Assert
        shouldThrow<Exception> { Json.decodeFromString<HomeContentDto>(json) }
    }
}
