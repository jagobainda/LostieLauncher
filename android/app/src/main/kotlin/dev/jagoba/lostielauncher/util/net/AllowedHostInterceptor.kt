package dev.jagoba.lostielauncher.util.net

import dev.jagoba.lostielauncher.model.AllowedHosts
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

internal class DisallowedUrlException(message: String) : IOException(message)

internal object AllowedHostRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val allowedHosts = request.tag(AllowedHosts::class.java)
        if (allowedHosts != null && !allowedHosts.allows(request.url.host)) {
            throw DisallowedUrlException("Request host '${request.url.host}' is not allowed.")
        }
        return chain.proceed(request)
    }
}

internal object AllowedHostRedirectInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val allowedHosts = request.tag(AllowedHosts::class.java)
        if (allowedHosts != null && !allowedHosts.allows(request.url.host)) {
            throw DisallowedUrlException("Request host '${request.url.host}' is not allowed.")
        }
        val response = chain.proceed(request)
        if (allowedHosts != null && response.isRedirect) {
            validateRedirectTarget(request.url, response, allowedHosts)
        }
        return response
    }

    private fun validateRedirectTarget(currentUrl: HttpUrl, response: Response, allowedHosts: AllowedHosts) {
        val location = response.header(LOCATION_HEADER) ?: return
        val nextUrl = currentUrl.resolve(location)
        if (nextUrl == null) {
            response.close()
            throw DisallowedUrlException("Redirect target could not be resolved.")
        }
        if (currentUrl.isHttps && !nextUrl.isHttps) {
            response.close()
            throw DisallowedUrlException("Redirect from HTTPS to '${nextUrl.scheme}' is not allowed.")
        }
        if (!allowedHosts.allows(nextUrl.host)) {
            response.close()
            throw DisallowedUrlException("Redirect host '${nextUrl.host}' is not allowed.")
        }
    }

    private const val LOCATION_HEADER = "Location"
}
