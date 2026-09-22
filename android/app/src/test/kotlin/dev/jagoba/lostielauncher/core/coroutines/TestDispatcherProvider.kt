package dev.jagoba.lostielauncher.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher

internal class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val default = dispatcher
    override val main = dispatcher
}
