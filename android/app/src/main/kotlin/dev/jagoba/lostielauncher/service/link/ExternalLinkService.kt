package dev.jagoba.lostielauncher.service.link

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.model.ExternalLinkOptions
import dev.jagoba.lostielauncher.util.log.Logger
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import javax.inject.Inject

enum class ExternalLinkResult {
    OPENED,
    INVALID_URL,
    NO_HANDLER,
}

interface ExternalLinkService {
    fun open(link: ExternalLink): ExternalLinkResult
}

internal class AndroidExternalLinkService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val options: ExternalLinkOptions,
    private val logger: Logger,
) : ExternalLinkService {
    override fun open(link: ExternalLink): ExternalLinkResult {
        val uri = HttpsUrls.parseOrNull(options.urls[link])
        if (uri == null) {
            logger.error("External link $link was rejected because its URL is not HTTPS.")
            return ExternalLinkResult.INVALID_URL
        }
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, HttpsUrls.canonicalUrl(uri).toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            ExternalLinkResult.OPENED
        } catch (error: Exception) {
            logger.error("Android could not open external link $link.", error)
            ExternalLinkResult.NO_HANDLER
        }
    }
}
