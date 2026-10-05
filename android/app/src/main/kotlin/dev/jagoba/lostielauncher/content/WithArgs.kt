package dev.jagoba.lostielauncher.content

fun String.withArgs(vararg args: Any?): String {
    if (args.isEmpty() || !contains('{')) return this

    val out = StringBuilder(length)
    var i = 0
    while (i < length) {
        val char = this[i]
        if (char != '{') {
            out.append(char)
            i++
            continue
        }

        val close = indexOf('}', startIndex = i + 1)
        val index = if (close > i + 1) substring(i + 1, close).toIntOrNull() else null
        if (index != null && index in args.indices) {
            out.append(args[index]?.toString() ?: "")
            i = close + 1
        } else {
            out.append(char)
            i++
        }
    }
    return out.toString()
}
