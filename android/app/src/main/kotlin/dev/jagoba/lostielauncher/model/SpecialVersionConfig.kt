package dev.jagoba.lostielauncher.model

import java.util.Locale
import java.util.UUID

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

        private fun parseUuidOrNull(value: String): UUID? {
            val text = value.trim()
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

        private const val HYPHENATED_LENGTH = 36

        private const val COMPACT_LENGTH = 32
    }
}
