package dev.jagoba.lostielauncher.content

/**
 * One frequently-asked question and its answer.
 *
 * The desktop's `FaqEntry` record struct, in `Content/Faqs.cs`. It is a
 * `content/` type rather than a `model/` one for the same reason it is on that
 * side: it is catalogue text, and the FAQ screen reads it through [faqsFor]
 * exactly as every label reads [Strings].
 */
data class FaqEntry(val question: String, val answer: String)
