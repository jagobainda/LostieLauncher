package dev.jagoba.lostielauncher.model

import kotlin.time.Duration

/**
 * Where the content service reads from, and how long it is allowed to trust the
 * maintenance flag.
 *
 * The desktop's `Models/ContentOptions.cs`, plus two values the desktop keeps
 * as constants inside the service. They are configuration here so that no URL
 * and no timing number is written inside the type that does the work, and so a
 * test can pin them.
 */
data class ContentOptions(
    /**
     * The CDN origin, without a trailing slash. Catalogue entries carry paths
     * relative to it — `logo` and `rutaRelativa` both start with a slash — so
     * this is what turns them into absolute URLs.
     */
    val cdnBaseUrl: String,
    /** The game catalogue: a bare JSON array. */
    val catalogueUrl: String,
    /** News and notifications: an object with two arrays. Note the different host. */
    val homeContentUrl: String,
    /**
     * The maintenance kill switch. Its **existence** is the signal: any 2xx
     * means server-backed actions are blocked, anything else means they are
     * not. The body is never read.
     */
    val maintenanceFlagUrl: String,
    /**
     * How long a maintenance-flag answer is reused before probing again.
     *
     * Short on purpose: the flag gates every server-backed action, so the check
     * must never be what the user waits on, but it also must not go stale
     * enough to keep a download running after maintenance starts.
     */
    val maintenanceFlagCacheDuration: Duration,
)
