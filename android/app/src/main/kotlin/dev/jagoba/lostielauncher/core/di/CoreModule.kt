package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.core.coroutines.DefaultDispatcherProvider
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.util.log.AndroidLogger
import dev.jagoba.lostielauncher.util.log.Logger
import javax.inject.Singleton

/**
 * The composition root's application-wide bindings.
 *
 * This is the counterpart of `Core/DependencyInjection.cs`: the single place a
 * type is wired up, everything a singleton, everything reached by constructor
 * injection. Nothing is ever constructed at a call site.
 *
 * Later steps add their own modules next to this one — one per area, not one
 * giant module — and they follow the same two rules: no URL, path or magic
 * number inside the type that does the work, and every platform-bound thing
 * behind an interface so the thing using it stays testable.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()
}
