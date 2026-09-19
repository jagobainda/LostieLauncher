package dev.jagoba.lostielauncher.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The seam that keeps anything doing real work off the main thread and testable
 * without one.
 *
 * Nothing outside this interface names a [Dispatchers] member. A service or
 * ViewModel takes a `DispatcherProvider`, so a test can hand it a single
 * deterministic test dispatcher and stop guessing about timing — the Android
 * counterpart of the desktop's `.ConfigureAwait(false)` discipline and its
 * guarded-dispatcher pattern.
 */
interface DispatcherProvider {
    /** Blocking I/O: network, disk, the content resolver. */
    val io: CoroutineDispatcher

    /** CPU-bound work: parsing, hashing, sorting a catalogue. */
    val default: CoroutineDispatcher

    /** The UI thread. */
    val main: CoroutineDispatcher
}

/** The production provider. The only place [Dispatchers] is referenced. */
class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
