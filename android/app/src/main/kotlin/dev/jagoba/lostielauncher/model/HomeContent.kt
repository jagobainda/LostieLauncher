package dev.jagoba.lostielauncher.model

import java.time.LocalDateTime
import java.util.UUID

data class HomeContent(
    val news: List<NewsItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val isStale: Boolean = false,
)

data class NewsItem(
    val id: UUID?,
    val title: String,
    val description: String,
    val tag: String,
    val date: LocalDateTime,
    val expiresAt: LocalDateTime?,
)

data class NotificationItem(
    val id: UUID?,
    val title: String,
    val message: String,
    val type: NotificationType,
    val date: LocalDateTime,
    val expiresAt: LocalDateTime?,
)
