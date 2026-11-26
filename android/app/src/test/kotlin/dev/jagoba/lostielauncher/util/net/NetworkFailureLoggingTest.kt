package dev.jagoba.lostielauncher.util.net

import dev.jagoba.lostielauncher.util.log.Logger
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

@DisplayName("NetworkFailureLogging")
class NetworkFailureLoggingTest {
    private val logger = mockk<Logger>(relaxed = true)

    @Test
    fun `logs IOException at info without a stack trace`() {
        val error = SocketTimeoutException("timeout")

        logger.logNetworkOrError(error, { "Request failed ($it)." }, "Request failed.")

        verify(exactly = 1) { logger.info("Request failed (SocketTimeoutException: timeout).") }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `omits the colon when an expected network exception has no message`() {
        val error = IOException()

        logger.logNetworkOrError(error, { "Request failed ($it)." }, "Request failed.")

        verify(exactly = 1) { logger.info("Request failed (IOException).") }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `logs HttpException at info without a stack trace`() {
        val error = HttpException(Response.error<String>(503, "".toResponseBody()))

        logger.logNetworkOrError(error, { "Request failed ($it)." }, "Request failed.")

        verify(exactly = 1) { logger.info(match { it.startsWith("Request failed (HttpException:") }) }
        verify(exactly = 0) { logger.error(any(), any()) }
    }

    @Test
    fun `keeps DisallowedUrlException and ResponseTooLargeException at error with the throwable`() {
        val disallowed = DisallowedUrlException("rejected")
        val oversized = ResponseTooLargeException("Feed", 32)

        disallowed.isExpectedNetworkFailure() shouldBe false
        oversized.isExpectedNetworkFailure() shouldBe false

        logger.logNetworkOrError(disallowed, { "Request failed ($it)." }, "Request failed.")
        logger.logNetworkOrError(oversized, { "Request failed ($it)." }, "Request failed.")

        verify(exactly = 0) { logger.info(any()) }
        verify(exactly = 1) { logger.error("Request failed.", disallowed) }
        verify(exactly = 1) { logger.error("Request failed.", oversized) }
    }

    @Test
    fun `keeps non-network exceptions at error with the throwable`() {
        val error = IllegalStateException("unexpected")

        logger.logNetworkOrError(error, { "Request failed ($it)." }, "Request failed.")

        verify(exactly = 0) { logger.info(any()) }
        verify(exactly = 1) { logger.error("Request failed.", error) }
    }
}
