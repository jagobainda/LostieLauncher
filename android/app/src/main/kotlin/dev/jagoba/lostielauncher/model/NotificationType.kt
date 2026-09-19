package dev.jagoba.lostielauncher.model

/**
 * The severity of a home-screen notification.
 *
 * Serialized by name on the wire (`"Info"`, `"Warning"`, `"Exclamation"`), not
 * as an ordinal — see `NotificationTypeSerializer`. Only `Info` has ever
 * appeared in a captured payload.
 */
enum class NotificationType {
    INFO,
    WARNING,
    EXCLAMATION,
}
