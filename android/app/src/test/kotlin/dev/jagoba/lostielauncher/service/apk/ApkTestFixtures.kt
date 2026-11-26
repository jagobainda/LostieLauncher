package dev.jagoba.lostielauncher.service.apk

import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.service.download.DownloadTransfer
import io.mockk.coEvery
import java.io.File

internal fun DownloadTransfer.succeedsWriting(installer: File, onProgress: () -> Unit = {}) {
    coEvery { download(any(), any()) } coAnswers {
        secondArg<suspend (DownloadProgress) -> Unit>()(DownloadProgress(50.0, 0.0, 1, 2))
        onProgress()
        installer.parentFile?.mkdirs()
        installer.writeText("apk")
        DownloadTransferResult.Success
    }
}
