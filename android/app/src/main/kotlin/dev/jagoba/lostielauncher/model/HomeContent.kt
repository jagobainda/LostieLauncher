package dev.jagoba.lostielauncher.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Everything the home screen shows, already resolved to one language and
 * already filtered of expired items.
 *
 * [isStale] is the whole reason this type carries a flag instead of being a
 * bare pair of lists: when the CDN cannot be reached the launcher keeps serving
 * the last content it saw and says so, rather than showing an empty screen.
 * With no cache at all the lists are empty *and* the flag is set, which is
 * "content unavailable" rather than "no content".
 */
data class HomeContent(
    val news: List<NewsItem> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val isStale: Boolean = false,
)

/**
 * One news entry, resolved to the requested language.
 *
 * [date] and [expiresAt] are wall-clock times without an offset: the publisher
 * writes them in Central European Time. [expiresAt] has already been applied by
 * the time an item reaches here — it is kept because the UI shows it and
 * because dropping it would make the model lossy.
 */
data class NewsItem(
    val id: UUID?,
    val title: String,
    val description: String,
    val tag: String,
    val date: LocalDateTime,
    val expiresAt: LocalDateTime?,
)

/** One notification, resolved to the requested language. See [NewsItem] on the dates. */
data class NotificationItem(
    val id: UUID?,
    val title: String,
    val message: String,
    val type: NotificationType,
    val date: LocalDateTime,
    val expiresAt: LocalDateTime?,
)
