package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.service.cdn.dto.CdnDateTime
import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import dev.jagoba.lostielauncher.service.cdn.dto.NewsItemDto
import dev.jagoba.lostielauncher.service.cdn.dto.NotificationItemDto
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * The language-resolution and expiry cases of the desktop's
 * `Services/ContentServiceTests.cs`, against the pure functions that took over
 * from `ContentService.Resolve` and its inline expiry filter.
 */
@DisplayName("CdnMappers")
class CdnMappersTest {
    private val cdnBase = "https://content.test"

    /** 19 June 2026, 12:00 UTC — 14:00 in CET, since June is summer time. */
    private val clock: Clock = Clock.fixed(Instant.parse("2026-06-19T12:00:00Z"), ZoneOffset.UTC)

    // ---- resolveLocalized ----

    @Test
    fun `returns the requested language when it is there`() {
        val localized = mapOf("es" to "Hola", "en" to "Hi", "fr" to "Salut")

        CdnMappers.resolveLocalized(localized, "fr") shouldBe "Salut"
    }

    @Test
    fun `falls back to Spanish when the requested language is missing`() {
        val localized = mapOf("en" to "Hi", "es" to "Hola")

        CdnMappers.resolveLocalized(localized, "ja") shouldBe "Hola"
    }

    @Test
    fun `falls back to English when neither the requested language nor Spanish is there`() {
        val localized = mapOf("fr" to "Salut", "en" to "Hi")

        CdnMappers.resolveLocalized(localized, "ja") shouldBe "Hi"
    }

    @Test
    fun `picks the first key in ordinal order when no preferred language is there`() {
        // Arrange — inserted out of order on purpose: the answer must come from
        // the key order, not the insertion order, or two runs could disagree.
        val localized = linkedMapOf("pt" to "Ola", "ca" to "Hola", "gl" to "Ola")

        CdnMappers.resolveLocalized(localized, "ja") shouldBe "Hola"
    }

    @Test
    fun `resolves an empty map to an empty string`() {
        CdnMappers.resolveLocalized(emptyMap(), "es") shouldBe ""
    }

    @Test
    fun `resolves a missing map to an empty string`() {
        CdnMappers.resolveLocalized(null, "es") shouldBe ""
    }

    @Test
    fun `skips a null value and falls back rather than returning it`() {
        val localized = mapOf("es" to null, "en" to "Hi")

        CdnMappers.resolveLocalized(localized, "es") shouldBe "Hi"
    }

    @Test
    fun `resolves a map whose values are all null to an empty string`() {
        val localized = mapOf("es" to null, "en" to null)

        CdnMappers.resolveLocalized(localized, "es") shouldBe ""
    }

    @Test
    fun `resolves Valencian by its three-letter code`() {
        // Arrange — not a desktop case. `val` is the one code that is not two
        // letters, which makes it the one easiest to get wrong.
        val localized = mapOf("ca" to "catala", "val" to "valencia")

        CdnMappers.resolveLocalized(localized, AppLanguage.VAL.code) shouldBe "valencia"
    }

    // ---- expiry ----

    @Test
    fun `keeps an item with no expiry`() {
        CdnMappers.isCurrent(null, LocalDateTime.of(2026, 6, 19, 14, 0)) shouldBe true
    }

    @Test
    fun `drops an item expiring exactly now`() {
        // The comparison is strictly greater than, so "now" is already expired.
        val now = LocalDateTime.of(2026, 6, 19, 14, 0)

        CdnMappers.isCurrent(CdnDateTime.parse("2026-06-19T14:00:00"), now) shouldBe false
    }

    @Test
    fun `keeps an item expiring one minute from now`() {
        val now = LocalDateTime.of(2026, 6, 19, 14, 0)

        CdnMappers.isCurrent(CdnDateTime.parse("2026-06-19T14:01:00"), now) shouldBe true
    }

    @Test
    fun `drops an expired item and keeps a current one`() {
        // Arrange — the desktop's `GetHomeContentAsync_FiltersOutExpiredItems`.
        val dto = HomeContentDto(
            news = listOf(
                newsDto(title = "Vieja", expiresAt = "2026-06-18T00:00:00"),
                newsDto(title = "Actual", expiresAt = null),
            ),
        )

        // Act
        val content = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false)

        // Assert
        content.news shouldHaveSize 1
        content.news[0].title shouldBe "Actual"
    }

    @Test
    fun `measures expiry in CET, not in the device zone and not in UTC`() {
        // Arrange — the clock is pinned to 12:00 UTC, which is 14:00 CET. An
        // item expiring at 13:00 with no offset is a CET wall-clock time, so it
        // is already an hour past. Comparing the same 13:00 against UTC now
        // would keep it, which is exactly the mistake this case catches.
        val dto = HomeContentDto(news = listOf(newsDto(title = "Borderline", expiresAt = "2026-06-19T13:00:00")))

        // Act
        val content = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false)

        // Assert
        content.news.shouldBeEmpty()
    }

    @Test
    fun `applies a UTC expiry by converting it to CET first`() {
        // Arrange — 12:30 UTC is 14:30 CET, half an hour after the pinned now.
        val dto = HomeContentDto(news = listOf(newsDto(title = "Future", expiresAt = "2026-06-19T12:30:00Z")))

        // Act
        val content = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false)

        // Assert
        content.news shouldHaveSize 1
    }

    @Test
    fun `drops a UTC expiry that is already in the past`() {
        val dto = HomeContentDto(news = listOf(newsDto(title = "Past", expiresAt = "2026-06-18T12:30:00Z")))

        CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false).news.shouldBeEmpty()
    }

    // ---- projection ----

    @Test
    fun `skips a null array element instead of failing the payload`() {
        val dto = HomeContentDto(news = listOf(null, newsDto(title = "Demo")), notifications = listOf(null))

        val content = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false)

        content.news shouldHaveSize 1
        content.notifications.shouldBeEmpty()
    }

    @Test
    fun `carries the staleness flag straight through`() {
        CdnMappers.toDomain(HomeContentDto(), AppLanguage.ESP, clock, isStale = true).isStale shouldBe true
    }

    @Test
    fun `keeps the date as published rather than as converted`() {
        // The date is rendered, not compared, so it must survive untouched.
        val dto = HomeContentDto(news = listOf(newsDto(title = "Demo", date = "2026-07-24T00:00:00")))

        val news = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false).news.single()

        news.date shouldBe LocalDateTime.of(2026, 7, 24, 0, 0)
    }

    @Test
    fun `projects a notification with its severity and its message`() {
        val dto = HomeContentDto(
            notifications = listOf(
                NotificationItemDto(
                    id = "11111111-1111-1111-1111-111111111111",
                    title = mapOf("es" to "Aviso"),
                    message = mapOf("es" to "Texto"),
                    date = CdnDateTime.parse("2026-07-24T00:00:00"),
                ),
            ),
        )

        val notification = CdnMappers.toDomain(dto, AppLanguage.ESP, clock, isStale = false).notifications.single()

        notification.title shouldBe "Aviso"
        notification.message shouldBe "Texto"
        notification.id shouldBe UUID.fromString("11111111-1111-1111-1111-111111111111")
    }

    // ---- catalogue ----

    @Test
    fun `resolves the logo against the injected CDN base`() {
        val game = CdnMappers.toDomain(GameDto(logo = "/logos/x.png"), cdnBase)

        game.logoUrl shouldBe "https://content.test/logos/x.png"
    }

    @Test
    fun `leaves the logo null when the entry ships none`() {
        CdnMappers.toDomain(GameDto(logo = ""), cdnBase).logoUrl.shouldBeNull()
    }

    @Test
    fun `reads an absent id as no id`() {
        CdnMappers.toDomain(GameDto(), cdnBase).id.shouldBeNull()
    }

    @Test
    fun `reads the all-zero GUID as no id`() {
        // The desktop's `Guid.Empty`, which it takes to mean "match by name".
        CdnMappers.toDomain(GameDto(id = "00000000-0000-0000-0000-000000000000"), cdnBase).id.shouldBeNull()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "not-a-guid",
            "1-2-3-4-5",
            "1111111122223333444455555555-5-5-5-5",
            "11111111-2222-3333-4444-55555555555G",
            // These three are accepted by `Guid.TryParse`, and so by
            // `SpecialVersionConfig.parse`, but *not* by `System.Text.Json`,
            // which is what reads the catalogue on the desktop. The two parsers
            // differ on purpose and this is where the difference shows.
            "11111111222233334444555555555555",
            "{11111111-2222-3333-4444-555555555555}",
            "(11111111-2222-3333-4444-555555555555)",
        ],
    )
    fun `rejects an id the catalogue's own reader would reject`(id: String) {
        // The caller turns this into an empty catalogue, which is what the
        // desktop does when the same value fails deserialization.
        shouldThrow<IllegalArgumentException> { CdnMappers.toDomain(GameDto(id = id), cdnBase) }
    }

    @Test
    fun `maps every wire field onto its domain counterpart`() {
        val dto = GameDto(
            id = "6b49940d-5910-49e5-aab8-f933cd51c388",
            nombre = "Pokemon Anil",
            version = "v4.13.0",
            pesoGB = 0.5889,
            descripcion = "Remake",
            url = "https://example.test/anil",
            logo = "/public/imgs/4.png",
            rutaRelativa = "/pokemon-anil/4-13-0.zip",
            sha256 = "e379a48072c42942fd4f3f99b6aed0c4d1aff3d1f691448d65af76e035e17ac5",
        )

        val game = CdnMappers.toDomain(dto, cdnBase)

        game.id shouldBe UUID.fromString("6b49940d-5910-49e5-aab8-f933cd51c388")
        game.name shouldBe "Pokemon Anil"
        game.version shouldBe "v4.13.0"
        game.sizeGb shouldBe 0.5889
        game.description shouldBe "Remake"
        game.pageUrl shouldBe "https://example.test/anil"
        game.logoUrl shouldBe "https://content.test/public/imgs/4.png"
        game.relativePath shouldBe "/pokemon-anil/4-13-0.zip"
        game.sha256 shouldBe "e379a48072c42942fd4f3f99b6aed0c4d1aff3d1f691448d65af76e035e17ac5"
    }

    private fun newsDto(title: String, date: String = "2026-01-01T00:00:00", expiresAt: String? = null) = NewsItemDto(
        id = "11111111-1111-1111-1111-111111111111",
        title = mapOf("es" to title),
        description = mapOf("es" to "."),
        tag = "x",
        date = CdnDateTime.parse(date),
        expiresAt = expiresAt?.let { CdnDateTime.parse(it) },
    )
}
