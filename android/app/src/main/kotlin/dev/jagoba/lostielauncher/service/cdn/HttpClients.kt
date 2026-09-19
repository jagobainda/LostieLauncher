package dev.jagoba.lostielauncher.service.cdn

import javax.inject.Qualifier

/*
 * The three HTTP clients, told apart.
 *
 * They differ on purpose and spec/03-services.md explains why; using one client
 * for all three would break two of the three behaviours. The qualifiers live
 * beside the code that consumes them rather than in the Hilt module, so the
 * service layer does not have to reach into the composition root to name what
 * it needs.
 */

/**
 * Small JSON over a ten-second timeout.
 *
 * A slow CDN should fall back to the cache quickly rather than hold a screen
 * empty while it thinks about it.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ContentClient

/**
 * The maintenance flag, over a three-second timeout.
 *
 * This check gates every server-backed action, so it must never be the thing
 * the user waits on. Timing out means "not blocked".
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SecurityFlagClient

/**
 * Game archives: no read timeout at all, and a twenty-second connect timeout.
 *
 * A multi-gigabyte transfer must not be killed by a wall clock, but a dead host
 * must still fail fast before any bytes move. A stalled transfer is caught by
 * an inactivity watchdog instead, which arrives with port plan step 08 along
 * with the only consumer of this client.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DownloadClient
