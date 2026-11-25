package dev.jagoba.lostielauncher.service.download

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WorkManagerDownloadScheduler")
class WorkManagerDownloadSchedulerTest {
    private val workManager = mockk<WorkManager>()
    private val workId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @Test
    fun `manual downloads require any connected network`() {
        WorkManagerDownloadScheduler.constraintsFor(requiresUnmeteredNetwork = false)
            .requiredNetworkType shouldBe NetworkType.CONNECTED
    }

    @Test
    fun `automatic updates require an unmetered network`() {
        WorkManagerDownloadScheduler.constraintsFor(requiresUnmeteredNetwork = true)
            .requiredNetworkType shouldBe NetworkType.UNMETERED
    }

    @Test
    fun `enqueues unique work with the requested work id`() {
        val request = slot<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(any(), any(), capture(request))
        } returns mockk<Operation>()
        val sut = createSut()

        sut.enqueue("demo", workId, requiresUnmeteredNetwork = true)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork("game-download:demo", ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>())
        }
        request.captured.id shouldBe workId
    }

    private fun createSut() = WorkManagerDownloadScheduler(workManager)
}
