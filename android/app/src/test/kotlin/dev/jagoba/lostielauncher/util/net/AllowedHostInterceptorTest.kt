package dev.jagoba.lostielauncher.util.net

import dev.jagoba.lostielauncher.core.di.NetworkModule
import dev.jagoba.lostielauncher.model.AllowedHosts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AllowedHostInterceptor")
class AllowedHostInterceptorTest {
    private val kirinDownloadHosts = AllowedHosts(
        exact = setOf("github.com", "ericlostie-launcher.jagoba.dev", "cdn.jagoba.dev"),
        suffixes = setOf("githubusercontent.com"),
    )

    @Test
    fun `allows a request when no host allowlist tag is attached`() {
        val request = Request.Builder().url("https://unrestricted.example/item").build()
        val expected = okResponse(request)
        val chain = fakeChain(request, expected)

        AllowedHostRequestInterceptor.intercept(chain) shouldBe expected
        verify(exactly = 1) { chain.proceed(request) }
    }

    @Test
    fun `rejects an initial request to a disallowed host before connecting and without leaking the path`() {
        val request = Request.Builder()
            .url("https://evil.example/secret-segment/file.apk?token=xyz")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val chain = mockk<Interceptor.Chain> { every { request() } returns request }

        val error = shouldThrow<DisallowedUrlException> { AllowedHostRequestInterceptor.intercept(chain) }

        error.message.orEmpty() shouldNotContain "secret-segment"
        error.message.orEmpty() shouldNotContain "token=xyz"
        verify(exactly = 0) { chain.proceed(any()) }
    }

    @Test
    fun `allows a cross-host HTTPS redirect to an allowed CDN subdomain`() {
        val request = Request.Builder()
            .url("https://github.com/org/repo/releases/download/0.4.2/Kirin.apk")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val redirect = redirectResponse(request, "https://release-assets.githubusercontent.com/asset/Kirin.apk")
        val chain = fakeChain(request, redirect)

        AllowedHostRedirectInterceptor.intercept(chain) shouldBe redirect
    }

    @Test
    fun `allows a relative HTTPS redirect on the same allowed host`() {
        val request = Request.Builder()
            .url("https://github.com/org/repo/releases/download/0.4.2/Kirin.apk")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val redirect = redirectResponse(request, "/other/path/Kirin.apk")
        val chain = fakeChain(request, redirect)

        AllowedHostRedirectInterceptor.intercept(chain) shouldBe redirect
    }

    @Test
    fun `rejects an HTTPS to HTTP downgrade redirect even when the host is allowed`() {
        val request = Request.Builder()
            .url("https://github.com/org/repo/releases/download/0.4.2/Kirin.apk")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val redirect = redirectResponse(request, "http://release-assets.githubusercontent.com/secret/Kirin.apk")
        val chain = fakeChain(request, redirect)

        val error = shouldThrow<DisallowedUrlException> { AllowedHostRedirectInterceptor.intercept(chain) }

        error.message.orEmpty() shouldNotContain "secret"
    }

    @Test
    fun `rejects an HTTPS redirect to a disallowed host without leaking path or query`() {
        val request = Request.Builder()
            .url("https://github.com/org/repo/releases/download/0.4.2/Kirin.apk")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val redirect = redirectResponse(request, "https://evil.example/private/Kirin.apk?sig=123")
        val chain = fakeChain(request, redirect)

        val error = shouldThrow<DisallowedUrlException> { AllowedHostRedirectInterceptor.intercept(chain) }

        error.message.orEmpty() shouldNotContain "private"
        error.message.orEmpty() shouldNotContain "sig=123"
    }

    @Test
    fun `rejects a protocol-relative redirect to a disallowed host`() {
        val request = Request.Builder()
            .url("https://github.com/org/repo/releases/download/0.4.2/Kirin.apk")
            .tag(AllowedHosts::class.java, kirinDownloadHosts)
            .build()
        val redirect = redirectResponse(request, "//evil.example/Kirin.apk")
        val chain = fakeChain(request, redirect)

        shouldThrow<DisallowedUrlException> { AllowedHostRedirectInterceptor.intercept(chain) }
    }

    @Test
    fun `follows allowed redirects and preserves the allowlist tag across hops to block later disallowed hops`() {
        val client = NetworkModule.provideBaseHttpClient(NetworkModule.provideUserAgentInterceptor())
        MockWebServer().use { first ->
            MockWebServer().use { second ->
                first.start()
                second.start()
                val allowed = AllowedHosts(exact = setOf(first.hostName, second.hostName))
                first.enqueue(
                    MockResponse.Builder()
                        .code(302)
                        .addHeader("Location", second.url("/hop2").toString())
                        .build(),
                )
                second.enqueue(MockResponse.Builder().code(200).body("ok").build())

                val okRequest = Request.Builder()
                    .url(first.url("/hop1"))
                    .tag(AllowedHosts::class.java, allowed)
                    .build()
                client.newCall(okRequest).execute().use { response ->
                    response.code shouldBe 200
                    response.body.string() shouldBe "ok"
                }

                first.enqueue(
                    MockResponse.Builder()
                        .code(302)
                        .addHeader("Location", second.url("/hop2").toString())
                        .build(),
                )
                second.enqueue(
                    MockResponse.Builder()
                        .code(302)
                        .addHeader("Location", "http://evil.invalid/hop3")
                        .build(),
                )

                val blockedRequest = Request.Builder()
                    .url(first.url("/hop1"))
                    .tag(AllowedHosts::class.java, allowed)
                    .build()
                shouldThrow<DisallowedUrlException> {
                    client.newCall(blockedRequest).execute().close()
                }
            }
        }
    }

    private fun fakeChain(request: Request, response: Response): Interceptor.Chain = mockk {
        every { request() } returns request
        every { proceed(request) } returns response
    }

    private fun okResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("ok".toResponseBody())
        .build()

    private fun redirectResponse(request: Request, location: String): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(302)
        .message("Found")
        .header("Location", location)
        .body("".toResponseBody())
        .build()
}
