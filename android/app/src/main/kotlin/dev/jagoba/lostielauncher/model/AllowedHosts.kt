package dev.jagoba.lostielauncher.model

import java.util.Locale

data class AllowedHosts(val exact: Set<String> = emptySet(), val suffixes: Set<String> = emptySet()) {
    private val normalizedExact: Set<String> = exact.mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }
    private val normalizedSuffixes: Set<String> = suffixes.mapTo(mutableSetOf()) {
        it.trimStart('.').lowercase(Locale.ROOT)
    }

    fun allows(host: String?): Boolean {
        val candidate = host?.lowercase(Locale.ROOT)?.takeIf(String::isNotBlank) ?: return false
        if (candidate in normalizedExact) return true
        return normalizedSuffixes.any { suffix ->
            suffix.isNotEmpty() &&
                candidate.length > suffix.length + 1 &&
                !candidate.startsWith('.') &&
                candidate.endsWith(".$suffix")
        }
    }
}
