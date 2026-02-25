namespace EricLostieLauncher.Content;

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
    string BtnUpdate { get; }
    string BtnPlay { get; }
    string TooltipOpenFolder { get; }
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
    public string BtnUpdate => "Actualizar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Abrir carpeta del juego";
    public string TooltipUninstall => "Desinstalar juego";
    public string TooltipRefresh => "Actualizar";
    public string FolderNotFoundTitle => "Carpeta no encontrada";
    public string FolderNotFoundMessage => "La carpeta del juego no se encontró. ¿Quieres reinstalar los archivos?";
    public string UninstallConfirmTitle => "Desinstalar juego";
    public string UninstallConfirmMessage => "¿Seguro que quieres desinstalar {0}? Esta acción no se puede deshacer.";
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
    public string SettingsCheckForUpdates => "Buscar actualizaciones";
    public string CheckForUpdatesTitle => "Buscar actualizaciones";
    public string CheckForUpdatesMessage => "Buscar actualizaciones requiere reiniciar el launcher. Si hay alguna descarga en progreso, podría dañarse o interrumpirse. ¿Deseas continuar?";
    public string ChangeDownloadDirTitle => "Cambiar directorio de descargas";
    public string ChangeDownloadDirMessage => "Si tienes juegos instalados, tendrás que moverlos manualmente a la nueva ruta o el launcher no los reconocerá. ¿Deseas continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Salir";
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
    public string BtnUpdate => "Update";
    public string BtnPlay => "Play";
    public string TooltipOpenFolder => "Open game folder";
    public string TooltipUninstall => "Uninstall game";
    public string TooltipRefresh => "Refresh";
    public string FolderNotFoundTitle => "Folder not found";
    public string FolderNotFoundMessage => "The game folder was not found. Do you want to reinstall the files?";
    public string UninstallConfirmTitle => "Uninstall game";
    public string UninstallConfirmMessage => "Are you sure you want to uninstall {0}? This action cannot be undone.";
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
    public string SettingsCheckForUpdates => "Check for updates";
    public string CheckForUpdatesTitle => "Check for updates";
    public string CheckForUpdatesMessage => "Checking for updates requires restarting the launcher. Any ongoing download may be interrupted or corrupted. Do you want to continue?";
    public string ChangeDownloadDirTitle => "Change download directory";
    public string ChangeDownloadDirMessage => "If you have installed games, you will need to move them manually to the new path or the launcher won't recognize them. Do you want to continue?";
    public string TrayOpen => "Open";
    public string TrayExit => "Exit";
}