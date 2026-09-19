package dev.jagoba.lostielauncher.service.cdn

import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.GameInfo
import dev.jagoba.lostielauncher.model.HomeContent
import dev.jagoba.lostielauncher.model.NewsItem
import dev.jagoba.lostielauncher.model.NotificationItem
import dev.jagoba.lostielauncher.service.cdn.dto.CdnDateTime
import dev.jagoba.lostielauncher.service.cdn.dto.GameDto
import dev.jagoba.lostielauncher.service.cdn.dto.HomeContentDto
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * Wire to domain.
 *
 * These are pure functions on purpose: everything about the remote format that
 * is a *decision* — which language wins, when an item has expired, what counts
 * as "no id" — lives here and is tested directly, leaving the service with
 * nothing but the network and the cache.
 */
internal object CdnMappers {
    /** The all-zero GUID. The desktop's `Guid.Empty`, which it reads as "no id". */
    private val EMPTY_UUID: UUID = UUID(0L, 0L)

    /** `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`, the only form this payload may use. */
    private val HYPHENATED_UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

    /**
     * Maps one catalogue entry.
     *
     * @param cdnBaseUrl prepended to `logo`, which is published as a path with
     *   a leading slash. The model may not know the CDN origin, so the absolute
     *   URL is resolved here.
     * @throws IllegalArgumentException if `id` is present but is not a GUID. The
     *   caller degrades to an empty catalogue, which is what the desktop does
     *   when the same value fails deserialization.
     */
    fun toDomain(dto: GameDto, cdnBaseUrl: String): GameInfo = GameInfo(
        id = parseOptionalUuid(dto.id),
        name = dto.nombre,
        version = dto.version,
        sizeGb = dto.pesoGB,
        description = dto.descripcion,
        pageUrl = dto.url,
        logoUrl = if (dto.logo.isEmpty()) null else cdnBaseUrl + dto.logo,
        relativePath = dto.rutaRelativa,
        sha256 = dto.sha256,
    )

    /**
     * Projects a cached payload onto one language and one moment.
     *
     * Two things happen here and they happen in this order: items that have
     * expired are dropped, and the survivors have their localized fields
     * resolved. Items keep their payload order; `date` orders nothing.
     *
     * @param clock the source of "now". Injected because expiry is the one
     *   place this layer reads a wall clock, and a test that cannot pin it is a
     *   test that starts failing on a date nobody chose.
     */
    fun toDomain(dto: HomeContentDto, language: AppLanguage, clock: Clock, isStale: Boolean): HomeContent {
        val nowCet = LocalDateTime.ofInstant(clock.instant(), CdnDateTime.CET)
        val code = language.code

        val news = dto.news
            .filterNotNull()
            .filter { isCurrent(it.expiresAt, nowCet) }
            .map {
                NewsItem(
                    id = parseOptionalUuid(it.id),
                    title = resolveLocalized(it.title, code),
                    description = resolveLocalized(it.description, code),
                    tag = it.tag,
                    date = it.date.asWritten,
                    expiresAt = it.expiresAt?.asWritten,
                )
            }

        val notifications = dto.notifications
            .filterNotNull()
            .filter { isCurrent(it.expiresAt, nowCet) }
            .map {
                NotificationItem(
                    id = parseOptionalUuid(it.id),
                    title = resolveLocalized(it.title, code),
                    message = resolveLocalized(it.message, code),
                    type = it.type,
                    date = it.date.asWritten,
                    expiresAt = it.expiresAt?.asWritten,
                )
            }

        return HomeContent(news = news, notifications = notifications, isStale = isStale)
    }

    /**
     * Whether an item is still current at [nowCet].
     *
     * The comparison is strictly greater than, so an item expiring exactly now
     * is dropped, and a missing `expires_at` never expires.
     */
    fun isCurrent(expiresAt: CdnDateTime?, nowCet: LocalDateTime): Boolean =
        expiresAt == null || expiresAt.inCet > nowCet

    /**
     * Picks one language out of a localized field.
     *
     * The order is fixed and it is the point of the function: the requested
     * code, then Spanish, then English, then the first non-null value in
     * **ordinal key order**, then the empty string. A missing translation
     * therefore never blanks an item, and two runs never disagree about which
     * fallback they picked.
     *
     * A null map — a field the publisher omitted entirely — resolves to the
     * empty string.
     */
    fun resolveLocalized(localized: Map<String, String?>?, languageCode: String): String {
        if (localized == null) return ""
        localized[languageCode]?.let { return it }
        localized[AppLanguage.ESP.code]?.let { return it }
        localized[AppLanguage.ENG.code]?.let { return it }
        return localized.entries
            .sortedBy { it.key }
            .firstNotNullOfOrNull { it.value }
            ?: ""
    }

    /**
     * Reads an id that may not be there.
     *
     * Blank and the all-zero GUID both mean "no id", the way the desktop reads
     * an absent `id` and `Guid.Empty`. Anything else must be a GUID.
     *
     * Only the hyphenated form is accepted, and that is stricter than it looks
     * on purpose. The catalogue is read by `System.Text.Json` on the desktop,
     * which accepts nothing else — not the 32-digit form, not a braced one —
     * whereas `Guid.TryParse` accepts all of them. That is why
     * `SpecialVersionConfig.parse`, which the desktop reads with `Guid.TryParse`,
     * is deliberately laxer than this. `UUID.fromString` on its own is no use to
     * either: it happily parses `1-2-3-4-5`.
     *
     * One deliberate difference: an explicit `"id": ""` fails the payload on the
     * desktop, because `Guid` cannot parse it, while here it is read as no id —
     * the same thing the desktop concludes when the field is simply absent.
     */
    private fun parseOptionalUuid(value: String): UUID? {
        if (value.isBlank()) return null
        require(HYPHENATED_UUID.matches(value)) { "Not a GUID: '$value'." }
        val parsed = UUID.fromString(value)
        return if (parsed == EMPTY_UUID) null else parsed
    }
}
