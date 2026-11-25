package dev.jagoba.lostielauncher.util.version

import java.util.Locale

internal data class ParsedVersion(val base: BaseVersion, val prerelease: List<String>?) : Comparable<ParsedVersion> {
    override fun compareTo(other: ParsedVersion): Int {
        val baseComparison = base.compareTo(other.base)
        if (baseComparison != 0) return baseComparison

        val left = prerelease
        val right = other.prerelease
        return when {
            left == null && right == null -> 0
            left == null -> 1
            right == null -> -1
            else -> comparePrereleases(left, right)
        }
    }

    private companion object {
        fun comparePrereleases(left: List<String>, right: List<String>): Int {
            val shared = minOf(left.size, right.size)
            for (index in 0 until shared) {
                val diff = compareIdentifiers(left[index], right[index])
                if (diff != 0) return diff
            }
            return left.size.compareTo(right.size)
        }

        fun compareIdentifiers(left: String, right: String): Int {
            val leftTokens = tokenize(left)
            val rightTokens = tokenize(right)
            val shared = minOf(leftTokens.size, rightTokens.size)
            for (index in 0 until shared) {
                val diff = compareTokens(leftTokens[index], rightTokens[index])
                if (diff != 0) return diff
            }
            return leftTokens.size.compareTo(rightTokens.size)
        }

        fun compareTokens(left: String, right: String): Int {
            val leftNumeric = left[0].isDigit()
            val rightNumeric = right[0].isDigit()
            return when {
                leftNumeric && rightNumeric -> compareUnsignedDigits(left, right)
                leftNumeric != rightNumeric -> if (leftNumeric) -1 else 1
                else -> left.compareTo(right)
            }
        }

        fun compareUnsignedDigits(left: String, right: String): Int {
            val a = left.trimStart('0')
            val b = right.trimStart('0')
            val lengthDiff = a.length.compareTo(b.length)
            return if (lengthDiff != 0) lengthDiff else a.compareTo(b)
        }

        fun tokenize(identifier: String): List<String> {
            val tokens = mutableListOf<String>()
            var start = 0
            while (start < identifier.length) {
                val isDigit = identifier[start].isDigit()
                var end = start + 1
                while (end < identifier.length && identifier[end].isDigit() == isDigit) {
                    end++
                }
                tokens += identifier.substring(start, end)
                start = end
            }
            return tokens
        }
    }
}

object VersionUtils {
    private const val UNKNOWN_VERSION = "unknown"

    fun isNewerVersion(remoteVersion: String?, localVersion: String?): Boolean {
        val remote = parseVersion(remoteVersion) ?: return false
        val local = parseVersion(localVersion) ?: return false
        return remote > local
    }

    fun formatDisplayVersion(version: String?): String {
        if (version.isNullOrBlank()) return UNKNOWN_VERSION

        val normalized = version.trim().trimStart('v', 'V')
        return if (normalized.isEmpty()) UNKNOWN_VERSION else "v$normalized"
    }

    internal fun isValidVersion(version: String?): Boolean = parseVersion(version) != null

    internal fun parseVersion(version: String?): ParsedVersion? {
        if (version.isNullOrBlank()) return null

        val stripped = version.trim().trimStart('v', 'V')
        val dashIndex = stripped.indexOf('-')
        if (dashIndex < 0) {
            val base = BaseVersion.parse(stripped) ?: return null
            return ParsedVersion(base, null)
        }

        val base = BaseVersion.parse(stripped.substring(0, dashIndex)) ?: return null
        val suffix = stripped.substring(dashIndex + 1)
        if (suffix.isEmpty()) return null

        val identifiers = suffix.split('.')
        if (identifiers.any { it.isEmpty() }) return null

        return ParsedVersion(base, identifiers.map { it.lowercase(Locale.ROOT) })
    }
}
