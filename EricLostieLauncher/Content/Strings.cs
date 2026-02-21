namespace EricLostieLauncher.Content;

public interface IStrings
{
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
}

public class Esp : IStrings
{
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
}

public class Eng : IStrings
{
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
}