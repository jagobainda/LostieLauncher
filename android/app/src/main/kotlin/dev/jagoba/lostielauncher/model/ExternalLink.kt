package dev.jagoba.lostielauncher.model

enum class ExternalLink {
    GITHUB,
    TWITCH,
    YOUTUBE,
    TWITTER,
}

data class ExternalLinkOptions(val urls: Map<ExternalLink, String>)
