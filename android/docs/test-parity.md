# Android and desktop test correspondence

This is a map of test responsibilities, not a fixed inventory or a claim that
every desktop declaration has an Android counterpart. Source tests and current
[game-runtime bindings](game-runtime-options.md) take precedence over the
original port-step tables.

| Concern | Desktop tests / owner | Android tests / owner |
| --- | --- | --- |
| Version rules | `VersionUtilsTests` | `VersionUtilsTest`, `BaseVersionTest` |
| Search and link parsing | `SearchMatcherTests`, `LinkTextParserTests` | `SearchMatcherTest`, `LinkTextParserTest`, `HttpsUrlsTest` |
| Cache/path decisions | `DownloadPathUtilsTests`, `DownloadCachePolicyTests` | corresponding pure util suites |
| File move decisions | `FileFinalizerTests` | `FileMoveRetryPolicyTest` plus transfer tests for I/O |
| Settings and local library | service persistence suites | `DataStoreSettingsStoreTest`, `FileLocalLibraryStoreTest` |
| Log format and maintenance | `LogsTests`, `LogsMaintenanceTests` | `FileLoggerTest`, `LogFilesTest` |
| Transfer and cleanup | `DownloadServiceTests`, `DownloadArtifactsTests` | `OkHttpDownloadTransferTest`, `DownloadFileStoreTest`, `DefaultDownloadManagerTest` |
| Durable worker decisions | Windows service owns transfer lifetime | `DownloadWorkerRunnerTest`, with Room and scheduler seams |
| Archive installation | `GameArchiveInstallerTests` and Library tests | `ArchiveGameInstallationServiceTest`, `InstallDownloadedFileHandoffTest` |
| Kirin registration and launch | Windows process/file model | `KirinGameLibraryTest`, `KirinGameLaunchServiceTest` |
| Navigation and shared loading | `MainViewModelTests`, `HomeViewModelTests` | corresponding ViewModel tests, `NavigationStoreTest`, `LauncherDataCoordinatorTest` |
| Game identity | Games/Library matching | `GameIdentityMatcherTest`, `GamesViewModelTest` |
| Auto-update | Settings/Game behavior | `GameAutoUpdateCoordinatorTest` and settings tests |
| Launcher update | `UpdateServiceTests` with Velopack seams | `LauncherUpdateServiceTest`, `ApkReleaseInstallerTest`, Settings/Main tests |
| Theme/catalogue completeness | interfaces, theme dictionaries | `StringsTest`, `FaqsTest`, `PalettesTest`, `SplashThemesTest` |

## Why some cases differ

- Windows registry startup, OneDrive checks, named wait handles, window restore,
  Explorer folder opening and Win32 error decoding have no equivalent input here.
- Kotlin non-null constructor parameters do not need a copy of every C# null-
  argument test.
- Android does not track playtime. Running detection and individual installed
  lookup are still unsupported; pending-adapter tests prove their fallback
  contract, not the behavior of the real install/launch methods.
- Real transfer tests use a loopback HTTP server, and archive/registry tests
  use temporary roots. Platform storage permissions and Kirin intents still
  need manual device acceptance.
- Launcher APK download starts after consent, unlike Velopack's download-first
  flow. Failures and declined offers therefore map to different Android owners.
- Settings persist enum names instead of desktop ordinals; UI state uses flows
  instead of WPF property notifications.
- Date output follows the selected language, while transfer speed is invariant.
  Tests pin those decisions rather than inheriting the machine's locale.

The port's measured collation and URL equivalence work is retained separately in
[search-parity.md](search-parity.md), so routine test instructions do not require
loading its historical measurements.
