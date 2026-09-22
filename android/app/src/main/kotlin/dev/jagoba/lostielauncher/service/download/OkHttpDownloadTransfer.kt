package dev.jagoba.lostielauncher.service.download

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.DownloadProgress
import dev.jagoba.lostielauncher.model.DownloadResumeMetadata
import dev.jagoba.lostielauncher.model.DownloadTransferOptions
import dev.jagoba.lostielauncher.model.DownloadTransferResult
import dev.jagoba.lostielauncher.service.cdn.DownloadClient
import dev.jagoba.lostielauncher.util.download.DownloadPathUtils
import dev.jagoba.lostielauncher.util.download.DownloadProgressCalculator
import dev.jagoba.lostielauncher.util.download.DownloadResponseAction
import dev.jagoba.lostielauncher.util.download.DownloadResumePolicy
import dev.jagoba.lostielauncher.util.download.DownloadSpeedSampler
import dev.jagoba.lostielauncher.util.file.FileMoveRetryPolicy
import dev.jagoba.lostielauncher.util.log.Logger
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

@Singleton
internal class OkHttpDownloadTransfer @Inject constructor(
    @DownloadClient private val client: OkHttpClient,
    private val options: DownloadTransferOptions,
    private val dispatchers: DispatcherProvider,
    private val timeSource: MonotonicTimeSource,
    private val json: Json,
    private val logger: Logger,
) : DownloadTransfer {
    override suspend fun download(
        request: DownloadTransferRequest,
        onProgress: suspend (DownloadProgress) -> Unit,
    ): DownloadTransferResult = withContext(dispatchers.io) {
        try {
            var lastError: IOException? = null
            for (attempt in 1..options.maximumAttempts) {
                try {
                    downloadOnce(request, onProgress)
                    return@withContext DownloadTransferResult.Success
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: AccessDeniedException) {
                    return@withContext DownloadTransferResult.PermissionDenied
                } catch (error: SecurityException) {
                    return@withContext DownloadTransferResult.PermissionDenied
                } catch (error: IOException) {
                    lastError = error
                    if (attempt < options.maximumAttempts) {
                        logger.info(
                            "Download attempt $attempt/${options.maximumAttempts} failed (${error.message}), retrying.",
                        )
                        delay(options.retryBaseDelay * attempt)
                    }
                }
            }
            val message = lastError?.message ?: MAXIMUM_RETRIES_MESSAGE
            logger.error("Download failed after ${options.maximumAttempts} attempts: $message", lastError)
            DownloadTransferResult.Failed(MAXIMUM_RETRIES_MESSAGE)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: AccessDeniedException) {
            logger.error("Download storage denied access.", error)
            DownloadTransferResult.PermissionDenied
        } catch (error: SecurityException) {
            logger.error("Download storage denied access.", error)
            DownloadTransferResult.PermissionDenied
        } catch (error: Exception) {
            logger.error("Download failed.", error)
            DownloadTransferResult.Failed(error.message ?: DOWNLOAD_FAILED_MESSAGE)
        }
    }

    private suspend fun downloadOnce(
        request: DownloadTransferRequest,
        onProgress: suspend (DownloadProgress) -> Unit,
    ) {
        val destination = File(request.destinationPath)
        destination.parentFile?.mkdirs()
        val partFile = File(DownloadPathUtils.getPartFilePath(request.destinationPath))
        val metadataFile = File(DownloadPathUtils.getMetaFilePath(partFile.path))
        var existingBytes = partFile.takeIf(File::exists)?.length() ?: 0
        var metadata = readMetadata(metadataFile)
        val requestBuilder = Request.Builder().url(request.url)
        if (existingBytes > 0) {
            val validator = DownloadResumePolicy.validator(metadata)
            if (validator == null) {
                partFile.delete()
                metadataFile.delete()
                existingBytes = 0
                metadata = null
            } else {
                requestBuilder.header(RANGE_HEADER, "bytes=$existingBytes-")
                requestBuilder.header(IF_RANGE_HEADER, validator)
            }
        }
        val call = client.newCall(requestBuilder.build())
        val cancellationHandle = currentCoroutineContext()[Job]?.invokeOnCompletion { error ->
            if (error is CancellationException) call.cancel()
        }
        try {
            val response = awaitResponse(call)
            response.use {
                when (DownloadResumePolicy.responseAction(response.code, existingBytes, metadata?.totalBytes)) {
                    DownloadResponseAction.COMPLETE -> {
                        finalize(partFile, destination)
                        metadataFile.delete()
                        onProgress(DownloadProgressCalculator.calculate(existingBytes, existingBytes, 0.0))
                        return
                    }

                    DownloadResponseAction.INVALIDATE -> {
                        partFile.delete()
                        metadataFile.delete()
                        throw IOException(INVALID_PARTIAL_MESSAGE)
                    }

                    DownloadResponseAction.RESTART -> {
                        partFile.delete()
                        existingBytes = 0
                        metadata = buildMetadata(response)
                        writeMetadata(metadataFile, metadata)
                    }

                    DownloadResponseAction.WRITE -> {
                        metadata = buildMetadata(response)
                        writeMetadata(metadataFile, metadata)
                    }

                    DownloadResponseAction.APPEND -> Unit

                    DownloadResponseAction.REJECT -> throw IOException(
                        "Download server returned HTTP ${response.code}.",
                    )
                }
                writeBody(response, partFile, existingBytes, onProgress)
            }
        } finally {
            cancellationHandle?.dispose()
        }
        finalize(partFile, destination)
        metadataFile.delete()
        val total = destination.length()
        onProgress(DownloadProgressCalculator.calculate(total, total, 0.0))
    }

    private suspend fun awaitResponse(call: Call): Response = try {
        withTimeout(options.inactivityTimeout) {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { call.cancel() }
                call.enqueue(
                    object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }

                        override fun onResponse(call: Call, response: Response) {
                            continuation.resume(response) { _, value, _ -> value.close() }
                        }
                    },
                )
            }
        }
    } catch (cancelled: CancellationException) {
        if (!currentCoroutineContext().isActive) throw cancelled
        throw IOException(INACTIVITY_MESSAGE, cancelled)
    }

    private suspend fun writeBody(
        response: Response,
        partFile: File,
        existingBytes: Long,
        onProgress: suspend (DownloadProgress) -> Unit,
    ) {
        val body = response.body
        val bodyLength = body.contentLength().takeIf { it >= 0 }
        val totalBytes = bodyLength?.plus(existingBytes)
        val source = body.source()
        val buffer = ByteArray(options.bufferSizeBytes)
        var totalRead = existingBytes
        val startedAt = timeSource.readNanos()
        val sampler = DownloadSpeedSampler(
            intervalNanos = options.progressSampleInterval.inWholeNanoseconds,
            initialBytes = existingBytes,
            initialNanos = startedAt,
        )
        FileOutputStream(partFile, existingBytes > 0).use { output ->
            while (true) {
                currentCoroutineContext().ensureActive()
                source.timeout().timeout(options.inactivityTimeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                val read = source.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                totalRead += read
                val speed = sampler.sample(totalRead, timeSource.readNanos())
                if (speed != null) {
                    onProgress(DownloadProgressCalculator.calculate(totalRead, totalBytes, speed))
                }
            }
            output.flush()
        }
    }

    private suspend fun finalize(partFile: File, destination: File) {
        var lastError: Throwable? = null
        for (attempt in 1..options.finalizationMaximumAttempts) {
            try {
                Files.move(partFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                return
            } catch (error: Throwable) {
                lastError = error
                val state = FileMoveRetryPolicy.DestinationState(
                    isDirectory = destination.isDirectory,
                    isReadOnly = destination.exists() && !destination.canWrite(),
                )
                if (attempt == options.finalizationMaximumAttempts || !FileMoveRetryPolicy.isRetryable(error, state)) {
                    throw error
                }
                delay(options.finalizationBaseDelay * attempt)
            }
        }
        throw IOException(DOWNLOAD_FAILED_MESSAGE, lastError)
    }

    private fun buildMetadata(response: Response): DownloadResumeMetadata = DownloadResumeMetadata(
        etag = response.header(ETAG_HEADER),
        lastModified = response.header(LAST_MODIFIED_HEADER),
        totalBytes = response.body.contentLength().takeIf { it >= 0 },
    )

    private fun readMetadata(file: File): DownloadResumeMetadata? = try {
        if (!file.exists()) null else json.decodeFromString<DownloadResumeMetadataDto>(file.readText()).toDomain()
    } catch (error: Exception) {
        logger.error("Download resume metadata could not be read.", error)
        null
    }

    private fun writeMetadata(file: File, metadata: DownloadResumeMetadata) {
        try {
            file.writeText(json.encodeToString(DownloadResumeMetadataDto.from(metadata)))
        } catch (error: Exception) {
            logger.error("Download resume metadata could not be written.", error)
        }
    }

    private companion object {
        const val RANGE_HEADER = "Range"
        const val IF_RANGE_HEADER = "If-Range"
        const val ETAG_HEADER = "ETag"
        const val LAST_MODIFIED_HEADER = "Last-Modified"
        const val MAXIMUM_RETRIES_MESSAGE = "Download failed after maximum retries."
        const val DOWNLOAD_FAILED_MESSAGE = "Download failed."
        const val INVALID_PARTIAL_MESSAGE = "Partial download is invalid and must restart."
        const val INACTIVITY_MESSAGE = "Download stalled because no data was received before the inactivity timeout."
    }
}

@Serializable
private data class DownloadResumeMetadataDto(
    val etag: String? = null,
    val lastModified: String? = null,
    val totalBytes: Long? = null,
) {
    fun toDomain(): DownloadResumeMetadata = DownloadResumeMetadata(etag, lastModified, totalBytes)

    companion object {
        fun from(metadata: DownloadResumeMetadata): DownloadResumeMetadataDto = DownloadResumeMetadataDto(
            metadata.etag,
            metadata.lastModified,
            metadata.totalBytes,
        )
    }
}
