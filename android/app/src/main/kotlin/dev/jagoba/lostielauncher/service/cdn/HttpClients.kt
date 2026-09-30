package dev.jagoba.lostielauncher.service.cdn

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ContentClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class SecurityFlagClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DownloadClient
