package dev.jagoba.lostielauncher.model

/**
 * Everything a download needs to know about what it is fetching.
 *
 * The desktop's `Models/GameDownloadArgs.cs`. [key] is set only for a special
 * version: it is part of the URL and part of the cache token, which is what
 * keeps a keyed build and the standard one of the same version from writing
 * over each other's partial file.
 *
 * The transfer that consumes this arrives with port plan step 08. It is here
 * because the cache-naming decisions in
 * [dev.jagoba.lostielauncher.util.download.DownloadPathUtils] take it.
 */
data class GameDownloadArgs(
    /** The catalogue slug, `GameInfo.gameId`. Names the cached archive. */
    val gameId: String,
    /** The version being fetched, as the catalogue writes it. */
    val version: String,
    /** The archive path relative to the download base, leading slash included. */
    val relativePath: String,
    /** The special-version key, or `null` for a standard download. */
    val key: String? = null,
)
