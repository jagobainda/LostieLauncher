package dev.jagoba.lostielauncher.core.di

import android.content.Context
import android.os.Build
import android.os.Environment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jagoba.lostielauncher.model.InstalledGameManifestOptions
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.model.KirinSetupOptions
import dev.jagoba.lostielauncher.model.LocalLibraryOptions
import dev.jagoba.lostielauncher.service.game.AndroidKirinShortcut
import dev.jagoba.lostielauncher.service.game.DataStoreKirinMinimumVersionStore
import dev.jagoba.lostielauncher.service.game.DefaultKirinSetupService
import dev.jagoba.lostielauncher.service.game.FileInstalledGameManifestStore
import dev.jagoba.lostielauncher.service.game.InstalledGameManifestStore
import dev.jagoba.lostielauncher.service.game.KirinMinimumVersionStore
import dev.jagoba.lostielauncher.service.game.KirinSetupService
import dev.jagoba.lostielauncher.service.game.KirinShortcut
import dev.jagoba.lostielauncher.service.library.FileLocalLibraryStore
import dev.jagoba.lostielauncher.service.library.LocalLibraryStore
import java.io.File
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Module
@InstallIn(SingletonComponent::class)
internal abstract class KirinBindingsModule {
    @Binds
    @Singleton
    internal abstract fun bindShortcut(impl: AndroidKirinShortcut): KirinShortcut

    @Binds
    @Singleton
    internal abstract fun bindMinimumVersionStore(impl: DataStoreKirinMinimumVersionStore): KirinMinimumVersionStore

    @Binds
    @Singleton
    internal abstract fun bindSetupService(impl: DefaultKirinSetupService): KirinSetupService

    @Binds
    @Singleton
    internal abstract fun bindLocalLibraryStore(impl: FileLocalLibraryStore): LocalLibraryStore

    @Binds
    @Singleton
    internal abstract fun bindManifestStore(impl: FileInstalledGameManifestStore): InstalledGameManifestStore
}

@Module
@InstallIn(SingletonComponent::class)
internal object KirinModule {
    @Provides
    @Singleton
    fun provideKirinOptions(): KirinOptions {
        val sharedStorageRoot = Environment.getExternalStorageDirectory()
        return KirinOptions(
            gamesRoot = File(sharedStorageRoot, "LostieLauncher"),
            libraryFile = File(sharedStorageRoot, "Kirin/game-library.json"),
            executableName = "Game.exe",
            runtimeMode = "auto",
            packageName = "com.gmax.kirin",
            launchActivity = "com.gmax.kirin.LibraryActivity",
            launchAction = "com.gmax.kirin.action.LAUNCH_GAME_SHORTCUT",
            gamePathExtra = "shortcut_game_path",
            archiveBufferSizeBytes = 64 * 1024,
            archiveExtractionWorkers = 4,
            archiveEntryCostBytes = 64 * 1024,
            archiveProgressInterval = 500.milliseconds,
            maxLibraryFileBytes = KirinOptions.DEFAULT_MAX_LIBRARY_FILE_BYTES,
            archiveMaxEntries = KirinOptions.DEFAULT_ARCHIVE_MAX_ENTRIES,
            archiveMaxUncompressedBytes = KirinOptions.DEFAULT_ARCHIVE_MAX_UNCOMPRESSED_BYTES,
            archiveMaxCompressionRatio = KirinOptions.DEFAULT_ARCHIVE_MAX_COMPRESSION_RATIO,
            archiveCompressionRatioThresholdBytes = KirinOptions.DEFAULT_ARCHIVE_COMPRESSION_RATIO_THRESHOLD_BYTES,
        )
    }

    @Provides
    @Singleton
    fun provideKirinSetupOptions(@ApplicationContext context: Context): KirinSetupOptions = KirinSetupOptions(
        requirementsUrl = "https://ericlostie-launcher.jagoba.dev/public/config/kirin-setup-options.json",
        fallbackMinimumVersion = "0.4.0",
        installerFile = File(context.cacheDir, "apk/kirin/Kirin.apk"),
        signerSha256 = "a0033b292224df78a686a199eecf99bfbc45c2e0a23a311bfdeddbaad0cc20b2",
        supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
        feedRetryInterval = 5.minutes,
        maxInstallerBytes = KirinSetupOptions.DEFAULT_MAX_INSTALLER_BYTES,
    )

    @Provides
    @Singleton
    internal fun provideLocalLibraryOptions(kirin: KirinOptions): LocalLibraryOptions = LocalLibraryOptions(
        registryFile = File(kirin.gamesRoot, "local_games.json"),
        maxRegistryBytes = LocalLibraryOptions.DEFAULT_MAX_REGISTRY_BYTES,
    )

    @Provides
    @Singleton
    internal fun provideInstalledGameManifestOptions(@ApplicationContext context: Context) =
        InstalledGameManifestOptions(File(context.noBackupFilesDir, "manifests"))
}
