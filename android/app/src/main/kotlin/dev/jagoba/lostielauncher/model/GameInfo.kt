package dev.jagoba.lostielauncher.model

import java.util.Locale
import java.util.UUID

/**
 * One entry of the remote game catalogue, as the rest of the app sees it.
 *
 * Immutable on purpose: the desktop's `GameInfo` doubles as an observable
 * view-model row and carries the live download status on the same object.
 * `spec/04-data-model.md` explicitly allows an Android port to hold that
 * transient state beside the model instead, and that is what later steps do —
 * nothing here changes while a transfer runs.
 *
 * The wire field names are Spanish; these are their English counterparts. The
 * mapping between the two is in `service/cdn/CdnMappers.kt`, which is also
 * where [logoUrl] is resolved against the injected CDN base — a model may not
 * know a URL.
 */
data class GameInfo(
    /**
     * The catalogue identity, or `null` when the entry has none.
     *
     * Old catalogue data ships either no `id` at all or the all-zero GUID; the
     * desktop treats both as "no id" and falls back to matching by [name], so
     * both arrive here as `null`.
     */
    val id: UUID?,
    /** The display name, and the folder name the game is installed into. */
    val name: String,
    /** A `v`-prefixed version, for example `v4.13.0`. */
    val version: String,
    /** Download size in gigabytes, fractional. */
    val sizeGb: Double,
    val description: String,
    /** A human page about the game, opened from the download dialog. May be empty. */
    val pageUrl: String,
    /** The absolute logo URL, or `null` when the entry ships no logo. */
    val logoUrl: String?,
    /** The ZIP path relative to the download base, leading slash included. */
    val relativePath: String,
    /** 64 hex characters. Anything else fails verification. */
    val sha256: String,
) {
    /**
     * The slug identity, the desktop's `GameInfo.GameId`.
     *
     * Lowercase the name, collapse every run of characters outside `[a-z0-9]`
     * into a single `-`, then trim dashes from both ends: `Pokémon Añil`
     * becomes `pok-mon-a-il`. It is what names a download session and a cache
     * file, so renaming a game remotely changes its cache identity and orphans
     * the files it had — by design on the desktop, and reproduced here.
     *
     * Not [id]: that is the catalogue GUID, and it can be absent.
     */
    val gameId: String
        get() = SLUG_SEPARATORS.replace(name.lowercase(Locale.ROOT), "-").trim('-')

    /**
     * The size as it is shown on a card: `1.5 GB` at or above one gigabyte,
     * `256 MB` below it.
     *
     * Formatted in [Locale.ROOT] so the decimal separator never follows the
     * device locale — the desktop pins the invariant culture here for the same
     * reason.
     */
    val formattedSize: String
        get() = if (sizeGb >= 1) {
            trimTrailingZero(String.format(Locale.ROOT, "%.1f", sizeGb)) + " GB"
        } else {
            String.format(Locale.ROOT, "%.0f", sizeGb * BYTES_PER_KIB) + " MB"
        }

    private companion object {
        val SLUG_SEPARATORS = Regex("[^a-z0-9]+")

        /** `pesoGB` is gigabytes; the desktop's MB fallback multiplies by 1024, not 1000. */
        const val BYTES_PER_KIB = 1024

        /** `0.#` in .NET drops a trailing `.0`; `%.1f` does not, so it is dropped here. */
        fun trimTrailingZero(value: String) = value.removeSuffix(".0")
    }
}
