package dev.jagoba.lostielauncher.core.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.LauncherUpdateOptions
import dev.jagoba.lostielauncher.service.update.DefaultLauncherUpdateService
import dev.jagoba.lostielauncher.service.update.LauncherUpdateService
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindLauncherUpdateService(impl: DefaultLauncherUpdateService): LauncherUpdateService
}

@Module
@InstallIn(SingletonComponent::class)
internal object UpdateModule {
    @Provides
    @Singleton
    fun provideLauncherUpdateOptions(@ApplicationContext context: Context): LauncherUpdateOptions =
        LauncherUpdateOptions(
            packageName = context.packageName,
            feedUrl = "https://ericlostie-launcher.jagoba.dev/public/android/latest.json",
            installerFile = File(context.cacheDir, "apk/launcher/LostieLauncher.apk"),
        )
}
