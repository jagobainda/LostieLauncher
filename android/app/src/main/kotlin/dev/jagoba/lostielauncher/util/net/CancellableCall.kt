package dev.jagoba.lostielauncher.util.net

import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import okhttp3.Call
import okhttp3.Response

internal suspend inline fun <T> Call.useCancellableResponse(
    receive: (Call) -> Response = { it.execute() },
    consume: (Response) -> T,
): T {
    val context = currentCoroutineContext()
    // This child finishes on parent cancellation even while the caller is blocked in a socket read.
    val cancellation = Job(context.job)
    cancellation.invokeOnCompletion { if (it != null) cancel() }
    try {
        context.ensureActive()
        return receive(this).use(consume)
    } catch (error: IOException) {
        context.ensureActive()
        throw error
    } finally {
        cancellation.complete()
    }
}
