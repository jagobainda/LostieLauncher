package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadResumeMetadata
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@DisplayName("DownloadResumePolicy")
class DownloadResumePolicyTest {
    @Test
    fun `uses a strong entity tag before the last modified date`() {
        val metadata = DownloadResumeMetadata("\"v1\"", "Wed, 21 Oct 2015 07:28:00 GMT", 100)

        DownloadResumePolicy.validator(metadata) shouldBe "\"v1\""
    }

    @Test
    fun `rejects a weak entity tag and uses the last modified date`() {
        val metadata = DownloadResumeMetadata("W/\"v1\"", "Wed, 21 Oct 2015 07:28:00 GMT", 100)

        DownloadResumePolicy.validator(metadata) shouldBe "Wed, 21 Oct 2015 07:28:00 GMT"
    }

    @Test
    fun `cannot resume without a validator`() {
        DownloadResumePolicy.validator(DownloadResumeMetadata(null, null, 100)) shouldBe null
    }

    @ParameterizedTest(name = "HTTP {0} with {1}/{2} ({3}) -> {4}")
    @CsvSource(
        "206, 40, 100, bytes 40-99/100, APPEND",
        "200, 40, 100, , RESTART",
        "416, 100, 100, , COMPLETE",
        "416, 90, 100, , INVALIDATE",
    )
    fun `decides the resume action from the response status and range`(
        statusCode: Int,
        partialBytes: Long,
        expectedTotalBytes: Long?,
        contentRange: String?,
        expected: String,
    ) {
        DownloadResumePolicy.responseAction(statusCode, partialBytes, expectedTotalBytes, contentRange) shouldBe
            DownloadResponseAction.valueOf(expected)
    }
}
