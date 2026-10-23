package dev.jagoba.lostielauncher.core.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.ApkInstallOptions
import dev.jagoba.lostielauncher.service.apk.AndroidApkPackages
import dev.jagoba.lostielauncher.service.apk.ApkPackages
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ApkBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindPackages(impl: AndroidApkPackages): ApkPackages
}

@Module
@InstallIn(SingletonComponent::class)
internal object ApkModule {
    @Provides
    @Singleton
    fun provideApkInstallOptions(@ApplicationContext context: Context): ApkInstallOptions =
        ApkInstallOptions("${context.packageName}.files")
}
