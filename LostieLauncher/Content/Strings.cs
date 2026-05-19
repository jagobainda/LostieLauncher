namespace LostieLauncher.Content;

public interface IStrings
{
    string TitleHome { get; }
    string TitleGames { get; }
    string TitleLibrary { get; }
    string TitleSettings { get; }
    string BtnOk { get; }
    string BtnYes { get; }
    string BtnNo { get; }
    string BtnDownload { get; }
    string BtnDownloading { get; }
    string BtnDownloaded { get; }
    string BtnPause { get; }
    string BtnResume { get; }
    string BtnUpdate { get; }
    string BtnPlay { get; }
    string TooltipOpenFolder { get; }
    string TooltipOpenHelp { get; }
    string TooltipUninstall { get; }
    string TooltipRefresh { get; }
    string FolderNotFoundTitle { get; }
    string FolderNotFoundMessage { get; }
    string UninstallConfirmTitle { get; }
    string UninstallConfirmMessage { get; }
    string UninstallNotFoundTitle { get; }
    string UninstallNotFoundMessage { get; }
    string UninstallErrorTitle { get; }
    string UninstallErrorMessage { get; }
    string UpdateAvailableTitle { get; }
    string UpdateAvailableMessage { get; }
    string SettingsGeneral { get; }
    string SettingsAppearance { get; }
    string SettingsStartWithWindows { get; }
    string SettingsStartMinimized { get; }
    string SettingsAutoUpdate { get; }
    string SettingsLanguage { get; }
    string SettingsDownloadDir { get; }
    string SettingsTheme { get; }
    string BtnBrowse { get; }
    string SettingsCheckForUpdates { get; }
    string CheckForUpdatesTitle { get; }
    string CheckForUpdatesMessage { get; }
    string ChangeDownloadDirTitle { get; }
    string ChangeDownloadDirMessage { get; }
    string TrayOpen { get; }
    string TrayExit { get; }
    string LibraryNoContent { get; }
    string GamesNoContent { get; }
    string GamesGoToLibrary { get; }
    string HomeNews { get; }
    string HomeNotifications { get; }
    string HomeLoading { get; }
    string HomeNoContent { get; }
    string DownloadDialogTitle { get; }
    string DownloadDialogPath { get; }
    string DownloadDialogGameSize { get; }
    string DownloadDialogFreeSpace { get; }
    string DownloadDialogViewPage { get; }
    string DownloadDialogNoDescription { get; }
    string DownloadDialogKey { get; }
    string DownloadKeyInvalidTitle { get; }
    string DownloadKeyInvalidMessage { get; }
    string DownloadKeyConsumedTitle { get; }
    string DownloadKeyConsumedMessage { get; }
    string DownloadErrorTitle { get; }
    string DownloadErrorMessage { get; }
    string BtnCancel { get; }
    string CancelDownloadConfirmTitle { get; }
    string CancelDownloadConfirmMessage { get; }
    string StatusExtracting { get; }
    string StatusVerifying { get; }
    string GameExeNotFoundTitle { get; }
    string GameExeNotFoundMessage { get; }
    string HashMismatchTitle { get; }
    string HashMismatchMessage { get; }
    string WelcomeDialogTitle { get; }
    string WelcomeDialogDescription { get; }
    string WelcomeDialogContinue { get; }
    string RepositoryUrl { get; }
    string SpecialVersionDialogTitle { get; }
    string SpecialVersionDialogDescription { get; }
    string SpecialVersionDialogKeyLabel { get; }
    string BtnConfirm { get; }
    string DownloadKeyNotFoundTitle { get; }
    string DownloadKeyNotFoundMessage { get; }
    string DownloadKeyMismatchTitle { get; }
    string DownloadKeyMismatchMessage { get; }
    string TooltipSwitchSpecialVersion { get; }
    string ServerActionsUnavailableTitle { get; }
    string ServerActionsUnavailableMessage { get; }
    string OfflineModeLabel { get; }
    string ServerMaintenanceNotificationTitle { get; }
    string ServerMaintenanceNotificationMessage { get; }
}

public class Esp : IStrings
{
    public string TitleHome => "Inicio";
    public string TitleGames => "Mis Juegos";
    public string TitleLibrary => "Biblioteca";
    public string TitleSettings => "Ajustes";
    public string BtnOk => "Aceptar";
    public string BtnYes => "Sí";
    public string BtnNo => "No";
    public string BtnDownload => "Descargar";
    public string BtnDownloading => "Descargando...";
    public string BtnDownloaded => "Descargado";
    public string BtnPause => "Pausar";
    public string BtnResume => "Reanudar";
    public string BtnUpdate => "Actualizar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Abrir carpeta del juego";
    public string TooltipOpenHelp => "Abrir carpeta de ayuda";
    public string TooltipUninstall => "Desinstalar juego";
    public string TooltipRefresh => "Actualizar";
    public string FolderNotFoundTitle => "Carpeta no encontrada";
    public string FolderNotFoundMessage => "La carpeta del juego no se encontró. ¿Quieres reinstalar los archivos?";
    public string UninstallConfirmTitle => "Desinstalar juego";
    public string UninstallConfirmMessage => "¿Seguro que quieres desinstalar {0}? Las partidas guardadas y el registro de tiempo jugado no se perderán.";
    public string UninstallNotFoundTitle => "Archivos no encontrados";
    public string UninstallNotFoundMessage => "No se encontraron los archivos del juego, pero se ha limpiado el registro de la lista.";
    public string UninstallErrorTitle => "Error al desinstalar";
    public string UninstallErrorMessage => "No se pudo desinstalar el juego. Es posible que algunos archivos estén en uso.";
    public string UpdateAvailableTitle => "Actualización disponible";
    public string UpdateAvailableMessage => "Nueva versión {0} disponible. ¿Reiniciar para actualizar?";
    public string SettingsGeneral => "General";
    public string SettingsAppearance => "Apariencia";
    public string SettingsStartWithWindows => "Iniciar con Windows";
    public string SettingsStartMinimized => "Iniciar minimizado";
    public string SettingsAutoUpdate => "Actualizaciones automáticas";
    public string SettingsLanguage => "Idioma";
    public string SettingsDownloadDir => "Directorio de descargas";
    public string SettingsTheme => "Tema";
    public string BtnBrowse => "Examinar...";
    public string SettingsCheckForUpdates => "Buscar actualizaciones del launcher";
    public string CheckForUpdatesTitle => "Buscar actualizaciones";
    public string CheckForUpdatesMessage => "Buscar actualizaciones requiere reiniciar el launcher. Si hay alguna descarga en progreso, podría dañarse o interrumpirse. ¿Deseas continuar?";
    public string ChangeDownloadDirTitle => "Cambiar directorio de descargas";
    public string ChangeDownloadDirMessage => "Si tienes juegos instalados, tendrás que moverlos manualmente a la nueva ruta o el launcher no los reconocerá. ¿Deseas continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Salir";
    public string LibraryNoContent => "No disponible";
    public string GamesNoContent => "No tienes juegos instalados";
    public string GamesGoToLibrary => "Ir a la biblioteca";
    public string HomeNews => "Novedades";
    public string HomeNotifications => "Notificaciones";
    public string HomeLoading => "Cargando...";
    public string HomeNoContent => "Sin contenido";
    public string DownloadDialogTitle => "Confirmar descarga";
    public string DownloadDialogPath => "Ruta de descarga";
    public string DownloadDialogGameSize => "Tamaño";
    public string DownloadDialogFreeSpace => "Espacio libre";
    public string DownloadDialogViewPage => "Ver página del juego";
    public string DownloadDialogNoDescription => "Sin descripción disponible.";
    public string DownloadDialogKey => "Clave para versiones especiales (opcional)";
    public string DownloadKeyInvalidTitle => "Clave no válida";
    public string DownloadKeyInvalidMessage => "El formato de la clave no es válido. Debe seguir el formato XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Clave consumida";
    public string DownloadKeyConsumedMessage => "Esta clave ya no es válida o ha sido utilizada. Solicita una nueva clave.";
    public string DownloadErrorTitle => "Error en la descarga";
    public string DownloadErrorMessage => "No se pudo completar la descarga. Por favor, intenta más tarde. Si el problema persiste, escribe en #bugs-launcher en Discord.";
    public string BtnCancel => "Cancelar";
    public string CancelDownloadConfirmTitle => "Cancelar descarga";
    public string CancelDownloadConfirmMessage => "¿Seguro que quieres cancelar la descarga? Se eliminarán los archivos parcialmente descargados.";
    public string StatusExtracting => "Descomprimiendo...";
    public string StatusVerifying => "Comprobando integridad...";
    public string GameExeNotFoundTitle => "Juego no encontrado";
    public string GameExeNotFoundMessage => "No se encontró el ejecutable del juego. Intenta reinstalarlo.";
    public string HashMismatchTitle => "Error de integridad";
    public string HashMismatchMessage => "El archivo descargado está dañado o ha sido modificado. Por favor, intenta de nuevo. Si el problema persiste, escribe en #bugs-launcher en Discord.";
    public string WelcomeDialogTitle => "¡Bienvenido al Lostie Launcher!";
    public string WelcomeDialogDescription => "Descarga, actualiza y juega tus títulos favoritos en un solo lugar. Simple, rápido y sin complicaciones.\n\nTu privacidad es importante. No recogemos datos personales. Solo consultamos información básica de tu PC, como los núcleos del procesador o la RAM disponible para optimizar los juegos.\n\nEste proyecto es opensource. ¿Dudas sobre cómo funciona? Consulta el código fuente";
    public string WelcomeDialogContinue => "Continuar";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Cambiar a versión especial";
    public string SpecialVersionDialogDescription => "Al cambiar a una versión especial no se pierde nada, funciona como una actualización normal.";
    public string SpecialVersionDialogKeyLabel => "Clave de versión especial";
    public string BtnConfirm => "Confirmar";
    public string DownloadKeyNotFoundTitle => "Clave no encontrada";
    public string DownloadKeyNotFoundMessage => "No se ha encontrado una versión especial con esta clave. Comprueba la clave e inténtalo de nuevo.";
    public string DownloadKeyMismatchTitle => "Clave incorrecta";
    public string DownloadKeyMismatchMessage => "La clave no corresponde a este juego.";
    public string TooltipSwitchSpecialVersion => "Cambiar a versión especial";
    public string ServerActionsUnavailableTitle => "Servidor en mantenimiento";
    public string ServerActionsUnavailableMessage => "El servidor está en mantenimiento. Las descargas, actualizaciones y versiones especiales volverán en cuanto termine.";
    public string OfflineModeLabel => "Modo offline";
    public string ServerMaintenanceNotificationTitle => "Servidor en mantenimiento";
    public string ServerMaintenanceNotificationMessage => "El launcher está en modo offline. Puedes seguir viendo tus juegos instalados; las descargas y actualizaciones se reactivarán automáticamente cuando el servicio vuelva.";
}

public class Eng : IStrings
{
    public string TitleHome => "Home";
    public string TitleGames => "My Games";
    public string TitleLibrary => "Library";
    public string TitleSettings => "Settings";
    public string BtnOk => "OK";
    public string BtnYes => "Yes";
    public string BtnNo => "No";
    public string BtnDownload => "Download";
    public string BtnDownloading => "Downloading...";
    public string BtnDownloaded => "Downloaded";
    public string BtnPause => "Pause";
    public string BtnResume => "Resume";
    public string BtnUpdate => "Update";
    public string BtnPlay => "Play";
    public string TooltipOpenFolder => "Open game folder";
    public string TooltipOpenHelp => "Open help folder";
    public string TooltipUninstall => "Uninstall game";
    public string TooltipRefresh => "Refresh";
    public string FolderNotFoundTitle => "Folder not found";
    public string FolderNotFoundMessage => "The game folder was not found. Do you want to reinstall the files?";
    public string UninstallConfirmTitle => "Uninstall game";
    public string UninstallConfirmMessage => "Are you sure you want to uninstall {0}? Your saved games and playtime record will not be lost.";
    public string UninstallNotFoundTitle => "Files not found";
    public string UninstallNotFoundMessage => "The game files were not found, but the entry has been cleaned up from the list.";
    public string UninstallErrorTitle => "Uninstall error";
    public string UninstallErrorMessage => "Failed to uninstall the game. Some files may be in use.";
    public string UpdateAvailableTitle => "Update available";
    public string UpdateAvailableMessage => "New version {0} available. Restart to update?";
    public string SettingsGeneral => "General";
    public string SettingsAppearance => "Appearance";
    public string SettingsStartWithWindows => "Start with Windows";
    public string SettingsStartMinimized => "Start minimized";
    public string SettingsAutoUpdate => "Automatic updates";
    public string SettingsLanguage => "Language";
    public string SettingsDownloadDir => "Download directory";
    public string SettingsTheme => "Theme";
    public string BtnBrowse => "Browse...";
    public string SettingsCheckForUpdates => "Check for launcher updates";
    public string CheckForUpdatesTitle => "Check for updates";
    public string CheckForUpdatesMessage => "Checking for updates requires restarting the launcher. Any ongoing download may be interrupted or corrupted. Do you want to continue?";
    public string ChangeDownloadDirTitle => "Change download directory";
    public string ChangeDownloadDirMessage => "If you have installed games, you will need to move them manually to the new path or the launcher won't recognize them. Do you want to continue?";
    public string TrayOpen => "Open";
    public string TrayExit => "Exit";
    public string LibraryNoContent => "Not available";
    public string GamesNoContent => "No games installed";
    public string GamesGoToLibrary => "Go to Library";
    public string HomeNews => "News";
    public string HomeNotifications => "Notifications";
    public string HomeLoading => "Loading...";
    public string HomeNoContent => "No content";
    public string DownloadDialogTitle => "Confirm download";
    public string DownloadDialogPath => "Download path";
    public string DownloadDialogGameSize => "Size";
    public string DownloadDialogFreeSpace => "Free space";
    public string DownloadDialogViewPage => "View game page";
    public string DownloadDialogNoDescription => "No description available.";
    public string DownloadDialogKey => "Access key for special versions (optional)";
    public string DownloadKeyInvalidTitle => "Invalid key";
    public string DownloadKeyInvalidMessage => "The key format is invalid. It must follow the format XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Key consumed";
    public string DownloadKeyConsumedMessage => "This key is invalid or has already been used. Please request a new key.";
    public string DownloadErrorTitle => "Download failed";
    public string DownloadErrorMessage => "The download could not be completed. Please try again later. If the problem persists, write in #bugs-launcher on Discord.";
    public string BtnCancel => "Cancel";
    public string CancelDownloadConfirmTitle => "Cancel download";
    public string CancelDownloadConfirmMessage => "Are you sure you want to cancel the download? Partially downloaded files will be deleted.";
    public string StatusExtracting => "Extracting...";
    public string StatusVerifying => "Verifying integrity...";
    public string GameExeNotFoundTitle => "Game not found";
    public string GameExeNotFoundMessage => "The game executable was not found. Try reinstalling the game.";
    public string HashMismatchTitle => "Integrity error";
    public string HashMismatchMessage => "The downloaded file is corrupted or has been tampered with. Please try again. If the problem persists, write in #bugs-launcher on Discord.";
    public string WelcomeDialogTitle => "Welcome to Lostie Launcher!";
    public string WelcomeDialogDescription => "Download, update, and play your favorite games in one place. Simple, fast, and hassle-free.\n\nYour privacy is important. We don't collect personal data. We only check basic information about your PC, like processor cores or available RAM to optimize your games.\n\nThis project is open source. Questions about how it works? Check the source code";
    public string WelcomeDialogContinue => "Continue";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Switch to special version";
    public string SpecialVersionDialogDescription => "Switching to a special version won't lose anything, it works like a normal update.";
    public string SpecialVersionDialogKeyLabel => "Special version key";
    public string BtnConfirm => "Confirm";
    public string DownloadKeyNotFoundTitle => "Key not found";
    public string DownloadKeyNotFoundMessage => "No special version was found with this key. Please check the key and try again.";
    public string DownloadKeyMismatchTitle => "Incorrect key";
    public string DownloadKeyMismatchMessage => "The key does not match this game.";
    public string TooltipSwitchSpecialVersion => "Switch to special version";
    public string ServerActionsUnavailableTitle => "Server maintenance";
    public string ServerActionsUnavailableMessage => "The server is under maintenance. Downloads, updates, and special versions will return as soon as it is finished.";
    public string OfflineModeLabel => "Offline mode";
    public string ServerMaintenanceNotificationTitle => "Server maintenance";
    public string ServerMaintenanceNotificationMessage => "The launcher is in offline mode. You can keep viewing your installed games; downloads and updates will reactivate automatically when the service returns.";
}

public class Cat : IStrings
{
    public string TitleHome => "Inici";
    public string TitleGames => "Els meus jocs";
    public string TitleLibrary => "Biblioteca";
    public string TitleSettings => "Configuració";
    public string BtnOk => "Acceptar";
    public string BtnYes => "Sí";
    public string BtnNo => "No";
    public string BtnDownload => "Descarregar";
    public string BtnDownloading => "Descarregant...";
    public string BtnDownloaded => "Descarregat";
    public string BtnPause => "Pausar";
    public string BtnResume => "Reprendre";
    public string BtnUpdate => "Actualitzar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Obrir carpeta del joc";
    public string TooltipOpenHelp => "Obrir carpeta d'ajuda";
    public string TooltipUninstall => "Desinstal·lar joc";
    public string TooltipRefresh => "Actualitzar";
    public string FolderNotFoundTitle => "Carpeta no trobada";
    public string FolderNotFoundMessage => "La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers?";
    public string UninstallConfirmTitle => "Desinstal·lar joc";
    public string UninstallConfirmMessage => "Estàs segur que vols desinstal·lar {0}? Les teves partides desades i el registre de temps jugat no es perdran.";
    public string UninstallNotFoundTitle => "Fitxers no trobats";
    public string UninstallNotFoundMessage => "No s'han trobat els fitxers del joc, però s'ha netejat el registre de la llista.";
    public string UninstallErrorTitle => "Error en desinstal·lar";
    public string UninstallErrorMessage => "No s'ha pogut desinstal·lar el joc. És possible que alguns fitxers estiguin en ús.";
    public string UpdateAvailableTitle => "Actualització disponible";
    public string UpdateAvailableMessage => "Nova versió {0} disponible. Reiniciar per actualitzar?";
    public string SettingsGeneral => "General";
    public string SettingsAppearance => "Aparença";
    public string SettingsStartWithWindows => "Iniciar amb Windows";
    public string SettingsStartMinimized => "Iniciar minimitzat";
    public string SettingsAutoUpdate => "Actualitzacions automàtiques";
    public string SettingsLanguage => "Idioma";
    public string SettingsDownloadDir => "Directori de descàrregues";
    public string SettingsTheme => "Tema";
    public string BtnBrowse => "Explorar...";
    public string SettingsCheckForUpdates => "Buscar actualitzacions del launcher";
    public string CheckForUpdatesTitle => "Buscar actualitzacions";
    public string CheckForUpdatesMessage => "Buscar actualitzacions requereix reiniciar el launcher. Si hi ha alguna descàrrega en curs, podria danyar-se o interrompre's. Vols continuar?";
    public string ChangeDownloadDirTitle => "Canviar directori de descàrregues";
    public string ChangeDownloadDirMessage => "Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar?";
    public string TrayOpen => "Obrir";
    public string TrayExit => "Sortir";
    public string LibraryNoContent => "No disponible";
    public string GamesNoContent => "No tens cap joc instal·lat";
    public string GamesGoToLibrary => "Anar a la biblioteca";
    public string HomeNews => "Novetats";
    public string HomeNotifications => "Notificacions";
    public string HomeLoading => "Carregant...";
    public string HomeNoContent => "Sense contingut";
    public string DownloadDialogTitle => "Confirmar descàrrega";
    public string DownloadDialogPath => "Ruta de descàrrega";
    public string DownloadDialogGameSize => "Mida";
    public string DownloadDialogFreeSpace => "Espai lliure";
    public string DownloadDialogViewPage => "Veure pàgina del joc";
    public string DownloadDialogNoDescription => "Sense descripció disponible.";
    public string DownloadDialogKey => "Clau per a versions especials (opcional)";
    public string DownloadKeyInvalidTitle => "Clau no vàlida";
    public string DownloadKeyInvalidMessage => "El format de la clau no és vàlid. Ha de tenir el format XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Clau consumida";
    public string DownloadKeyConsumedMessage => "Aquesta clau ja no és vàlida o ja s'ha utilitzat. Sol·licita una nova clau.";
    public string DownloadErrorTitle => "Ha fallat la descàrrega";
    public string DownloadErrorMessage => "No s'ha pogut completar la descàrrega. Si us plau, intenta-ho més tard. Si el problema persiste, escriu a #bugs-launcher en Discord.";
    public string BtnCancel => "Cancel·lar";
    public string CancelDownloadConfirmTitle => "Cancel·lar descàrrega";
    public string CancelDownloadConfirmMessage => "Estàs segur que vols cancel·lar la descàrrega? Els fitxers descarregats parcialment seran eliminats.";
    public string StatusExtracting => "Descomprimint...";
    public string StatusVerifying => "Comprovant integritat...";
    public string GameExeNotFoundTitle => "Joc no trobat";
    public string GameExeNotFoundMessage => "No s'ha trobat l'executable del joc. Prova a reinstal·lar-lo.";
    public string HashMismatchTitle => "Error d'integritat";
    public string HashMismatchMessage => "El fitxer descarregat està danyat o ha estat modificat. Si us plau, intenta-ho de nou. Si el problema persisteix, escriu a #bugs-launcher a Discord.";
    public string WelcomeDialogTitle => "Benvingut al Lostie Launcher!";
    public string WelcomeDialogDescription => "Descarrega, actualitza i juga els teus jocs favorits en un sol lloc. Simple, ràpid i sense complicacions.\n\nLa teva privacitat és important. No recollim dades personals. Només consultem informació bàsica del teu PC, com els nuclis del processador o la RAM disponible per a optimitzar els teus jocs.\n\nAquest projecte és opensource. Dubtes sobre com funciona? Consulta el codi font";
    public string WelcomeDialogContinue => "Continuar";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Canviar a versió especial";
    public string SpecialVersionDialogDescription => "En canviar a una versió especial no es perd res, funciona com una actualització normal.";
    public string SpecialVersionDialogKeyLabel => "Clau de versió especial";
    public string BtnConfirm => "Confirmar";
    public string DownloadKeyNotFoundTitle => "Clau no trobada";
    public string DownloadKeyNotFoundMessage => "No s'ha trobat cap versió especial amb aquesta clau. Comprova la clau i torna-ho a intentar.";
    public string DownloadKeyMismatchTitle => "Clau incorrecta";
    public string DownloadKeyMismatchMessage => "La clau no correspon a aquest joc.";
    public string TooltipSwitchSpecialVersion => "Canviar a versió especial";
    public string ServerActionsUnavailableTitle => "Servidor en manteniment";
    public string ServerActionsUnavailableMessage => "El servidor està en manteniment. Les descàrregues, actualitzacions i versions especials tornaran quan acabe.";
    public string OfflineModeLabel => "Mode offline";
    public string ServerMaintenanceNotificationTitle => "Servidor en manteniment";
    public string ServerMaintenanceNotificationMessage => "El launcher està en mode offline. Pots continuar veient els jocs instal·lats; les descàrregues i actualitzacions es reactivaran automàticament quan torne el servei.";
}

public class Eus : IStrings
{
    public string TitleHome => "Hasiera";
    public string TitleGames => "Nire Jokoak";
    public string TitleLibrary => "Liburutegia";
    public string TitleSettings => "Ezarpenak";
    public string BtnOk => "Ados";
    public string BtnYes => "Bai";
    public string BtnNo => "Ez";
    public string BtnDownload => "Deskargatu";
    public string BtnDownloading => "Deskargatzen...";
    public string BtnDownloaded => "Deskargatuta";
    public string BtnPause => "Pausatu";
    public string BtnResume => "Berrekin";
    public string BtnUpdate => "Eguneratu";
    public string BtnPlay => "Jolastu";
    public string TooltipOpenFolder => "Jokoaren karpeta ireki";
    public string TooltipOpenHelp => "Laguntza karpeta ireki";
    public string TooltipUninstall => "Jokoa desinstalatu";
    public string TooltipRefresh => "Freskatu";
    public string FolderNotFoundTitle => "Karpeta ez da aurkitu";
    public string FolderNotFoundMessage => "Jokoaren karpeta ez da aurkitu. Fitxategiak berrinstalatu nahi dituzu?";
    public string UninstallConfirmTitle => "Jokoa desinstalatu";
    public string UninstallConfirmMessage => "{0} desinstalatu nahi duzu? Gordetako partidak eta jolasdenbora ez dira galduko.";
    public string UninstallNotFoundTitle => "Fitxategiak ez dira aurkitu";
    public string UninstallNotFoundMessage => "Jokoaren fitxategiak ez dira aurkitu, baina zerrenda garbi utzi da.";
    public string UninstallErrorTitle => "Desinstalazio errorea";
    public string UninstallErrorMessage => "Ezin izan da jokoa desinstalatu. Baliteke fitxategi batzuk erabilita egotea.";
    public string UpdateAvailableTitle => "Eguneraketa eskuragarri";
    public string UpdateAvailableMessage => "{0} bertsio berria eskuragarri. Berrabiarazi eguneratzeko?";
    public string SettingsGeneral => "Orokorra";
    public string SettingsAppearance => "Itxura";
    public string SettingsStartWithWindows => "Windows-ekin hasi";
    public string SettingsStartMinimized => "Minimizatuta hasi";
    public string SettingsAutoUpdate => "Eguneraketa automatikoak";
    public string SettingsLanguage => "Hizkuntza";
    public string SettingsDownloadDir => "Deskarga direktorioa";
    public string SettingsTheme => "Gaia";
    public string BtnBrowse => "Arakatu...";
    public string SettingsCheckForUpdates => "Launcher-aren eguneraketak bilatu";
    public string CheckForUpdatesTitle => "Eguneraketak bilatu";
    public string CheckForUpdatesMessage => "Eguneraketak bilatzeak launcher-a berrabiarazi behar du. Deskarga bat abian badago, hondatu edo eten daiteke. Jarraitu nahi duzu?";
    public string ChangeDownloadDirTitle => "Deskarga direktorioa aldatu";
    public string ChangeDownloadDirMessage => "Jokoak instalatuta badituzu, eskuz mugitu beharko dituzu bide berrira, edo launcher-ak ez ditu ezagutuko. Jarraitu nahi duzu?";
    public string TrayOpen => "Ireki";
    public string TrayExit => "Irten";
    public string LibraryNoContent => "Ez dago eskuragarri";
    public string GamesNoContent => "Ez daukazu jokorik instalatuta";
    public string GamesGoToLibrary => "Liburutegira joan";
    public string HomeNews => "Berriak";
    public string HomeNotifications => "Jakinarazpenak";
    public string HomeLoading => "Kargatzen...";
    public string HomeNoContent => "Eduki gabe";
    public string DownloadDialogTitle => "Deskarga berretsi";
    public string DownloadDialogPath => "Deskarga bidea";
    public string DownloadDialogGameSize => "Tamaina";
    public string DownloadDialogFreeSpace => "Leku librea";
    public string DownloadDialogViewPage => "Jokoaren orria ikusi";
    public string DownloadDialogNoDescription => "Deskribapenik ez.";
    public string DownloadDialogKey => "Bertsio berezietarako gakoa (aukerakoa)";
    public string DownloadKeyInvalidTitle => "Gako baliogabea";
    public string DownloadKeyInvalidMessage => "Gakoaren formatua ez da baliozkoa. Formatua XXXX-XXXX-XXXX-XXXX-XXXX izan behar da.";
    public string DownloadKeyConsumedTitle => "Gakoa kontsumituta";
    public string DownloadKeyConsumedMessage => "Gako hau baliogabea da edo dagoeneko erabili da. Eskatu gako berri bat.";
    public string DownloadErrorTitle => "Deskargetak huts egin du";
    public string DownloadErrorMessage => "Deskargetak ezin izan du osatu. Mesedez, geroago saiatu. Arazoa jarraitzen badu, idatzi #bugs-launcher kanalean Discord-en.";
    public string BtnCancel => "Utzi";
    public string CancelDownloadConfirmTitle => "Deskarga utzi";
    public string CancelDownloadConfirmMessage => "Ziur zaude deskarga utzi nahi duzula? Partzialki deskargatutako fitxategiak ezabatuko dira.";
    public string StatusExtracting => "Deskonprimatzen...";
    public string StatusVerifying => "Osotasuna egiaztatzen...";
    public string GameExeNotFoundTitle => "Jokoa ez da aurkitu";
    public string GameExeNotFoundMessage => "Jokoaren exekutagarria ez da aurkitu. Saiatu berrinstalatzea.";
    public string HashMismatchTitle => "Osotasun errorea";
    public string HashMismatchMessage => "Deskargatutako fitxategia hondatuta edo aldatuta dago. Mesedez, saiatu berriro. Arazoa jarraitzen badu, idatzi #bugs-launcher kanalean Discord-en.";
    public string WelcomeDialogTitle => "Ongi etorri Lostie Launcher-era!";
    public string WelcomeDialogDescription => "Deskargatu, eguneratu eta jolastu zure joko gogokoak leku batean. Sinplea, azkarra eta konplikazio gabe.\n\nZure pribatutasuna garrantzitsua da. Ez dugu datu pertsonalik biltzen. PCaren oinarrizko informazioa bakarrik kontsultatzen dugu, prozesadoreko nukleak edo eskuragarri dagoen RAMa adibidez, zure jokoak optimizatzeko.\n\nProiektu hau opensource. Zalantzak nola funtzionatzen duen jakin nahi? Bilatu iturburu kodea";
    public string WelcomeDialogContinue => "Jarraitu";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Bertsio berezira aldatu";
    public string SpecialVersionDialogDescription => "Bertsio berezi batera aldatzean ez da ezer galtzen, eguneraketa normal bat bezala funtzionatzen du.";
    public string SpecialVersionDialogKeyLabel => "Bertsio bereziaren gakoa";
    public string BtnConfirm => "Berretsi";
    public string DownloadKeyNotFoundTitle => "Gakoa ez da aurkitu";
    public string DownloadKeyNotFoundMessage => "Ez da gako honekin bertsio berezirik aurkitu. Egiaztatu gakoa eta saiatu berriro.";
    public string DownloadKeyMismatchTitle => "Gako okerra";
    public string DownloadKeyMismatchMessage => "Gakoa ez dator bat joko honekin.";
    public string TooltipSwitchSpecialVersion => "Bertsio berezira aldatu";
    public string ServerActionsUnavailableTitle => "Zerbitzaria mantentze-lanetan";
    public string ServerActionsUnavailableMessage => "Zerbitzaria mantentze-lanetan dago. Deskargak, eguneraketak eta bertsio bereziak amaitzean itzuliko dira.";
    public string OfflineModeLabel => "Offline modua";
    public string ServerMaintenanceNotificationTitle => "Zerbitzaria mantentze-lanetan";
    public string ServerMaintenanceNotificationMessage => "Launcher-a offline moduan dago. Instalatutako jokoak ikusten jarrai dezakezu; deskargak eta eguneraketak automatikoki berraktibatuko dira zerbitzua itzultzen denean.";
}

public class Gal : IStrings
{
    public string TitleHome => "Inicio";
    public string TitleGames => "Os meus xogos";
    public string TitleLibrary => "Biblioteca";
    public string TitleSettings => "Axustes";
    public string BtnOk => "Aceptar";
    public string BtnYes => "Si";
    public string BtnNo => "Non";
    public string BtnDownload => "Descargar";
    public string BtnDownloading => "Descargando...";
    public string BtnDownloaded => "Descargado";
    public string BtnPause => "Pausar";
    public string BtnResume => "Retomar";
    public string BtnUpdate => "Actualizar";
    public string BtnPlay => "Xogar";
    public string TooltipOpenFolder => "Abrir cartafol do xogo";
    public string TooltipOpenHelp => "Abrir cartafol de axuda";
    public string TooltipUninstall => "Desinstalar xogo";
    public string TooltipRefresh => "Actualizar";
    public string FolderNotFoundTitle => "Cartafol non atopado";
    public string FolderNotFoundMessage => "O cartafol do xogo non foi atopado. Queres reinstalar os ficheiros?";
    public string UninstallConfirmTitle => "Desinstalar xogo";
    public string UninstallConfirmMessage => "Seguro que queres desinstalar {0}? As partidas gardadas e o rexistro de tempo xogado non se perderán.";
    public string UninstallNotFoundTitle => "Ficheiros non atopados";
    public string UninstallNotFoundMessage => "Non se atoparon os ficheiros do xogo, pero limpouse o rexistro da lista.";
    public string UninstallErrorTitle => "Erro ao desinstalar";
    public string UninstallErrorMessage => "Non se puido desinstalar o xogo. É posible que algúns ficheiros estean en uso.";
    public string UpdateAvailableTitle => "Actualización dispoñible";
    public string UpdateAvailableMessage => "Nova versión {0} dispoñible. Reiniciar para actualizar?";
    public string SettingsGeneral => "Xeral";
    public string SettingsAppearance => "Aparencia";
    public string SettingsStartWithWindows => "Iniciar con Windows";
    public string SettingsStartMinimized => "Iniciar minimizado";
    public string SettingsAutoUpdate => "Actualizacións automáticas";
    public string SettingsLanguage => "Idioma";
    public string SettingsDownloadDir => "Directorio de descargas";
    public string SettingsTheme => "Tema";
    public string BtnBrowse => "Examinar...";
    public string SettingsCheckForUpdates => "Buscar actualizacións do launcher";
    public string CheckForUpdatesTitle => "Buscar actualizacións";
    public string CheckForUpdatesMessage => "Buscar actualizacións require reiniciar o launcher. Se hai algunha descarga en progreso, podería danarse ou interromperse. Desexas continuar?";
    public string ChangeDownloadDirTitle => "Cambiar directorio de descargas";
    public string ChangeDownloadDirMessage => "Se tes xogos instalados, terás que movelos manualmente á nova ruta ou o launcher non os recoñecerá. Desexas continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Saír";
    public string LibraryNoContent => "Non dispoñible";
    public string GamesNoContent => "Non tes ningún xogo instalado";
    public string GamesGoToLibrary => "Ir á biblioteca";
    public string HomeNews => "Novidades";
    public string HomeNotifications => "Notificacións";
    public string HomeLoading => "Cargando...";
    public string HomeNoContent => "Sen contido";
    public string DownloadDialogTitle => "Confirmar descarga";
    public string DownloadDialogPath => "Ruta de descarga";
    public string DownloadDialogGameSize => "Tamaño";
    public string DownloadDialogFreeSpace => "Espazo libre";
    public string DownloadDialogViewPage => "Ver páxina do xogo";
    public string DownloadDialogNoDescription => "Sen descrición dispoñible.";
    public string DownloadDialogKey => "Clave para versións especiais (opcional)";
    public string DownloadKeyInvalidTitle => "Clave non válida";
    public string DownloadKeyInvalidMessage => "O formato da clave non é válido. Debe ter o formato XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Clave consumida";
    public string DownloadKeyConsumedMessage => "Esta clave xa non é válida ou xa foi utilizada. Solicita unha nova clave.";
    public string DownloadErrorTitle => "Erro na descarga";
    public string DownloadErrorMessage => "Non foi posible completar a descarga. Inténtao de novo máis tarde. Se o problema persiste, escribe en #bugs-launcher en Discord.";
    public string BtnCancel => "Cancelar";
    public string CancelDownloadConfirmTitle => "Cancelar descarga";
    public string CancelDownloadConfirmMessage => "Seguro que queres cancelar a descarga? Os ficheiros parcialmente descargados serán eliminados.";
    public string StatusExtracting => "Descomprimindo...";
    public string StatusVerifying => "Comprobando integridade...";
    public string GameExeNotFoundTitle => "Xogo non atopado";
    public string GameExeNotFoundMessage => "Non se atopou o executable do xogo. Intenta reinstalalo.";
    public string HashMismatchTitle => "Erro de integridade";
    public string HashMismatchMessage => "O ficheiro descargado está danado ou foi modificado. Por favor, téntao de novo. Se o problema persiste, escribe en #bugs-launcher en Discord.";
    public string WelcomeDialogTitle => "Benvido ao Lostie Launcher!";
    public string WelcomeDialogDescription => "Descarga, actualiza e xoga os teus xogos favoritos nun só lugar. Simple, rápido e sen complicacións.\n\nA túa privacidade é importante. Non recollemos datos persoais. Só consultamos información básica do teu PC, como os núcleos do procesador ou a RAM dispoñible para optimizar os teus xogos.\n\nEste proxecto é opensource. Dúbidas sobre como funciona? Consulta o código fonte";
    public string WelcomeDialogContinue => "Continuar";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Cambiar a versión especial";
    public string SpecialVersionDialogDescription => "Ao cambiar a unha versión especial non se perde nada, funciona como unha actualización normal.";
    public string SpecialVersionDialogKeyLabel => "Clave de versión especial";
    public string BtnConfirm => "Confirmar";
    public string DownloadKeyNotFoundTitle => "Clave non atopada";
    public string DownloadKeyNotFoundMessage => "Non se atopou ningunha versión especial con esta clave. Comproba a clave e téntao de novo.";
    public string DownloadKeyMismatchTitle => "Clave incorrecta";
    public string DownloadKeyMismatchMessage => "A clave non corresponde a este xogo.";
    public string TooltipSwitchSpecialVersion => "Cambiar a versión especial";
    public string ServerActionsUnavailableTitle => "Servidor en mantemento";
    public string ServerActionsUnavailableMessage => "O servidor está en mantemento. As descargas, actualizacións e versións especiais volverán cando remate.";
    public string OfflineModeLabel => "Modo offline";
    public string ServerMaintenanceNotificationTitle => "Servidor en mantemento";
    public string ServerMaintenanceNotificationMessage => "O launcher está en modo offline. Podes seguir vendo os teus xogos instalados; as descargas e actualizacións reactivaranse automaticamente cando volva o servizo.";
}

public class Por : IStrings
{
    public string TitleHome => "Início";
    public string TitleGames => "Meus Jogos";
    public string TitleLibrary => "Biblioteca";
    public string TitleSettings => "Configurações";
    public string BtnOk => "OK";
    public string BtnYes => "Sim";
    public string BtnNo => "Não";
    public string BtnDownload => "Baixar";
    public string BtnDownloading => "Baixando...";
    public string BtnDownloaded => "Baixado";
    public string BtnPause => "Pausar";
    public string BtnResume => "Retomar";
    public string BtnUpdate => "Atualizar";
    public string BtnPlay => "Jogar";
    public string TooltipOpenFolder => "Abrir pasta do jogo";
    public string TooltipOpenHelp => "Abrir pasta de ajuda";
    public string TooltipUninstall => "Desinstalar jogo";
    public string TooltipRefresh => "Atualizar";
    public string FolderNotFoundTitle => "Pasta não encontrada";
    public string FolderNotFoundMessage => "A pasta do jogo não foi encontrada. Deseja reinstalar os arquivos?";
    public string UninstallConfirmTitle => "Desinstalar jogo";
    public string UninstallConfirmMessage => "Tem certeza que deseja desinstalar {0}? Seus saves e o registro de tempo de jogo não serão perdidos.";
    public string UninstallNotFoundTitle => "Arquivos não encontrados";
    public string UninstallNotFoundMessage => "Os arquivos do jogo não foram encontrados, mas o registro foi limpo da lista.";
    public string UninstallErrorTitle => "Erro ao desinstalar";
    public string UninstallErrorMessage => "Não foi possível desinstalar o jogo. Alguns arquivos podem estar em uso.";
    public string UpdateAvailableTitle => "Atualização disponível";
    public string UpdateAvailableMessage => "Nova versão {0} disponível. Reiniciar para atualizar?";
    public string SettingsGeneral => "Geral";
    public string SettingsAppearance => "Aparência";
    public string SettingsStartWithWindows => "Iniciar com o Windows";
    public string SettingsStartMinimized => "Iniciar minimizado";
    public string SettingsAutoUpdate => "Atualizações automáticas";
    public string SettingsLanguage => "Idioma";
    public string SettingsDownloadDir => "Diretório de downloads";
    public string SettingsTheme => "Tema";
    public string BtnBrowse => "Procurar...";
    public string SettingsCheckForUpdates => "Verificar atualizações do launcher";
    public string CheckForUpdatesTitle => "Verificar atualizações";
    public string CheckForUpdatesMessage => "Verificar atualizações requer reiniciar o launcher. Se houver algum download em andamento, ele pode ser interrompido ou corrompido. Deseja continuar?";
    public string ChangeDownloadDirTitle => "Alterar diretório de downloads";
    public string ChangeDownloadDirMessage => "Se você tiver jogos instalados, precisará movê-los manualmente para o novo caminho ou o launcher não os reconhecerá. Deseja continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Sair";
    public string LibraryNoContent => "Não disponível";
    public string GamesNoContent => "Nenhum jogo instalado";
    public string GamesGoToLibrary => "Ir à biblioteca";
    public string HomeNews => "Novidades";
    public string HomeNotifications => "Notificações";
    public string HomeLoading => "Carregando...";
    public string HomeNoContent => "Sem conteúdo";
    public string DownloadDialogTitle => "Confirmar download";
    public string DownloadDialogPath => "Caminho de download";
    public string DownloadDialogGameSize => "Tamanho";
    public string DownloadDialogFreeSpace => "Espaço livre";
    public string DownloadDialogViewPage => "Ver página do jogo";
    public string DownloadDialogNoDescription => "Sem descrição disponível.";
    public string DownloadDialogKey => "Chave para versões especiais (opcional)";
    public string DownloadKeyInvalidTitle => "Chave inválida";
    public string DownloadKeyInvalidMessage => "O formato da chave é inválido. Deve seguir o formato XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Chave consumida";
    public string DownloadKeyConsumedMessage => "Esta chave é inválida ou já foi utilizada. Solicite uma nova chave.";
    public string DownloadErrorTitle => "Falha no download";
    public string DownloadErrorMessage => "O download não pôde ser concluído. Por favor, tente mais tarde. Se o problema persistir, escreva em #bugs-launcher no Discord.";
    public string BtnCancel => "Cancelar";
    public string CancelDownloadConfirmTitle => "Cancelar download";
    public string CancelDownloadConfirmMessage => "Tem certeza que deseja cancelar o download? Os arquivos parcialmente baixados serão excluídos.";
    public string StatusExtracting => "Descomprimindo...";
    public string StatusVerifying => "Verificando integridade...";
    public string GameExeNotFoundTitle => "Jogo não encontrado";
    public string GameExeNotFoundMessage => "O executável do jogo não foi encontrado. Tente reinstalá-lo.";
    public string HashMismatchTitle => "Erro de integridade";
    public string HashMismatchMessage => "O arquivo baixado está corrompido ou foi modificado. Por favor, tente novamente. Se o problema persistir, escreva em #bugs-launcher no Discord.";
    public string WelcomeDialogTitle => "Bem-vindo ao Lostie Launcher!";
    public string WelcomeDialogDescription => "Baixe, atualize e jogue seus jogos favoritos em um único lugar. Simples, rápido e sem complicações.\n\nSua privacidade é importante. Não coletamos dados pessoais. Apenas consultamos informações básicas do seu PC, como núcleos do processador ou RAM disponível para otimizar seus jogos.\n\nEste projeto é open source. Dúvidas sobre como funciona? Consulte o código-fonte";
    public string WelcomeDialogContinue => "Continuar";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Mudar para versão especial";
    public string SpecialVersionDialogDescription => "Ao mudar para uma versão especial não se perde nada, funciona como uma atualização normal.";
    public string SpecialVersionDialogKeyLabel => "Chave de versão especial";
    public string BtnConfirm => "Confirmar";
    public string DownloadKeyNotFoundTitle => "Chave não encontrada";
    public string DownloadKeyNotFoundMessage => "Não foi encontrada nenhuma versão especial com esta chave. Verifique a chave e tente novamente.";
    public string DownloadKeyMismatchTitle => "Chave incorreta";
    public string DownloadKeyMismatchMessage => "A chave não corresponde a este jogo.";
    public string TooltipSwitchSpecialVersion => "Mudar para versão especial";
    public string ServerActionsUnavailableTitle => "Servidor em manutenção";
    public string ServerActionsUnavailableMessage => "O servidor está em manutenção. Downloads, atualizações e versões especiais voltarão assim que terminar.";
    public string OfflineModeLabel => "Modo offline";
    public string ServerMaintenanceNotificationTitle => "Servidor em manutenção";
    public string ServerMaintenanceNotificationMessage => "O launcher está em modo offline. Você pode continuar vendo seus jogos instalados; downloads e atualizações serão reativados automaticamente quando o serviço voltar.";
}

public class Val : IStrings
{
    public string TitleHome => "Inici";
    public string TitleGames => "Els meus jocs";
    public string TitleLibrary => "Biblioteca";
    public string TitleSettings => "Ajustos";
    public string BtnOk => "Acceptar";
    public string BtnYes => "Sí";
    public string BtnNo => "No";
    public string BtnDownload => "Descarregar";
    public string BtnDownloading => "Descarregant...";
    public string BtnDownloaded => "Descarregat";
    public string BtnPause => "Pausar";
    public string BtnResume => "Reprendre";
    public string BtnUpdate => "Actualitzar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Obrir carpeta del joc";
    public string TooltipOpenHelp => "Obrir carpeta d'ajuda";
    public string TooltipUninstall => "Desinstal·lar joc";
    public string TooltipRefresh => "Actualitzar";
    public string FolderNotFoundTitle => "Carpeta no trobada";
    public string FolderNotFoundMessage => "La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers?";
    public string UninstallConfirmTitle => "Desinstal·lar joc";
    public string UninstallConfirmMessage => "Estàs segur que vols desinstal·lar {0}? Les teues partides guardades i el registre de temps jugat no es perdran.";
    public string UninstallNotFoundTitle => "Fitxers no trobats";
    public string UninstallNotFoundMessage => "No s'han trobat els fitxers del joc, però s'ha netejat el registre de la llista.";
    public string UninstallErrorTitle => "Error en desinstal·lar";
    public string UninstallErrorMessage => "No s'ha pogut desinstal·lar el joc. És possible que alguns fitxers estiguen en ús.";
    public string UpdateAvailableTitle => "Actualització disponible";
    public string UpdateAvailableMessage => "Nova versió {0} disponible. Reiniciar per a actualitzar?";
    public string SettingsGeneral => "General";
    public string SettingsAppearance => "Aparença";
    public string SettingsStartWithWindows => "Iniciar amb Windows";
    public string SettingsStartMinimized => "Iniciar minimitzat";
    public string SettingsAutoUpdate => "Actualitzacions automàtiques";
    public string SettingsLanguage => "Idioma";
    public string SettingsDownloadDir => "Directori de descàrregues";
    public string SettingsTheme => "Tema";
    public string BtnBrowse => "Explorar...";
    public string SettingsCheckForUpdates => "Buscar actualitzacions del launcher";
    public string CheckForUpdatesTitle => "Buscar actualitzacions";
    public string CheckForUpdatesMessage => "Buscar actualitzacions requereix reiniciar el launcher. Si hi ha alguna descàrrega en curs, podria danyar-se o interrompre's. Vols continuar?";
    public string ChangeDownloadDirTitle => "Canviar directori de descàrregues";
    public string ChangeDownloadDirMessage => "Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar?";
    public string TrayOpen => "Obrir";
    public string TrayExit => "Eixir";
    public string LibraryNoContent => "No disponible";
    public string GamesNoContent => "No tens cap joc instal·lat";
    public string GamesGoToLibrary => "Anar a la biblioteca";
    public string HomeNews => "Novetats";
    public string HomeNotifications => "Notificacions";
    public string HomeLoading => "Carregant...";
    public string HomeNoContent => "Sense contingut";
    public string DownloadDialogTitle => "Confirmar descàrrega";
    public string DownloadDialogPath => "Ruta de descàrrega";
    public string DownloadDialogGameSize => "Grandària";
    public string DownloadDialogFreeSpace => "Espai lliure";
    public string DownloadDialogViewPage => "Veure pàgina del joc";
    public string DownloadDialogNoDescription => "Sense descripció disponible.";
    public string DownloadDialogKey => "Clau per a versions especials (opcional)";
    public string DownloadKeyInvalidTitle => "Clau no vàlida";
    public string DownloadKeyInvalidMessage => "El format de la clau no és vàlid. Ha de tindre el format XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Clau consumida";
    public string DownloadKeyConsumedMessage => "Esta clau ja no és vàlida o ja s'ha utilitzat. Sol·licita una nova clau.";
    public string DownloadErrorTitle => "Ha fallat la descàrrega";
    public string DownloadErrorMessage => "No s'ha pogut completar la descàrrega. Si us plau, intenta-ho més tard. Si el problema persiste, escriu a #bugs-launcher en Discord.";
    public string BtnCancel => "Cancel·lar";
    public string CancelDownloadConfirmTitle => "Cancel·lar descàrrega";
    public string CancelDownloadConfirmMessage => "Estàs segur que vols cancel·lar la descàrrega? Els fitxers descarregats parcialment seran eliminats.";
    public string StatusExtracting => "Descomprimint...";
    public string StatusVerifying => "Comprovant integritat...";
    public string GameExeNotFoundTitle => "Joc no trobat";
    public string GameExeNotFoundMessage => "No s'ha trobat l'executable del joc. Prova a reinstal·lar-lo.";
    public string HashMismatchTitle => "Error d'integritat";
    public string HashMismatchMessage => "El fitxer descarregat està danyat o ha sigut modificat. Si us plau, intenta-ho de nou. Si el problema persisteix, escriu a #bugs-launcher a Discord.";
    public string WelcomeDialogTitle => "Benvingut al Lostie Launcher!";
    public string WelcomeDialogDescription => "Descarrega, actualitza i juga els teus jocs favorits en un sol lloc. Simple, ràpid i sense complicacions.\n\nLa teva privacitat és important. No recollim dades personals. Només consultem informació bàsica del teu PC, com els nuclis del processador o la RAM disponible per a optimitzar els teus jocs.\n\nEst projecte és opensource. Dubtes sobre com funciona? Consulta el codi font";
    public string WelcomeDialogContinue => "Continuar";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Canviar a versió especial";
    public string SpecialVersionDialogDescription => "En canviar a una versió especial no es perd res, funciona com una actualització normal.";
    public string SpecialVersionDialogKeyLabel => "Clau de versió especial";
    public string BtnConfirm => "Confirmar";
    public string DownloadKeyNotFoundTitle => "Clau no trobada";
    public string DownloadKeyNotFoundMessage => "No s'ha trobat cap versió especial amb esta clau. Comprova la clau i torna-ho a intentar.";
    public string DownloadKeyMismatchTitle => "Clau incorrecta";
    public string DownloadKeyMismatchMessage => "La clau no correspon a este joc.";
    public string TooltipSwitchSpecialVersion => "Canviar a versió especial";
    public string ServerActionsUnavailableTitle => "Servidor en manteniment";
    public string ServerActionsUnavailableMessage => "El servidor està en manteniment. Les descàrregues, actualitzacions i versions especials tornaran quan acabe.";
    public string OfflineModeLabel => "Mode offline";
    public string ServerMaintenanceNotificationTitle => "Servidor en manteniment";
    public string ServerMaintenanceNotificationMessage => "El launcher està en mode offline. Pots continuar veient els jocs instal·lats; les descàrregues i actualitzacions es reactivaran automàticament quan torne el servei.";
}

public class Fra : IStrings
{
    public string TitleHome => "Accueil";
    public string TitleGames => "Mes jeux";
    public string TitleLibrary => "Bibliothèque";
    public string TitleSettings => "Paramètres";
    public string BtnOk => "OK";
    public string BtnYes => "Oui";
    public string BtnNo => "Non";
    public string BtnDownload => "Télécharger";
    public string BtnDownloading => "Téléchargement...";
    public string BtnDownloaded => "Téléchargé";
    public string BtnPause => "Pause";
    public string BtnResume => "Reprendre";
    public string BtnUpdate => "Mettre à jour";
    public string BtnPlay => "Jouer";
    public string TooltipOpenFolder => "Ouvrir le dossier du jeu";
    public string TooltipOpenHelp => "Ouvrir le dossier d'aide";
    public string TooltipUninstall => "Désinstaller le jeu";
    public string TooltipRefresh => "Actualiser";
    public string FolderNotFoundTitle => "Dossier non trouvé";
    public string FolderNotFoundMessage => "Le dossier du jeu n'a pas été trouvé. Voulez-vous réinstaller les fichiers ?";
    public string UninstallConfirmTitle => "Désinstaller le jeu";
    public string UninstallConfirmMessage => "Êtes-vous sûr de vouloir désinstaller {0} ? Vos sauvegardes et le registre de temps de jeu ne seront pas perdus.";
    public string UninstallNotFoundTitle => "Fichiers non trouvés";
    public string UninstallNotFoundMessage => "Les fichiers du jeu n'ont pas été trouvés, mais l'entrée a été supprimée de la liste.";
    public string UninstallErrorTitle => "Erreur de désinstallation";
    public string UninstallErrorMessage => "Impossible de désinstaller le jeu. Certains fichiers peuvent être en cours d'utilisation.";
    public string UpdateAvailableTitle => "Mise à jour disponible";
    public string UpdateAvailableMessage => "Nouvelle version {0} disponible. Redémarrer pour mettre à jour ?";
    public string SettingsGeneral => "Général";
    public string SettingsAppearance => "Apparence";
    public string SettingsStartWithWindows => "Démarrer avec Windows";
    public string SettingsStartMinimized => "Démarrer minimisé";
    public string SettingsAutoUpdate => "Mises à jour automatiques";
    public string SettingsLanguage => "Langue";
    public string SettingsDownloadDir => "Répertoire de téléchargement";
    public string SettingsTheme => "Thème";
    public string BtnBrowse => "Parcourir...";
    public string SettingsCheckForUpdates => "Vérifier les mises à jour du launcher";
    public string CheckForUpdatesTitle => "Vérifier les mises à jour";
    public string CheckForUpdatesMessage => "La vérification des mises à jour nécessite de redémarrer le lanceur. Tout téléchargement en cours peut être interrompu ou corrompu. Voulez-vous continuer ?";
    public string ChangeDownloadDirTitle => "Modifier le répertoire de téléchargement";
    public string ChangeDownloadDirMessage => "Si vous avez des jeux installés, vous devrez les déplacer manuellement vers le nouveau chemin ou le lanceur ne les reconnaîtra pas. Voulez-vous continuer ?";
    public string TrayOpen => "Ouvrir";
    public string TrayExit => "Quitter";
    public string LibraryNoContent => "Non disponible";
    public string GamesNoContent => "Aucun jeu installé";
    public string GamesGoToLibrary => "Aller à la bibliothèque";
    public string HomeNews => "Actualités";
    public string HomeNotifications => "Notifications";
    public string HomeLoading => "Chargement...";
    public string HomeNoContent => "Aucun contenu";
    public string DownloadDialogTitle => "Confirmer le téléchargement";
    public string DownloadDialogPath => "Chemin de téléchargement";
    public string DownloadDialogGameSize => "Taille";
    public string DownloadDialogFreeSpace => "Espace libre";
    public string DownloadDialogViewPage => "Voir la page du jeu";
    public string DownloadDialogNoDescription => "Aucune description disponible.";
    public string DownloadDialogKey => "Clé pour les versions spéciales (optionnel)";
    public string DownloadKeyInvalidTitle => "Clé invalide";
    public string DownloadKeyInvalidMessage => "Le format de la clé est invalide. Il doit suivre le format XXXX-XXXX-XXXX-XXXX-XXXX.";
    public string DownloadKeyConsumedTitle => "Clé consommée";
    public string DownloadKeyConsumedMessage => "Cette clé est invalide ou a déjà été utilisée. Veuillez demander une nouvelle clé.";
    public string DownloadErrorTitle => "Échec du téléchargement";
    public string DownloadErrorMessage => "Le téléchargement n'a pas pu être complété. Veuillez réessayer plus tard. Si le problème persiste, écrivez dans #bugs-launcher sur Discord.";
    public string BtnCancel => "Annuler";
    public string CancelDownloadConfirmTitle => "Annuler le téléchargement";
    public string CancelDownloadConfirmMessage => "Êtes-vous sûr de vouloir annuler le téléchargement ? Les fichiers partiellement téléchargés seront supprimés.";
    public string StatusExtracting => "Extraction en cours...";
    public string StatusVerifying => "Vérification de l'intégrité...";
    public string GameExeNotFoundTitle => "Jeu non trouvé";
    public string GameExeNotFoundMessage => "L'exécutable du jeu est introuvable. Essayez de le réinstaller.";
    public string HashMismatchTitle => "Erreur d'intégrité";
    public string HashMismatchMessage => "Le fichier téléchargé est corrompu ou a été modifié. Veuillez réessayer. Si le problème persiste, écrivez dans #bugs-launcher sur Discord.";
    public string WelcomeDialogTitle => "Bienvenue dans Lostie Launcher !";
    public string WelcomeDialogDescription => "Téléchargez, mettez à jour et jouez à vos jeux préférés en un seul endroit. Simple, rapide et sans tracas.\n\nVotre vie privée compte. Nous ne collectons pas de données personnelles. Nous consultons uniquement des informations basiques sur votre PC, comme les cœurs du processeur ou la RAM disponible pour optimiser vos jeux.\n\nCe projet est open source. Des questions sur le fonctionnement ? Consultez le code source";
    public string WelcomeDialogContinue => "Continuer";
    public string RepositoryUrl => "https://github.com/jagobainda/LostieLauncher";
    public string SpecialVersionDialogTitle => "Passer à la version spéciale";
    public string SpecialVersionDialogDescription => "En passant à une version spéciale, vous ne perdez rien, cela fonctionne comme une mise à jour normale.";
    public string SpecialVersionDialogKeyLabel => "Clé de version spéciale";
    public string BtnConfirm => "Confirmer";
    public string DownloadKeyNotFoundTitle => "Clé non trouvée";
    public string DownloadKeyNotFoundMessage => "Aucune version spéciale n'a été trouvée avec cette clé. Vérifiez la clé et réessayez.";
    public string DownloadKeyMismatchTitle => "Clé incorrecte";
    public string DownloadKeyMismatchMessage => "La clé ne correspond pas à ce jeu.";
    public string TooltipSwitchSpecialVersion => "Passer à la version spéciale";
    public string ServerActionsUnavailableTitle => "Serveur en maintenance";
    public string ServerActionsUnavailableMessage => "Le serveur est en maintenance. Les téléchargements, mises à jour et versions spéciales reviendront dès que ce sera terminé.";
    public string OfflineModeLabel => "Mode hors ligne";
    public string ServerMaintenanceNotificationTitle => "Serveur en maintenance";
    public string ServerMaintenanceNotificationMessage => "Le launcher est en mode hors ligne. Vous pouvez continuer à voir vos jeux installés ; les téléchargements et mises à jour se réactiveront automatiquement au retour du service.";
}
