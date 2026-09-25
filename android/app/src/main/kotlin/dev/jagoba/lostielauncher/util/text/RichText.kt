package dev.jagoba.lostielauncher.util.text

object RichText {
    data class Run(val text: String, val url: String?, val highlighted: Boolean)

    fun runs(text: String?, highlight: String?, detectLinks: Boolean): List<Run> {
        if (text.isNullOrEmpty()) return emptyList()
        val term = highlight?.trim().orEmpty()
        val segments = if (detectLinks) LinkTextParser.parse(text) else listOf(LinkTextParser.Segment(text, null))
        return segments.flatMap { highlighted(it.text, term, it.url) }
    }

    private fun highlighted(text: String, term: String, url: String?): List<Run> {
        val runs = mutableListOf<Run>()
        var index = 0
        for (match in SearchMatcher.findMatches(text, term)) {
            if (match.start > index) runs += Run(text.substring(index, match.start), url, false)
            runs += Run(text.substring(match.start, match.start + match.length), url, true)
            index = match.start + match.length
        }
        if (index < text.length) runs += Run(text.substring(index), url, false)
        return runs
    }
}
