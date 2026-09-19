package dev.jagoba.lostielauncher.model

/**
 * Where game archives come from.
 *
 * The desktop's `Models/DownloadOptions.cs`. Two URL shapes are built from
 * [baseUrl], both by the caller rather than inside the download service:
 * - a regular download is [baseUrl] plus the catalogue entry's `rutaRelativa`,
 *   which already begins with a slash;
 * - a special version is `<baseUrl>/<key>/<fileName>`, and its configuration is
 *   at `<baseUrl>/<key>/game.config`.
 *
 * The transfer that consumes this arrives with port plan step 08. The record is
 * here because this step owns the endpoint definitions.
 */
data class DownloadOptions(
    /** The download origin, without a trailing slash. */
    val baseUrl: String,
)
