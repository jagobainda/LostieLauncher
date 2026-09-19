package dev.jagoba.lostielauncher.content

/**
 * Substitutes the `{0}`, `{1}` … placeholders in a catalogue string.
 *
 * **It is called `withArgs` and not `format` for a reason.** `kotlin.text`
 * declares `String.format(vararg Any?)` and is imported into every file by
 * default, so an extension of that name here would not shadow it — it would
 * lose to it, silently, in any file that forgot the explicit import. No
 * warning, no error: the call returns the template with `{0}` still in it,
 * because `java.lang.String.format` looks for `%s` and finds none. That is
 * eight dialogs rendering a placeholder to the user, and the compiler is no
 * help. A name the standard library does not own removes the trap outright,
 * and `WithArgsResolutionTest` — which lives in another package on purpose —
 * is what keeps it removed.
 *
 * The catalogue keeps the desktop's placeholder syntax, which is .NET's
 * `string.Format`, so the eight translations stay byte-identical to
 * `desktop/LostieLauncher/Content/Strings.cs` and a translator sees the same
 * text on both sides. Kotlin has no `string.Format`, and the two obvious
 * substitutes are both wrong for this:
 *
 * - `String.format` needs `%1$s` markers, which would mean rewriting all
 *   eight copies of the eight keys that carry one.
 * - `java.text.MessageFormat` reads `{0}` but also assigns meaning to a single
 *   quote, and the catalogue is full of them — `s'han`, `l'aplicació`,
 *   `d'informació`. It would eat them.
 *
 * So this does the one thing the desktop does: replace `{n}` with the n-th
 * argument, left to right, in a single pass. Nothing else in the string is
 * interpreted, an argument is inserted verbatim, and a `{n}` with no matching
 * argument is left as it is rather than throwing — a missing argument must
 * not be able to crash a dialog.
 *
 * Single pass matters: an argument that itself contains `{0}` (a game name, a
 * filesystem path) is never re-scanned.
 *
 * `spec/05-localization.md` lists the eight keys that take arguments and the
 * order of each one's.
 */
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
