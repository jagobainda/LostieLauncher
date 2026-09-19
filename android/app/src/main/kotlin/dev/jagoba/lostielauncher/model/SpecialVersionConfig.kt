package dev.jagoba.lostielauncher.model

import java.util.Locale
import java.util.UUID

/**
 * The `game.config` that describes a special (seasonal) build of a game.
 *
 * It is served from `<downloadBase>/<key>/game.config` and it is **not** JSON:
 * it is a plain-text key/value file. [parse] is the whole format, and it is
 * strict — all five keys are required, because a half-read config would point a
 * download at the wrong archive.
 *
 * The fetch itself belongs to the download service, which arrives with port
 * plan step 08; the format and its parser live here because they are part of
 * the data model (`spec/04-data-model.md`).
 *
 * [type] is a label rendered verbatim as a badge on the card. It is not
 * localized.
 */
data class SpecialVersionConfig(
    val sha256: String,
    val type: String,
    val mainGameId: UUID,
    val version: String,
    val fileName: String,
) {
    companion object {
        private const val KEY_SHA256 = "sha256"
        private const val KEY_TYPE = "tipo"
        private const val KEY_MAIN_GAME = "juego-principal"
        private const val KEY_VERSION = "vers"
        private const val KEY_FILE = "archivo"

        /**
         * Parses the file, or returns `null` if it cannot be parsed in full.
         *
         * Exactly the desktop's rules, and each one matters:
         * - split on `\n`, drop empty entries, trim every line (so a CRLF file
         *   parses, the `\r` being trimmed off the end);
         * - split each line at the **first** `=`; a line with no `=`, or with
         *   `=` at index 0, is skipped, which is what lets the file carry
         *   comments;
         * - keys are compared case-insensitively in [Locale.ROOT], matching
         *   .NET's culture-invariant `OrdinalIgnoreCase`, and a repeated key
         *   wins with
         *   its **last** occurrence;
         * - all five keys are required, and `juego-principal` must be a GUID.
         */
        fun parse(content: String): SpecialVersionConfig? {
            val values = mutableMapOf<String, String>()
            for (rawLine in content.split('\n')) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue
                val separator = line.indexOf('=')
                if (separator <= 0) continue
                values[line.substring(0, separator).trim().lowercase(Locale.ROOT)] =
                    line.substring(separator + 1).trim()
            }

            val sha256 = values[KEY_SHA256] ?: return null
            val type = values[KEY_TYPE] ?: return null
            val mainGame = values[KEY_MAIN_GAME] ?: return null
            val version = values[KEY_VERSION] ?: return null
            val fileName = values[KEY_FILE] ?: return null

            val mainGameId = parseUuidOrNull(mainGame) ?: return null

            return SpecialVersionConfig(
                sha256 = sha256,
                type = type,
                mainGameId = mainGameId,
                version = version,
                fileName = fileName,
            )
        }

        /**
         * Parses a GUID the way `Guid.TryParse` does, which is not the way
         * either `UUID.fromString` or the catalogue does.
         *
         * Three parsers, three behaviours, and the differences are load-bearing:
         *
         * - `Guid.TryParse`, which is what the desktop calls here, accepts the
         *   hyphenated form, 32 bare hex digits, and the hyphenated form wrapped
         *   in `{}` or `()`. A `game.config` written in any of those works on
         *   Windows, so it has to work here too.
         * - `UUID.fromString` is laxer still in one direction — it accepts short
         *   groups, so `1-2-3-4-5` parses — and stricter in another, since it
         *   knows nothing about the wrapped forms. Neither is wanted, so the
         *   text is normalised and validated before it ever reaches it.
         * - The catalogue is read by `System.Text.Json` on the desktop, which
         *   accepts **only** the hyphenated form. That is why
         *   `CdnMappers.parseOptionalUuid` is deliberately stricter than this
         *   and must not be unified with it.
         *
         * The one `Guid.TryParse` form left out is `X`
         * (`{0x…,0x…,0x…,{0x…}}`): it is what .NET emits, never what a human
         * writes into a config file by hand, and supporting it approximately
         * would be worse than not supporting it.
         */
        private fun parseUuidOrNull(value: String): UUID? {
            val text = value.trim()
            // Only the hyphenated form may be wrapped: .NET rejects `{32 digits}`,
            // and unwrapping here only ever yields a candidate of that length.
            val wrapped = text.length == HYPHENATED_LENGTH + 2 &&
                ((text.first() == '{' && text.last() == '}') || (text.first() == '(' && text.last() == ')'))
            val body = if (wrapped) text.substring(1, text.length - 1) else text

            val digits = when (body.length) {
                HYPHENATED_LENGTH -> {
                    if (body[8] != '-' || body[13] != '-' || body[18] != '-' || body[23] != '-') return null
                    body.replace("-", "")
                }

                COMPACT_LENGTH -> body

                else -> return null
            }

            if (!HEX_32.matches(digits)) return null
            return UUID.fromString(
                "${digits.substring(0, 8)}-${digits.substring(8, 12)}-${digits.substring(12, 16)}-" +
                    "${digits.substring(16, 20)}-${digits.substring(20)}",
            )
        }

        private val HEX_32 = Regex("[0-9a-fA-F]{32}")

        /** `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`. */
        private const val HYPHENATED_LENGTH = 36

        /** The same value with the hyphens taken out. */
        private const val COMPACT_LENGTH = 32
    }
}
