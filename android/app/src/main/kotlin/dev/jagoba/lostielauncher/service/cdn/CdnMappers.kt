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

internal object CdnMappers {
    private val EMPTY_UUID: UUID = UUID(0L, 0L)

    private val HYPHENATED_UUID = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

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

    fun isCurrent(expiresAt: CdnDateTime?, nowCet: LocalDateTime): Boolean =
        expiresAt == null || expiresAt.inCet > nowCet

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

    private fun parseOptionalUuid(value: String): UUID? {
        if (value.isBlank()) return null
        require(HYPHENATED_UUID.matches(value)) { "Not a GUID: '$value'." }
        val parsed = UUID.fromString(value)
        return if (parsed == EMPTY_UUID) null else parsed
    }
}
