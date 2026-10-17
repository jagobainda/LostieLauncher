package dev.jagoba.lostielauncher.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.core.coroutines.DefaultDispatcherProvider
import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
