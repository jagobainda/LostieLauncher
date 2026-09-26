package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.content.strings.CatStrings
import dev.jagoba.lostielauncher.content.strings.EngStrings
import dev.jagoba.lostielauncher.content.strings.EspStrings
import dev.jagoba.lostielauncher.content.strings.EusStrings
import dev.jagoba.lostielauncher.content.strings.FraStrings
import dev.jagoba.lostielauncher.content.strings.GalStrings
import dev.jagoba.lostielauncher.content.strings.PorStrings
import dev.jagoba.lostielauncher.content.strings.ValStrings
import dev.jagoba.lostielauncher.model.AppLanguage

interface Strings {
    val titleHome: String
    val titleGames: String
    val titleLibrary: String
    val titleSettings: String
    val titleFaqs: String
    val faqsSearchPlaceholder: String
    val faqsNoResults: String
    val btnOk: String
    val btnYes: String
    val btnNo: String
    val btnDownload: String
    val btnDownloaded: String
    val btnPause: String
    val btnResume: String
    val btnUpdate: String
    val btnPlay: String
    val tooltipOpenFolder: String
    val tooltipOpenHelp: String
    val tooltipUninstall: String
    val tooltipRefresh: String
    val folderNotFoundTitle: String
    val folderNotFoundMessage: String
    val uninstallConfirmTitle: String
    val uninstallConfirmMessage: String
    val uninstallNotFoundTitle: String
    val uninstallNotFoundMessage: String
    val uninstallErrorTitle: String
    val uninstallErrorMessage: String
    val uninstallBlockedTitle: String
    val uninstallBlockedMessage: String
    val uninstallGameRunningTitle: String
    val uninstallGameRunningMessage: String
    val uninstallMaybeRunningMessage: String
    val updateAvailableTitle: String
    val updateAvailableMessage: String
    val settingsGeneral: String
    val settingsAppearance: String
    val settingsAutoUpdate: String
    val settingsLanguage: String
    val settingsDownloadDir: String
    val settingsTheme: String
    val btnBrowse: String
    val settingsCheckForUpdates: String
    val upToDateTitle: String
    val upToDateMessage: String
    val updateCheckBusyTitle: String
    val updateCheckBusyMessage: String
    val updateCheckFailedTitle: String
    val updateCheckFailedMessage: String
    val changeDownloadDirTitle: String
    val changeDownloadDirMessage: String
    val exitWarningTitle: String
    val exitWarningDownloadMessage: String
    val exitWarningGameMessage: String
    val exitWarningBothMessage: String
    val libraryNoContent: String
    val gamesNoContent: String
    val gamesGoToLibrary: String
    val homeNews: String
    val homeNotifications: String
    val homeNoContent: String
    val downloadDialogTitle: String
    val downloadDialogPath: String
    val downloadDialogGameSize: String
    val downloadDialogFreeSpace: String
    val downloadDialogViewPage: String
    val downloadDialogNoDescription: String
    val downloadDialogKey: String
    val downloadKeyInvalidTitle: String
    val downloadKeyInvalidMessage: String
    val downloadErrorTitle: String
    val downloadErrorMessage: String
    val downloadPermissionDeniedTitle: String
    val downloadPermissionDeniedMessage: String
    val btnCancel: String
    val cancelDownloadConfirmTitle: String
    val cancelDownloadConfirmMessage: String
    val statusExtracting: String
    val statusVerifying: String
    val statusUninstalling: String
    val statusNotSupportedYet: String
    val notSupportedYetMessage: String
    val locationNoHandlerTitle: String
    val locationNoHandlerMessage: String
    val gameExeNotFoundTitle: String
    val gameExeNotFoundMessage: String
    val hashMismatchTitle: String
    val hashMismatchMessage: String
    val welcomeDialogTitle: String
    val welcomeDialogDescription: String
    val welcomeDialogContinue: String
    val repositoryUrl: String
    val specialVersionDialogTitle: String
    val specialVersionDialogDescription: String
    val specialVersionDialogKeyLabel: String
    val btnConfirm: String
    val downloadKeyNotFoundTitle: String
    val downloadKeyNotFoundMessage: String
    val downloadKeyMismatchTitle: String
    val downloadKeyMismatchMessage: String
    val tooltipSwitchSpecialVersion: String
    val serverActionsUnavailableTitle: String
    val serverActionsUnavailableMessage: String
    val offlineModeLabel: String
    val serverMaintenanceNotificationTitle: String
    val serverMaintenanceNotificationMessage: String
    val homeContentUnavailable: String
    val contentOutOfDateLabel: String
    val contentOutOfDateMessage: String
    val settingsGamesStoredIn: String
    val settingsOneDriveWarning: String
    val oneDriveWarningTitle: String
    val oneDriveWarningMessage: String
    val downloadDirNotUsableTitle: String
    val downloadDirNotUsableMessage: String
    val downloadDirStepCreate: String
    val downloadDirStepWrite: String
    val downloadDirStepRename: String
}

fun stringsFor(language: AppLanguage): Strings = when (language) {
    AppLanguage.ESP -> EspStrings
    AppLanguage.ENG -> EngStrings
    AppLanguage.CAT -> CatStrings
    AppLanguage.EUS -> EusStrings
    AppLanguage.GAL -> GalStrings
    AppLanguage.POR -> PorStrings
    AppLanguage.VAL -> ValStrings
    AppLanguage.FRA -> FraStrings
}
