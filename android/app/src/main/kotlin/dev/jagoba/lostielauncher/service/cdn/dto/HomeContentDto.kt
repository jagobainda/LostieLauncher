package dev.jagoba.lostielauncher.service.cdn.dto

import dev.jagoba.lostielauncher.model.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `homepage-notifications.json`, exactly as the CDN writes it.
 *
 * This is what the content service caches: the raw payload, before any
 * language has been chosen and before anything has been filtered. Projecting it
 * per call is what lets the language change without a refetch.
 *
 * The list elements are nullable although the real payload has no nulls. The
 * desktop drops a stray null element rather than failing the payload over it,
 * and a non-nullable element type here would fail it.
 */
@Serializable
internal data class HomeContentDto(
    val news: List<NewsItemDto?> = emptyList(),
    val notifications: List<NotificationItemDto?> = emptyList(),
)

/**
 * One news entry on the wire.
 *
 * [title] and [description] are **maps**, not strings: one entry per language
 * code. The values are nullable because a partially translated item ships an
 * explicit null, and the resolver's job is to fall back rather than blank the
 * item.
 *
 * `expires_at` is the one field the payload writes in snake_case; everything
 * else is camelCase.
 */
@Serializable
internal data class NewsItemDto(
    val id: String = "",
    val title: Map<String, String?>? = null,
    val description: Map<String, String?>? = null,
    val tag: String = "",
    val date: CdnDateTime = CdnDateTime.MIN_VALUE,
    @SerialName("expires_at") val expiresAt: CdnDateTime? = null,
)

/** One notification on the wire. See [NewsItemDto] for the localized maps and the date fields. */
@Serializable
internal data class NotificationItemDto(
    val id: String = "",
    val title: Map<String, String?>? = null,
    val message: Map<String, String?>? = null,
    @Serializable(with = NotificationTypeSerializer::class)
    val type: NotificationType = NotificationType.INFO,
    val date: CdnDateTime = CdnDateTime.MIN_VALUE,
    @SerialName("expires_at") val expiresAt: CdnDateTime? = null,
)
