package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.GameDownloadArgs
import okhttp3.HttpUrl.Companion.toHttpUrl

internal object DownloadUrlResolver {
    fun resolve(baseUrl: String, args: GameDownloadArgs): String {
        val segments = args.relativePath.trim('/').split('/').filter(String::isNotEmpty)
        require(segments.isNotEmpty() && segments.none { it == "." || it == ".." })
        val builder = baseUrl.toHttpUrl().newBuilder()
        args.key?.let(builder::addPathSegment)
        segments.forEach(builder::addPathSegment)
        return builder.build().toString()
    }
}
