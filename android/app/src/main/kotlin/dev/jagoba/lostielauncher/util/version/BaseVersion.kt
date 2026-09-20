package dev.jagoba.lostielauncher.util.version

/**
 * A dotted numeric version, with .NET's `System.Version` semantics.
 *
 * It exists because the comparison has to stay bit-for-bit the desktop's, and
 * the two runtimes disagree about what a version is. The rules reproduced here
 * are `System.Version`'s, not a reasonable interpretation of them:
 *
 * - **Two to four components.** `1` does not parse; `1.2.3.4.5` does not
 *   either.
 * - **An absent component is `-1`, not `0`.** This is the rule that makes `1.2`
 *   compare **lower** than `1.2.0`, and it is load-bearing: the launcher would
 *   otherwise offer an update between two versions that are the same release.
 * - **A component is parsed as .NET parses an `Int32`** with
 *   `NumberStyles.Integer`: surrounding whitespace is allowed, a leading `+` or
 *   `-` is allowed, and the result is then rejected if it is negative. So `-0`
 *   is `0` and parses, while `-1` does not; and an overflowing component fails
 *   rather than wrapping.
 *
 * That last rule is a .NET quirk rather than a design decision, and no catalogue
 * version has ever exercised it. It is reproduced anyway because this type's one
 * job is equivalence, not improvement.
 */
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
        /** The value `System.Version` stores for a component that was not written. */
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
