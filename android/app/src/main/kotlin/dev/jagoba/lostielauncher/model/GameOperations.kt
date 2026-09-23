package dev.jagoba.lostielauncher.model

import java.time.Instant
import java.util.UUID

data class GameTarget(val id: UUID?, val name: String)

data class GameInstallationRequest(
    val file: DownloadedFile,
    val catalogueId: UUID?,
    val expectedSha256: String?,
    val variant: String?,
)

sealed interface GameInstallationResult {
    data class Installed(val game: LocalGame) : GameInstallationResult

    data object MissingArchive : GameInstallationResult

    data object InvalidHash : GameInstallationResult

    data object HashMismatch : GameInstallationResult

    data object ExtractionFailed : GameInstallationResult

    data object RegistryFailed : GameInstallationResult

    data object NotSupportedYet : GameInstallationResult
}

sealed interface GameInstallationState {
    data object NotStarted : GameInstallationState

    data object VerifyingIntegrity : GameInstallationState

    data object Extracting : GameInstallationState

    data class Finished(val result: GameInstallationResult) : GameInstallationState
}

sealed interface InstalledGamesState {
    data class Available(val games: List<LocalGame>) : InstalledGamesState

    data object NotSupportedYet : InstalledGamesState
}

sealed interface InstalledGameResult {
    data class Installed(val game: LocalGame) : InstalledGameResult

    data object NotInstalled : InstalledGameResult

    data object NotSupportedYet : InstalledGameResult
}

enum class GameUninstallOutcome {
    COMPLETED,
    FILES_NOT_FOUND,
    FILES_LEFT_BEHIND,
    NOTHING_DELETED,
    GAME_RUNNING,
    NOT_SUPPORTED_YET,
}

/** An uninstall location token that presentation passes back without interpreting it. */
data class GameLocationReference(val token: String, val displayName: String?)

data class GameUninstallResult(val outcome: GameUninstallOutcome, val blockingLocation: GameLocationReference? = null)

sealed interface GameLaunchResult {
    data object Launched : GameLaunchResult

    data object GameNotFound : GameLaunchResult

    data object LaunchFailed : GameLaunchResult

    data object NotSupportedYet : GameLaunchResult
}

enum class GameRunningSignal {
    NOT_RUNNING,
    TRACKED_SESSION,
    POSSIBLY_RUNNING,
    NOT_SUPPORTED_YET,
}

sealed interface GameActivityState {
    data class Active(val games: Set<GameTarget>) : GameActivityState

    data object NotSupportedYet : GameActivityState
}

data class GamePlaySession(val gameId: UUID, val startedAt: Instant, val endedAt: Instant)

sealed interface PlaySessionResult {
    data class Recorded(val minutes: Int) : PlaySessionResult

    data object Ignored : PlaySessionResult

    data object NotSupportedYet : PlaySessionResult
}

enum class GameLocation {
    GAME,
    HELP,
}

enum class GameHelpAvailability {
    AVAILABLE,
    NOT_FOUND,
    NOT_SUPPORTED_YET,
}

sealed interface OpenGameLocationResult {
    data object Opened : OpenGameLocationResult

    data object NotFound : OpenGameLocationResult

    data object NoHandler : OpenGameLocationResult

    data object NotSupportedYet : OpenGameLocationResult
}
