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
    string LibraryNoContent { get; }
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
    string BtnCancel { get; }
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
    public string LibraryNoContent => "No disponible";
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
    public string BtnCancel => "Cancelar";
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
    public string LibraryNoContent => "Not available";
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
    public string BtnCancel => "Cancel";
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
    public string BtnUpdate => "Actualitzar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Obrir carpeta del joc";
    public string TooltipUninstall => "Desinstal·lar joc";
    public string TooltipRefresh => "Actualitzar";
    public string FolderNotFoundTitle => "Carpeta no trobada";
    public string FolderNotFoundMessage => "La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers?";
    public string UninstallConfirmTitle => "Desinstal·lar joc";
    public string UninstallConfirmMessage => "Estàs segur que vols desinstal·lar {0}? Aquesta acció no es pot desfer.";
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
    public string SettingsCheckForUpdates => "Buscar actualitzacions";
    public string CheckForUpdatesTitle => "Buscar actualitzacions";
    public string CheckForUpdatesMessage => "Buscar actualitzacions requereix reiniciar el launcher. Si hi ha alguna descàrrega en curs, podria danyar-se o interrompre's. Vols continuar?";
    public string ChangeDownloadDirTitle => "Canviar directori de descàrregues";
    public string ChangeDownloadDirMessage => "Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar?";
    public string TrayOpen => "Obrir";
    public string TrayExit => "Sortir";
    public string LibraryNoContent => "No disponible";
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
    public string BtnCancel => "Cancel·lar";
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
    public string BtnUpdate => "Eguneratu";
    public string BtnPlay => "Jolastu";
    public string TooltipOpenFolder => "Jokoaren karpeta ireki";
    public string TooltipUninstall => "Jokoa desinstalatu";
    public string TooltipRefresh => "Freskatu";
    public string FolderNotFoundTitle => "Karpeta ez da aurkitu";
    public string FolderNotFoundMessage => "Jokoaren karpeta ez da aurkitu. Fitxategiak berrinstalatu nahi dituzu?";
    public string UninstallConfirmTitle => "Jokoa desinstalatu";
    public string UninstallConfirmMessage => "{0} desinstalatu nahi duzu? Ekintza hau ezin da desegin.";
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
    public string SettingsCheckForUpdates => "Eguneraketak bilatu";
    public string CheckForUpdatesTitle => "Eguneraketak bilatu";
    public string CheckForUpdatesMessage => "Eguneraketak bilatzeak launcher-a berrabiarazi behar du. Deskarga bat abian badago, hondatu edo eten daiteke. Jarraitu nahi duzu?";
    public string ChangeDownloadDirTitle => "Deskarga direktorioa aldatu";
    public string ChangeDownloadDirMessage => "Jokoak instalatuta badituzu, eskuz mugitu beharko dituzu bide berrira, edo launcher-ak ez ditu ezagutuko. Jarraitu nahi duzu?";
    public string TrayOpen => "Ireki";
    public string TrayExit => "Irten";
    public string LibraryNoContent => "Ez dago eskuragarri";
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
    public string BtnCancel => "Utzi";
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
    public string BtnUpdate => "Actualizar";
    public string BtnPlay => "Xogar";
    public string TooltipOpenFolder => "Abrir cartafol do xogo";
    public string TooltipUninstall => "Desinstalar xogo";
    public string TooltipRefresh => "Actualizar";
    public string FolderNotFoundTitle => "Cartafol non atopado";
    public string FolderNotFoundMessage => "O cartafol do xogo non foi atopado. Queres reinstalar os ficheiros?";
    public string UninstallConfirmTitle => "Desinstalar xogo";
    public string UninstallConfirmMessage => "Seguro que queres desinstalar {0}? Esta acción non se pode desfacer.";
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
    public string SettingsCheckForUpdates => "Buscar actualizacións";
    public string CheckForUpdatesTitle => "Buscar actualizacións";
    public string CheckForUpdatesMessage => "Buscar actualizacións require reiniciar o launcher. Se hai algunha descarga en progreso, podería danarse ou interromperse. Desexas continuar?";
    public string ChangeDownloadDirTitle => "Cambiar directorio de descargas";
    public string ChangeDownloadDirMessage => "Se tes xogos instalados, terás que movelos manualmente á nova ruta ou o launcher non os recoñecerá. Desexas continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Saír";
    public string LibraryNoContent => "Non dispoñible";
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
    public string BtnCancel => "Cancelar";
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
    public string BtnUpdate => "Atualizar";
    public string BtnPlay => "Jogar";
    public string TooltipOpenFolder => "Abrir pasta do jogo";
    public string TooltipUninstall => "Desinstalar jogo";
    public string TooltipRefresh => "Atualizar";
    public string FolderNotFoundTitle => "Pasta não encontrada";
    public string FolderNotFoundMessage => "A pasta do jogo não foi encontrada. Deseja reinstalar os arquivos?";
    public string UninstallConfirmTitle => "Desinstalar jogo";
    public string UninstallConfirmMessage => "Tem certeza que deseja desinstalar {0}? Esta ação não pode ser desfeita.";
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
    public string SettingsCheckForUpdates => "Verificar atualizações";
    public string CheckForUpdatesTitle => "Verificar atualizações";
    public string CheckForUpdatesMessage => "Verificar atualizações requer reiniciar o launcher. Se houver algum download em andamento, ele pode ser interrompido ou corrompido. Deseja continuar?";
    public string ChangeDownloadDirTitle => "Alterar diretório de downloads";
    public string ChangeDownloadDirMessage => "Se você tiver jogos instalados, precisará movê-los manualmente para o novo caminho ou o launcher não os reconhecerá. Deseja continuar?";
    public string TrayOpen => "Abrir";
    public string TrayExit => "Sair";
    public string LibraryNoContent => "Não disponível";
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
    public string BtnCancel => "Cancelar";
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
    public string BtnUpdate => "Actualitzar";
    public string BtnPlay => "Jugar";
    public string TooltipOpenFolder => "Obrir carpeta del joc";
    public string TooltipUninstall => "Desinstal·lar joc";
    public string TooltipRefresh => "Actualitzar";
    public string FolderNotFoundTitle => "Carpeta no trobada";
    public string FolderNotFoundMessage => "La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers?";
    public string UninstallConfirmTitle => "Desinstal·lar joc";
    public string UninstallConfirmMessage => "Estàs segur que vols desinstal·lar {0}? Esta acció no es pot desfer.";
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
    public string SettingsCheckForUpdates => "Buscar actualitzacions";
    public string CheckForUpdatesTitle => "Buscar actualitzacions";
    public string CheckForUpdatesMessage => "Buscar actualitzacions requereix reiniciar el launcher. Si hi ha alguna descàrrega en curs, podria danyar-se o interrompre's. Vols continuar?";
    public string ChangeDownloadDirTitle => "Canviar directori de descàrregues";
    public string ChangeDownloadDirMessage => "Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar?";
    public string TrayOpen => "Obrir";
    public string TrayExit => "Eixir";
    public string LibraryNoContent => "No disponible";
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
    public string BtnCancel => "Cancel·lar";
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
    public string BtnUpdate => "Mettre à jour";
    public string BtnPlay => "Jouer";
    public string TooltipOpenFolder => "Ouvrir le dossier du jeu";
    public string TooltipUninstall => "Désinstaller le jeu";
    public string TooltipRefresh => "Actualiser";
    public string FolderNotFoundTitle => "Dossier non trouvé";
    public string FolderNotFoundMessage => "Le dossier du jeu n'a pas été trouvé. Voulez-vous réinstaller les fichiers ?";
    public string UninstallConfirmTitle => "Désinstaller le jeu";
    public string UninstallConfirmMessage => "Êtes-vous sûr de vouloir désinstaller {0} ? Cette action ne peut pas être annulée.";
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
    public string SettingsCheckForUpdates => "Vérifier les mises à jour";
    public string CheckForUpdatesTitle => "Vérifier les mises à jour";
    public string CheckForUpdatesMessage => "La vérification des mises à jour nécessite de redémarrer le lanceur. Tout téléchargement en cours peut être interrompu ou corrompu. Voulez-vous continuer ?";
    public string ChangeDownloadDirTitle => "Modifier le répertoire de téléchargement";
    public string ChangeDownloadDirMessage => "Si vous avez des jeux installés, vous devrez les déplacer manuellement vers le nouveau chemin ou le lanceur ne les reconnaîtra pas. Voulez-vous continuer ?";
    public string TrayOpen => "Ouvrir";
    public string TrayExit => "Quitter";
    public string LibraryNoContent => "Non disponible";
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
    public string BtnCancel => "Annuler";
}