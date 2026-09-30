package dev.jagoba.lostielauncher.service.cdn.dto

import dev.jagoba.lostielauncher.model.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class HomeContentDto(
    val news: List<NewsItemDto?> = emptyList(),
    val notifications: List<NotificationItemDto?> = emptyList(),
)

@Serializable
internal data class NewsItemDto(
    val id: String = "",
    val title: Map<String, String?>? = null,
    val description: Map<String, String?>? = null,
    val tag: String = "",
    val date: CdnDateTime = CdnDateTime.MIN_VALUE,
    @SerialName("expires_at") val expiresAt: CdnDateTime? = null,
)

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
