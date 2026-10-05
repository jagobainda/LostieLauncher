package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadResumeMetadata
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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

    @Test
    fun `appends a partial response`() {
        DownloadResumePolicy.responseAction(206, 40, 100) shouldBe DownloadResponseAction.APPEND
    }

    @Test
    fun `restarts when a ranged request receives a full response`() {
        DownloadResumePolicy.responseAction(200, 40, 100) shouldBe DownloadResponseAction.RESTART
    }

    @Test
    fun `accepts a complete partial after range not satisfiable`() {
        DownloadResumePolicy.responseAction(416, 100, 100) shouldBe DownloadResponseAction.COMPLETE
    }

    @Test
    fun `invalidates a mismatched partial after range not satisfiable`() {
        DownloadResumePolicy.responseAction(416, 90, 100) shouldBe DownloadResponseAction.INVALIDATE
    }
}
