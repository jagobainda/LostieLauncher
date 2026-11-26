package dev.jagoba.lostielauncher.util.net

import dev.jagoba.lostielauncher.util.log.Logger
import java.io.IOException
import retrofit2.HttpException

internal fun Throwable.isExpectedNetworkFailure(): Boolean = this !is DisallowedUrlException &&
    this !is ResponseTooLargeException &&
    (this is IOException || this is HttpException)

internal fun Logger.logNetworkOrError(
    error: Throwable,
    networkMessage: (String) -> String,
    unexpectedMessage: String,
) {
    if (error.isExpectedNetworkFailure()) {
        val detail = error.message?.takeIf(String::isNotBlank)
            ?.let { "${error.javaClass.simpleName}: $it" }
            ?: error.javaClass.simpleName
        info(networkMessage(detail))
    } else {
        error(unexpectedMessage, error)
    }
}
