package dev.jagoba.lostielauncher.model

enum class ExternalLink(val brandName: String) {
    GITHUB("GitHub"),
    TWITCH("Twitch"),
    YOUTUBE("YouTube"),
    TWITTER("Twitter / X"),
}

data class ExternalLinkOptions(val urls: Map<ExternalLink, String>)
