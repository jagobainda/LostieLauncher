package dev.jagoba.lostielauncher.util.version

internal data class BaseVersion(val major: Int, val minor: Int, val build: Int, val revision: Int) :
    Comparable<BaseVersion> {
    override fun compareTo(other: BaseVersion): Int = compareValuesBy(
        this,
        other,
        BaseVersion::major,
        BaseVersion::minor,
        BaseVersion::build,
        BaseVersion::revision,
    )

    companion object {
        private const val ABSENT = -1

        private const val MIN_COMPONENTS = 2
        private const val MAX_COMPONENTS = 4

        fun parse(value: String): BaseVersion? {
            val parts = value.split('.')
            if (parts.size < MIN_COMPONENTS || parts.size > MAX_COMPONENTS) return null

            val components = IntArray(MAX_COMPONENTS) { ABSENT }
            for (index in parts.indices) {
                components[index] = parseComponent(parts[index]) ?: return null
            }

            return BaseVersion(components[0], components[1], components[2], components[3])
        }

        private fun parseComponent(text: String): Int? {
            val trimmed = text.trim()
            val signed = trimmed.startsWith('-') || trimmed.startsWith('+')
            val digits = if (signed) trimmed.substring(1) else trimmed
            if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null

            val magnitude = digits.toIntOrNull() ?: return null
            val value = if (trimmed.startsWith('-')) -magnitude else magnitude
            return if (value < 0) null else value
        }
    }
}
