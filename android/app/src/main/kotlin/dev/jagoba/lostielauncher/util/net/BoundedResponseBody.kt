package dev.jagoba.lostielauncher.util.net

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer

internal class ResponseTooLargeException(label: String, maxBytes: Long) :
    IOException("$label response exceeded the $maxBytes-byte limit.")

internal class BoundedResponseBodyInterceptor(private val maxBytes: Long) : Interceptor {
    init {
        require(maxBytes >= 0L) { "maxBytes must not be negative." }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (chain.request().method == HEAD_METHOD) return response
        val body = response.body
        if (body.contentLength() > maxBytes) {
            response.close()
            throw ResponseTooLargeException(DEFAULT_LABEL, maxBytes)
        }
        val bounded = BoundedSource(body.source(), maxBytes, DEFAULT_LABEL).buffer()
            .asResponseBody(body.contentType(), body.contentLength())
        return response.newBuilder().body(bounded).build()
    }

    private companion object {
        const val HEAD_METHOD = "HEAD"
        const val DEFAULT_LABEL = "HTTP"
    }
}

internal fun ResponseBody.readBoundedText(maxBytes: Long, label: String): String {
    require(maxBytes >= 0L) { "maxBytes must not be negative." }
    if (contentLength() > maxBytes) throw ResponseTooLargeException(label, maxBytes)
    val charset = contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
    return BoundedSource(source(), maxBytes, label).buffer().readString(charset)
}

private class BoundedSource(delegate: Source, private val maxBytes: Long, private val label: String) :
    ForwardingSource(delegate) {
    private var totalRead = 0L

    override fun read(sink: Buffer, byteCount: Long): Long {
        val remaining = maxBytes - totalRead
        val allowed = if (byteCount <= remaining) byteCount else remaining + 1L
        val read = super.read(sink, allowed)
        if (read != -1L) {
            totalRead += read
            if (totalRead > maxBytes) throw ResponseTooLargeException(label, maxBytes)
        }
        return read
    }
}
