# 05 · Localization — the full text catalogue

Extracted from `desktop/LostieLauncher/Content/Strings.cs`,
`desktop/LostieLauncher/Content/Faqs.cs` and
`desktop/LostieLauncher/Models/AppLanguage.cs`.

**118 keys x 8 languages = 944 strings, plus 6 FAQ entries x 8 = 48.** The
tables below are generated from the source, so they are the catalogue, not a
summary of it.

## The eight languages

| Enum value | Code in the remote payload | Shown in Settings | Implementation |
| --- | --- | --- | --- |
| `Esp` | `es` | Espanol | `Esp`, `EspFaqs` |
| `Eng` | `en` | English | `Eng`, `EngFaqs` |
| `Cat` | `ca` | Catala | `Cat`, `CatFaqs` |
| `Eus` | `eu` | Euskera | `Eus`, `EusFaqs` |
| `Gal` | `gl` | Galego | `Gal`, `GalFaqs` |
| `Por` | `pt` | Portugues | `Por`, `PorFaqs` |
| `Val` | `val` | Valencia | `Val`, `ValFaqs` |
| `Fra` | `fr` | Francais | `Fra`, `FraFaqs` |

The default is `Esp`, both for a fresh install and as the fallback when a stored
value is not a defined enum member. The names shown in Settings are each
language's own endonym and are **not** themselves localized.

Note the two codes that are not the obvious ones: Spanish is `es` (not `esp`)
and Valencian is `val` (a three-letter code among two-letter ones). These codes
are only used to resolve the remote home content; the string catalogue itself is
selected by enum value.

## How a language change works

There is no restart, no activity recreation and no resource-qualifier switch.

1. The user picks a language in Settings.
2. The settings ViewModel resolves `Strings` to the matching implementation and
   saves the choice.
3. Everything that displays text observes either `Strings` or `Language` and
   re-reads it:
   - the shell re-resolves the current screen title,
   - the tray menu re-resolves its two items,
   - the FAQ screen reloads its entries in the new language and re-applies the
     filter,
   - the home screen **refetches** its content, because the remote payload
     carries all languages and the service resolves one of them,
   - every bound label re-reads through the binding.
4. Nothing caches a resolved string in a field. That is the rule that makes the
   hot switch work.

The equivalent Android requirement: a language change must take effect without
recreating the process or losing screen state, and must persist across runs.

## Rules that must survive the port

- **No user-visible literal outside the catalogue.** The desktop has exactly
  three deliberate exceptions, all recorded in section 10: the window and tray
  title `"Lostie Launcher"`, the `"Saved Games"` tooltip, and the pre-startup
  fatal-error message box, which must work before the catalogue exists.
- **Every key exists in every language.** On desktop a missing implementation is
  a compile error; the Android mechanism must give the same guarantee or the
  step that adds a string must add all eight.
- **Placeholder count and order are identical across all eight languages.**
  Verified: 8 keys carry placeholders and there is **no drift** in any of them.
- Theme names are not localized. Special-version labels (`tipo`) come from the
  server and are not localized either.

### The 8 keys with placeholders

| Key | Placeholders | Arguments, in order |
| --- | --- | --- |
| `UninstallConfirmMessage` | `{0}` | game name |
| `UninstallErrorMessage` | `{0}`, `{1}` | game name, blocking path |
| `UninstallBlockedMessage` | `{0}`, `{1}` | game name, blocking path |
| `UninstallGameRunningMessage` | `{0}` | game name |
| `UninstallMaybeRunningMessage` | `{0}` | game name |
| `UpdateAvailableMessage` | `{0}` | the new launcher version |
| `OneDriveWarningMessage` | `{0}` | the folder path |
| `DownloadDirNotUsableMessage` | `{0}`, `{1}` | folder path, localized step name |

The step name in the last one is itself one of `DownloadDirStepCreate`,
`DownloadDirStepWrite` or `DownloadDirStepRename`.

### Multi-line strings

Five keys embed newlines and are laid out as paragraphs in the dialog:
`UninstallErrorMessage`, `UninstallBlockedMessage`, `WelcomeDialogDescription`,
`OneDriveWarningMessage`, `DownloadDirNotUsableMessage`. They appear as `<br>`
in the tables below; in the source they are `\n`.

### One key that is not a translation

`RepositoryUrl` holds `https://github.com/jagobainda/LostieLauncher` in all
eight languages. It lives in the catalogue because the welcome dialog reads its
link target through the same interface as its text, not because it varies.

---

## The catalogue

Keys are listed in interface declaration order, which groups them roughly by
screen. `<br>` marks an embedded newline.

#### `TitleHome`

| Lang | Text |
| --- | --- |
| Esp | Inicio |
| Eng | Home |
| Cat | Inici |
| Eus | Hasiera |
| Gal | Inicio |
| Por | Início |
| Val | Inici |
| Fra | Accueil |

#### `TitleGames`

| Lang | Text |
| --- | --- |
| Esp | Mis Juegos |
| Eng | My Games |
| Cat | Els meus jocs |
| Eus | Nire Jokoak |
| Gal | Os meus xogos |
| Por | Meus Jogos |
| Val | Els meus jocs |
| Fra | Mes jeux |

#### `TitleLibrary`

| Lang | Text |
| --- | --- |
| Esp | Biblioteca |
| Eng | Library |
| Cat | Biblioteca |
| Eus | Liburutegia |
| Gal | Biblioteca |
| Por | Biblioteca |
| Val | Biblioteca |
| Fra | Bibliothèque |

#### `TitleSettings`

| Lang | Text |
| --- | --- |
| Esp | Ajustes |
| Eng | Settings |
| Cat | Configuració |
| Eus | Ezarpenak |
| Gal | Axustes |
| Por | Configurações |
| Val | Ajustos |
| Fra | Paramètres |

#### `TitleFaqs`

| Lang | Text |
| --- | --- |
| Esp | Preguntas frecuentes |
| Eng | FAQs |
| Cat | Preguntes freqüents |
| Eus | Ohiko galderak |
| Gal | Preguntas frecuentes |
| Por | Perguntas frequentes |
| Val | Preguntes freqüents |
| Fra | Questions fréquentes |

#### `FaqsSearchPlaceholder`

| Lang | Text |
| --- | --- |
| Esp | Buscar en las preguntas y respuestas... |
| Eng | Search questions and answers... |
| Cat | Cerca a les preguntes i respostes... |
| Eus | Bilatu galderetan eta erantzunetan... |
| Gal | Buscar nas preguntas e respostas... |
| Por | Pesquisar nas perguntas e respostas... |
| Val | Busca en les preguntes i respostes... |
| Fra | Rechercher dans les questions et réponses... |

#### `FaqsNoResults`

| Lang | Text |
| --- | --- |
| Esp | Sin resultados para tu búsqueda |
| Eng | No results for your search |
| Cat | Sense resultats per a la teva cerca |
| Eus | Ez dago emaitzarik zure bilaketarako |
| Gal | Sen resultados para a túa busca |
| Por | Sem resultados para sua pesquisa |
| Val | Sense resultats per a la teua cerca |
| Fra | Aucun résultat pour votre recherche |

#### `BtnOk`

| Lang | Text |
| --- | --- |
| Esp | Aceptar |
| Eng | OK |
| Cat | Acceptar |
| Eus | Ados |
| Gal | Aceptar |
| Por | OK |
| Val | Acceptar |
| Fra | OK |

#### `BtnYes`

| Lang | Text |
| --- | --- |
| Esp | Sí |
| Eng | Yes |
| Cat | Sí |
| Eus | Bai |
| Gal | Si |
| Por | Sim |
| Val | Sí |
| Fra | Oui |

#### `BtnNo`

| Lang | Text |
| --- | --- |
| Esp | No |
| Eng | No |
| Cat | No |
| Eus | Ez |
| Gal | Non |
| Por | Não |
| Val | No |
| Fra | Non |

#### `BtnDownload`

| Lang | Text |
| --- | --- |
| Esp | Descargar |
| Eng | Download |
| Cat | Descarregar |
| Eus | Deskargatu |
| Gal | Descargar |
| Por | Baixar |
| Val | Descarregar |
| Fra | Télécharger |

#### `BtnDownloaded`

| Lang | Text |
| --- | --- |
| Esp | Descargado |
| Eng | Downloaded |
| Cat | Descarregat |
| Eus | Deskargatuta |
| Gal | Descargado |
| Por | Baixado |
| Val | Descarregat |
| Fra | Téléchargé |

#### `BtnPause`

| Lang | Text |
| --- | --- |
| Esp | Pausar |
| Eng | Pause |
| Cat | Pausar |
| Eus | Pausatu |
| Gal | Pausar |
| Por | Pausar |
| Val | Pausar |
| Fra | Pause |

#### `BtnResume`

| Lang | Text |
| --- | --- |
| Esp | Reanudar |
| Eng | Resume |
| Cat | Reprendre |
| Eus | Berrekin |
| Gal | Retomar |
| Por | Retomar |
| Val | Reprendre |
| Fra | Reprendre |

#### `BtnUpdate`

| Lang | Text |
| --- | --- |
| Esp | Actualizar |
| Eng | Update |
| Cat | Actualitzar |
| Eus | Eguneratu |
| Gal | Actualizar |
| Por | Atualizar |
| Val | Actualitzar |
| Fra | Mettre à jour |

#### `BtnPlay`

| Lang | Text |
| --- | --- |
| Esp | Jugar |
| Eng | Play |
| Cat | Jugar |
| Eus | Jolastu |
| Gal | Xogar |
| Por | Jogar |
| Val | Jugar |
| Fra | Jouer |

#### `TooltipOpenFolder`

| Lang | Text |
| --- | --- |
| Esp | Abrir carpeta del juego |
| Eng | Open game folder |
| Cat | Obrir carpeta del joc |
| Eus | Jokoaren karpeta ireki |
| Gal | Abrir cartafol do xogo |
| Por | Abrir pasta do jogo |
| Val | Obrir carpeta del joc |
| Fra | Ouvrir le dossier du jeu |

#### `TooltipOpenHelp`

| Lang | Text |
| --- | --- |
| Esp | Abrir carpeta de ayuda |
| Eng | Open help folder |
| Cat | Obrir carpeta d'ajuda |
| Eus | Laguntza karpeta ireki |
| Gal | Abrir cartafol de axuda |
| Por | Abrir pasta de ajuda |
| Val | Obrir carpeta d'ajuda |
| Fra | Ouvrir le dossier d'aide |

#### `TooltipUninstall`

| Lang | Text |
| --- | --- |
| Esp | Desinstalar juego |
| Eng | Uninstall game |
| Cat | Desinstal·lar joc |
| Eus | Jokoa desinstalatu |
| Gal | Desinstalar xogo |
| Por | Desinstalar jogo |
| Val | Desinstal·lar joc |
| Fra | Désinstaller le jeu |

#### `TooltipRefresh`

| Lang | Text |
| --- | --- |
| Esp | Actualizar |
| Eng | Refresh |
| Cat | Actualitzar |
| Eus | Freskatu |
| Gal | Actualizar |
| Por | Atualizar |
| Val | Actualitzar |
| Fra | Actualiser |

#### `FolderNotFoundTitle`

| Lang | Text |
| --- | --- |
| Esp | Carpeta no encontrada |
| Eng | Folder not found |
| Cat | Carpeta no trobada |
| Eus | Karpeta ez da aurkitu |
| Gal | Cartafol non atopado |
| Por | Pasta não encontrada |
| Val | Carpeta no trobada |
| Fra | Dossier non trouvé |

#### `FolderNotFoundMessage`

| Lang | Text |
| --- | --- |
| Esp | La carpeta del juego no se encontró. ¿Quieres reinstalar los archivos? |
| Eng | The game folder was not found. Do you want to reinstall the files? |
| Cat | La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers? |
| Eus | Jokoaren karpeta ez da aurkitu. Fitxategiak berrinstalatu nahi dituzu? |
| Gal | O cartafol do xogo non foi atopado. Queres reinstalar os ficheiros? |
| Por | A pasta do jogo não foi encontrada. Deseja reinstalar os arquivos? |
| Val | La carpeta del joc no s'ha trobat. Vols reinstal·lar els fitxers? |
| Fra | Le dossier du jeu n'a pas été trouvé. Voulez-vous réinstaller les fichiers ? |

#### `UninstallConfirmTitle`

| Lang | Text |
| --- | --- |
| Esp | Desinstalar juego |
| Eng | Uninstall game |
| Cat | Desinstal·lar joc |
| Eus | Jokoa desinstalatu |
| Gal | Desinstalar xogo |
| Por | Desinstalar jogo |
| Val | Desinstal·lar joc |
| Fra | Désinstaller le jeu |

#### `UninstallConfirmMessage`

Placeholders: `{0}`

| Lang | Text |
| --- | --- |
| Esp | ¿Seguro que quieres desinstalar {0}? Las partidas guardadas y el registro de tiempo jugado no se perderán. |
| Eng | Are you sure you want to uninstall {0}? Your saved games and playtime record will not be lost. |
| Cat | Estàs segur que vols desinstal·lar {0}? Les teves partides desades i el registre de temps jugat no es perdran. |
| Eus | {0} desinstalatu nahi duzu? Gordetako partidak eta jolasdenbora ez dira galduko. |
| Gal | Seguro que queres desinstalar {0}? As partidas gardadas e o rexistro de tempo xogado non se perderán. |
| Por | Tem certeza que deseja desinstalar {0}? Seus saves e o registro de tempo de jogo não serão perdidos. |
| Val | Estàs segur que vols desinstal·lar {0}? Les teues partides guardades i el registre de temps jugat no es perdran. |
| Fra | Êtes-vous sûr de vouloir désinstaller {0} ? Vos sauvegardes et le registre de temps de jeu ne seront pas perdus. |

#### `UninstallNotFoundTitle`

| Lang | Text |
| --- | --- |
| Esp | Archivos no encontrados |
| Eng | Files not found |
| Cat | Fitxers no trobats |
| Eus | Fitxategiak ez dira aurkitu |
| Gal | Ficheiros non atopados |
| Por | Arquivos não encontrados |
| Val | Fitxers no trobats |
| Fra | Fichiers non trouvés |

#### `UninstallNotFoundMessage`

| Lang | Text |
| --- | --- |
| Esp | No se encontraron los archivos del juego, pero se ha limpiado el registro de la lista. |
| Eng | The game files were not found, but the entry has been cleaned up from the list. |
| Cat | No s'han trobat els fitxers del joc, però s'ha netejat el registre de la llista. |
| Eus | Jokoaren fitxategiak ez dira aurkitu, baina zerrenda garbi utzi da. |
| Gal | Non se atoparon os ficheiros do xogo, pero limpouse o rexistro da lista. |
| Por | Os arquivos do jogo não foram encontrados, mas o registro foi limpo da lista. |
| Val | No s'han trobat els fitxers del joc, però s'ha netejat el registre de la llista. |
| Fra | Les fichiers du jeu n'ont pas été trouvés, mais l'entrée a été supprimée de la liste. |

#### `UninstallErrorTitle`

| Lang | Text |
| --- | --- |
| Esp | Error al desinstalar |
| Eng | Uninstall error |
| Cat | Error en desinstal·lar |
| Eus | Desinstalazio errorea |
| Gal | Erro ao desinstalar |
| Por | Erro ao desinstalar |
| Val | Error en desinstal·lar |
| Fra | Erreur de désinstallation |

#### `UninstallErrorMessage`

Placeholders: `{0}`, `{1}`

| Lang | Text |
| --- | --- |
| Esp | No se pudieron borrar todos los archivos de {0}. El juego se ha quitado de tu lista, pero queda esto en el disco:<br><br>{1}<br><br>¿Quieres abrir su ubicación para borrarlo a mano? |
| Eng | Some files of {0} could not be deleted. The game has been removed from your list, but this is still on disk:<br><br>{1}<br><br>Do you want to open its location to delete it manually? |
| Cat | No s'han pogut esborrar tots els fitxers de {0}. El joc s'ha tret de la teva llista, però queda això al disc:<br><br>{1}<br><br>Vols obrir la seva ubicació per esborrar-ho a mà? |
| Eus | Ezin izan dira {0} jokoaren fitxategi guztiak ezabatu. Jokoa zerrendatik kendu da, baina hau diskoan dago oraindik:<br><br>{1}<br><br>Bere kokapena ireki nahi duzu eskuz ezabatzeko? |
| Gal | Non se puideron borrar todos os ficheiros de {0}. O xogo quitouse da túa lista, pero queda isto no disco:<br><br>{1}<br><br>Queres abrir a súa localización para borralo a man? |
| Por | Não foi possível apagar todos os arquivos de {0}. O jogo foi removido da sua lista, mas isto ainda está no disco:<br><br>{1}<br><br>Deseja abrir a localização para apagá-lo manualmente? |
| Val | No s'han pogut esborrar tots els fitxers de {0}. El joc s'ha tret de la teua llista, però queda això en el disc:<br><br>{1}<br><br>Vols obrir la seua ubicació per a esborrar-ho a mà? |
| Fra | Certains fichiers de {0} n'ont pas pu être supprimés. Le jeu a été retiré de votre liste, mais ceci reste sur le disque :<br><br>{1}<br><br>Voulez-vous ouvrir son emplacement pour le supprimer manuellement ? |

#### `UninstallBlockedTitle`

| Lang | Text |
| --- | --- |
| Esp | No se pudo desinstalar |
| Eng | Could not uninstall |
| Cat | No s'ha pogut desinstal·lar |
| Eus | Ezin izan da desinstalatu |
| Gal | Non se puido desinstalar |
| Por | Não foi possível desinstalar |
| Val | No s'ha pogut desinstal·lar |
| Fra | Impossible de désinstaller |

#### `UninstallBlockedMessage`

Placeholders: `{0}`, `{1}`

| Lang | Text |
| --- | --- |
| Esp | No se ha podido borrar ningún archivo de {0}, así que sigue instalado y en tu lista. Algo lo está bloqueando:<br><br>{1}<br><br>Cierra los programas que puedan estar usándolo e inténtalo de nuevo. ¿Quieres abrir su ubicación? |
| Eng | No file of {0} could be deleted, so it is still installed and in your list. Something is blocking it:<br><br>{1}<br><br>Close any program that may be using it and try again. Do you want to open its location? |
| Cat | No s'ha pogut esborrar cap fitxer de {0}, així que continua instal·lat i a la teva llista. Alguna cosa el bloqueja:<br><br>{1}<br><br>Tanca els programes que el puguin estar utilitzant i torna-ho a provar. Vols obrir la seva ubicació? |
| Eus | Ezin izan da {0} jokoaren fitxategirik ezabatu, beraz instalatuta jarraitzen du eta zure zerrendan dago. Zerbaitek blokeatzen du:<br><br>{1}<br><br>Itxi erabiltzen ari daitezkeen programak eta saiatu berriro. Bere kokapena ireki nahi duzu? |
| Gal | Non se puido borrar ningún ficheiro de {0}, así que segue instalado e na túa lista. Algo o está bloqueando:<br><br>{1}<br><br>Pecha os programas que poidan estar usándoo e téntao de novo. Queres abrir a súa localización? |
| Por | Não foi possível apagar nenhum arquivo de {0}, portanto ele continua instalado e na sua lista. Algo o está bloqueando:<br><br>{1}<br><br>Feche os programas que possam estar usando-o e tente novamente. Deseja abrir a localização? |
| Val | No s'ha pogut esborrar cap fitxer de {0}, així que continua instal·lat i en la teua llista. Alguna cosa el bloqueja:<br><br>{1}<br><br>Tanca els programes que el puguen estar utilitzant i torna-ho a provar. Vols obrir la seua ubicació? |
| Fra | Aucun fichier de {0} n'a pu être supprimé, il est donc toujours installé et dans votre liste. Quelque chose le bloque :<br><br>{1}<br><br>Fermez les programmes qui pourraient l'utiliser et réessayez. Voulez-vous ouvrir son emplacement ? |

#### `UninstallGameRunningTitle`

| Lang | Text |
| --- | --- |
| Esp | El juego está abierto |
| Eng | Game is running |
| Cat | El joc està obert |
| Eus | Jokoa irekita dago |
| Gal | O xogo está aberto |
| Por | O jogo está aberto |
| Val | El joc està obert |
| Fra | Le jeu est en cours d'exécution |

#### `UninstallGameRunningMessage`

Placeholders: `{0}`

| Lang | Text |
| --- | --- |
| Esp | Cierra {0} antes de desinstalarlo. |
| Eng | Close {0} before uninstalling it. |
| Cat | Tanca {0} abans de desinstal·lar-lo. |
| Eus | Itxi {0} desinstalatu aurretik. |
| Gal | Pecha {0} antes de desinstalalo. |
| Por | Feche {0} antes de desinstalá-lo. |
| Val | Tanca {0} abans de desinstal·lar-lo. |
| Fra | Fermez {0} avant de le désinstaller. |

#### `UninstallMaybeRunningMessage`

Placeholders: `{0}`

| Lang | Text |
| --- | --- |
| Esp | Parece que {0} está en uso por otro programa (el propio juego, un antivirus o el explorador de archivos). Si lo desinstalas ahora es posible que queden archivos sin borrar. ¿Quieres continuar de todas formas? |
| Eng | {0} looks like it is in use by another program (the game itself, an antivirus or the file explorer). Uninstalling now may leave files behind. Do you want to continue anyway? |
| Cat | Sembla que {0} està en ús per un altre programa (el mateix joc, un antivirus o l'explorador de fitxers). Si el desinstal·les ara, pot ser que quedin fitxers sense esborrar. Vols continuar igualment? |
| Eus | Badirudi {0} beste programa batek erabiltzen duela (jokoa bera, antibirus bat edo fitxategi-arakatzailea). Orain desinstalatzen baduzu, baliteke fitxategi batzuk ezabatu gabe geratzea. Jarraitu nahi duzu? |
| Gal | Parece que {0} está en uso por outro programa (o propio xogo, un antivirus ou o explorador de ficheiros). Se o desinstalas agora é posible que queden ficheiros sen borrar. Queres continuar de todos os xeitos? |
| Por | Parece que {0} está em uso por outro programa (o próprio jogo, um antivírus ou o explorador de arquivos). Se desinstalar agora, alguns arquivos podem não ser apagados. Deseja continuar mesmo assim? |
| Val | Pareix que {0} està en ús per un altre programa (el mateix joc, un antivirus o l'explorador de fitxers). Si el desinstal·les ara, pot ser que queden fitxers sense esborrar. Vols continuar igualment? |
| Fra | {0} semble être utilisé par un autre programme (le jeu lui-même, un antivirus ou l'explorateur de fichiers). Si vous le désinstallez maintenant, des fichiers pourraient rester. Voulez-vous continuer quand même ? |

#### `UpdateAvailableTitle`

| Lang | Text |
| --- | --- |
| Esp | Actualización disponible |
| Eng | Update available |
| Cat | Actualització disponible |
| Eus | Eguneraketa eskuragarri |
| Gal | Actualización dispoñible |
| Por | Atualização disponível |
| Val | Actualització disponible |
| Fra | Mise à jour disponible |

#### `UpdateAvailableMessage`

Placeholders: `{0}`

| Lang | Text |
| --- | --- |
| Esp | Nueva versión {0} disponible. ¿Reiniciar para actualizar? |
| Eng | New version {0} available. Restart to update? |
| Cat | Nova versió {0} disponible. Reiniciar per actualitzar? |
| Eus | {0} bertsio berria eskuragarri. Berrabiarazi eguneratzeko? |
| Gal | Nova versión {0} dispoñible. Reiniciar para actualizar? |
| Por | Nova versão {0} disponível. Reiniciar para atualizar? |
| Val | Nova versió {0} disponible. Reiniciar per a actualitzar? |
| Fra | Nouvelle version {0} disponible. Redémarrer pour mettre à jour ? |

#### `SettingsGeneral`

| Lang | Text |
| --- | --- |
| Esp | General |
| Eng | General |
| Cat | General |
| Eus | Orokorra |
| Gal | Xeral |
| Por | Geral |
| Val | General |
| Fra | Général |

#### `SettingsAppearance`

| Lang | Text |
| --- | --- |
| Esp | Apariencia |
| Eng | Appearance |
| Cat | Aparença |
| Eus | Itxura |
| Gal | Aparencia |
| Por | Aparência |
| Val | Aparença |
| Fra | Apparence |

#### `SettingsStartWithWindows`

| Lang | Text |
| --- | --- |
| Esp | Iniciar con Windows |
| Eng | Start with Windows |
| Cat | Iniciar amb Windows |
| Eus | Windows-ekin hasi |
| Gal | Iniciar con Windows |
| Por | Iniciar com o Windows |
| Val | Iniciar amb Windows |
| Fra | Démarrer avec Windows |

#### `SettingsStartMinimized`

| Lang | Text |
| --- | --- |
| Esp | Iniciar minimizado |
| Eng | Start minimized |
| Cat | Iniciar minimitzat |
| Eus | Minimizatuta hasi |
| Gal | Iniciar minimizado |
| Por | Iniciar minimizado |
| Val | Iniciar minimitzat |
| Fra | Démarrer minimisé |

#### `SettingsAutoUpdate`

| Lang | Text |
| --- | --- |
| Esp | Actualizaciones automáticas |
| Eng | Automatic updates |
| Cat | Actualitzacions automàtiques |
| Eus | Eguneraketa automatikoak |
| Gal | Actualizacións automáticas |
| Por | Atualizações automáticas |
| Val | Actualitzacions automàtiques |
| Fra | Mises à jour automatiques |

#### `SettingsLanguage`

| Lang | Text |
| --- | --- |
| Esp | Idioma |
| Eng | Language |
| Cat | Idioma |
| Eus | Hizkuntza |
| Gal | Idioma |
| Por | Idioma |
| Val | Idioma |
| Fra | Langue |

#### `SettingsDownloadDir`

| Lang | Text |
| --- | --- |
| Esp | Directorio de descargas |
| Eng | Download directory |
| Cat | Directori de descàrregues |
| Eus | Deskarga direktorioa |
| Gal | Directorio de descargas |
| Por | Diretório de downloads |
| Val | Directori de descàrregues |
| Fra | Répertoire de téléchargement |

#### `SettingsTheme`

| Lang | Text |
| --- | --- |
| Esp | Tema |
| Eng | Theme |
| Cat | Tema |
| Eus | Gaia |
| Gal | Tema |
| Por | Tema |
| Val | Tema |
| Fra | Thème |

#### `BtnBrowse`

| Lang | Text |
| --- | --- |
| Esp | Examinar... |
| Eng | Browse... |
| Cat | Explorar... |
| Eus | Arakatu... |
| Gal | Examinar... |
| Por | Procurar... |
| Val | Explorar... |
| Fra | Parcourir... |

#### `SettingsCheckForUpdates`

| Lang | Text |
| --- | --- |
| Esp | Buscar actualizaciones del launcher |
| Eng | Check for launcher updates |
| Cat | Buscar actualitzacions del launcher |
| Eus | Launcher-aren eguneraketak bilatu |
| Gal | Buscar actualizacións do launcher |
| Por | Verificar atualizações do launcher |
| Val | Buscar actualitzacions del launcher |
| Fra | Vérifier les mises à jour du launcher |

#### `UpToDateTitle`

| Lang | Text |
| --- | --- |
| Esp | Sin actualizaciones |
| Eng | No updates |
| Cat | Sense actualitzacions |
| Eus | Eguneraketarik ez |
| Gal | Sen actualizacións |
| Por | Sem atualizações |
| Val | Sense actualitzacions |
| Fra | Aucune mise à jour |

#### `UpToDateMessage`

| Lang | Text |
| --- | --- |
| Esp | Ya tienes la última versión del launcher. |
| Eng | You already have the latest version of the launcher. |
| Cat | Ja tens l'última versió del launcher. |
| Eus | Dagoeneko launcher-aren azken bertsioa duzu. |
| Gal | Xa tes a última versión do launcher. |
| Por | Você já tem a versão mais recente do launcher. |
| Val | Ja tens l'última versió del launcher. |
| Fra | Vous avez déjà la dernière version du lanceur. |

#### `UpdateCheckBusyTitle`

| Lang | Text |
| --- | --- |
| Esp | Descarga en curso |
| Eng | Download in progress |
| Cat | Descàrrega en curs |
| Eus | Deskarga abian |
| Gal | Descarga en curso |
| Por | Download em andamento |
| Val | Descàrrega en curs |
| Fra | Téléchargement en cours |

#### `UpdateCheckBusyMessage`

| Lang | Text |
| --- | --- |
| Esp | No se pueden buscar actualizaciones mientras hay una descarga en curso. Espera a que termine e inténtalo de nuevo. |
| Eng | Updates can't be checked while a download is in progress. Wait for it to finish and try again. |
| Cat | No es poden buscar actualitzacions mentre hi ha una descàrrega en curs. Espera que acabi i torna-ho a provar. |
| Eus | Ezin dira eguneraketak bilatu deskarga bat abian dagoen bitartean. Itxaron amaitu arte eta saiatu berriro. |
| Gal | Non se poden buscar actualizacións mentres hai unha descarga en curso. Agarda a que remate e téntao de novo. |
| Por | Não é possível verificar atualizações enquanto há um download em andamento. Aguarde a conclusão e tente novamente. |
| Val | No es poden buscar actualitzacions mentre hi ha una descàrrega en curs. Espera que acabi i torna-ho a provar. |
| Fra | Impossible de vérifier les mises à jour pendant un téléchargement. Attendez la fin et réessayez. |

#### `UpdateCheckFailedTitle`

| Lang | Text |
| --- | --- |
| Esp | Error al buscar actualizaciones |
| Eng | Update check failed |
| Cat | Error en cercar actualitzacions |
| Eus | Errorea eguneraketak bilatzean |
| Gal | Erro ao buscar actualizacións |
| Por | Falha ao verificar atualizações |
| Val | Error en cercar actualitzacions |
| Fra | Échec de la vérification des mises à jour |

#### `UpdateCheckFailedMessage`

| Lang | Text |
| --- | --- |
| Esp | No se ha podido comprobar si hay actualizaciones. Revisa tu conexión e inténtalo de nuevo más tarde. |
| Eng | Couldn't check for updates. Check your connection and try again later. |
| Cat | No s'ha pogut comprovar si hi ha actualitzacions. Revisa la connexió i torna-ho a provar més tard. |
| Eus | Ezin izan da eguneraketarik dagoen egiaztatu. Egiaztatu konexioa eta saiatu berriro geroago. |
| Gal | Non se puido comprobar se hai actualizacións. Revisa a túa conexión e téntao de novo máis tarde. |
| Por | Não foi possível verificar atualizações. Verifique sua conexão e tente novamente mais tarde. |
| Val | No s'ha pogut comprovar si hi ha actualitzacions. Revisa la connexió i torna-ho a provar més tard. |
| Fra | Impossible de vérifier les mises à jour. Vérifiez votre connexion et réessayez plus tard. |

#### `ChangeDownloadDirTitle`

| Lang | Text |
| --- | --- |
| Esp | Cambiar directorio de descargas |
| Eng | Change download directory |
| Cat | Canviar directori de descàrregues |
| Eus | Deskarga direktorioa aldatu |
| Gal | Cambiar directorio de descargas |
| Por | Alterar diretório de downloads |
| Val | Canviar directori de descàrregues |
| Fra | Modifier le répertoire de téléchargement |

#### `ChangeDownloadDirMessage`

| Lang | Text |
| --- | --- |
| Esp | Si tienes juegos instalados, tendrás que moverlos manualmente a la nueva ruta o el launcher no los reconocerá. ¿Deseas continuar? |
| Eng | If you have installed games, you will need to move them manually to the new path or the launcher won't recognize them. Do you want to continue? |
| Cat | Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar? |
| Eus | Jokoak instalatuta badituzu, eskuz mugitu beharko dituzu bide berrira, edo launcher-ak ez ditu ezagutuko. Jarraitu nahi duzu? |
| Gal | Se tes xogos instalados, terás que movelos manualmente á nova ruta ou o launcher non os recoñecerá. Desexas continuar? |
| Por | Se você tiver jogos instalados, precisará movê-los manualmente para o novo caminho ou o launcher não os reconhecerá. Deseja continuar? |
| Val | Si tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta o el launcher no els reconeixerà. Vols continuar? |
| Fra | Si vous avez des jeux installés, vous devrez les déplacer manuellement vers le nouveau chemin ou le lanceur ne les reconnaîtra pas. Voulez-vous continuer ? |

#### `TrayOpen`

| Lang | Text |
| --- | --- |
| Esp | Abrir |
| Eng | Open |
| Cat | Obrir |
| Eus | Ireki |
| Gal | Abrir |
| Por | Abrir |
| Val | Obrir |
| Fra | Ouvrir |

#### `TrayExit`

| Lang | Text |
| --- | --- |
| Esp | Salir |
| Eng | Exit |
| Cat | Sortir |
| Eus | Irten |
| Gal | Saír |
| Por | Sair |
| Val | Eixir |
| Fra | Quitter |

#### `ExitWarningTitle`

| Lang | Text |
| --- | --- |
| Esp | Salir del launcher |
| Eng | Exit the launcher |
| Cat | Sortir del launcher |
| Eus | Launcherretik irten |
| Gal | Saír do launcher |
| Por | Sair do launcher |
| Val | Eixir del launcher |
| Fra | Quitter le launcher |

#### `ExitWarningDownloadMessage`

| Lang | Text |
| --- | --- |
| Esp | Hay una descarga en curso. Si sales ahora se detendrá, pero podrás reanudarla la próxima vez que abras el launcher. ¿Seguro que quieres salir? |
| Eng | A download is in progress. If you exit now it will stop, but you can resume it the next time you open the launcher. Are you sure you want to exit? |
| Cat | Hi ha una descàrrega en curs. Si surts ara s'aturarà, però la podràs reprendre la propera vegada que obris el launcher. Estàs segur que vols sortir? |
| Eus | Deskarga bat abian da. Orain irtenez gero geldituko da, baina launcherra hurrengoan irekitzean berrekin ahal izango diozu. Ziur zaude irten nahi duzula? |
| Gal | Hai unha descarga en curso. Se saes agora deterase, pero poderás retomala a próxima vez que abras o launcher. Seguro que queres saír? |
| Por | Há um download em andamento. Se sair agora ele será interrompido, mas poderá retomá-lo na próxima vez que abrir o launcher. Tem certeza que deseja sair? |
| Val | Hi ha una descàrrega en curs. Si ixes ara s'aturarà, però la podràs reprendre la pròxima vegada que òbrigues el launcher. Estàs segur que vols eixir? |
| Fra | Un téléchargement est en cours. Si vous quittez maintenant, il s'arrêtera, mais vous pourrez le reprendre à la prochaine ouverture du launcher. Êtes-vous sûr de vouloir quitter ? |

#### `ExitWarningGameMessage`

| Lang | Text |
| --- | --- |
| Esp | Tienes un juego abierto. Si sales ahora no se guardará el tiempo jugado de esta sesión. ¿Seguro que quieres salir? |
| Eng | You have a game open. If you exit now the playtime of this session will not be saved. Are you sure you want to exit? |
| Cat | Tens un joc obert. Si surts ara no es desarà el temps jugat d'aquesta sessió. Estàs segur que vols sortir? |
| Eus | Joko bat irekita duzu. Orain irtenez gero, saio honetan jokatutako denbora ez da gordeko. Ziur zaude irten nahi duzula? |
| Gal | Tes un xogo aberto. Se saes agora non se gardará o tempo xogado desta sesión. Seguro que queres saír? |
| Por | Você tem um jogo aberto. Se sair agora o tempo jogado desta sessão não será salvo. Tem certeza que deseja sair? |
| Val | Tens un joc obert. Si ixes ara no es guardarà el temps jugat d'esta sessió. Estàs segur que vols eixir? |
| Fra | Vous avez un jeu ouvert. Si vous quittez maintenant, le temps de jeu de cette session ne sera pas enregistré. Êtes-vous sûr de vouloir quitter ? |

#### `ExitWarningBothMessage`

| Lang | Text |
| --- | --- |
| Esp | Hay una descarga en curso y un juego abierto. La descarga se detendrá (podrás reanudarla más tarde) y no se guardará el tiempo jugado de esta sesión. ¿Seguro que quieres salir? |
| Eng | A download is in progress and a game is open. The download will stop (you can resume it later) and the playtime of this session will not be saved. Are you sure you want to exit? |
| Cat | Hi ha una descàrrega en curs i un joc obert. La descàrrega s'aturarà (la podràs reprendre més tard) i no es desarà el temps jugat d'aquesta sessió. Estàs segur que vols sortir? |
| Eus | Deskarga bat abian da eta joko bat irekita duzu. Deskarga geldituko da (geroago berrekin ahal izango diozu) eta saio honetan jokatutako denbora ez da gordeko. Ziur zaude irten nahi duzula? |
| Gal | Hai unha descarga en curso e un xogo aberto. A descarga deterase (poderás retomala máis tarde) e non se gardará o tempo xogado desta sesión. Seguro que queres saír? |
| Por | Há um download em andamento e um jogo aberto. O download será interrompido (poderá retomá-lo mais tarde) e o tempo jogado desta sessão não será salvo. Tem certeza que deseja sair? |
| Val | Hi ha una descàrrega en curs i un joc obert. La descàrrega s'aturarà (la podràs reprendre més tard) i no es guardarà el temps jugat d'esta sessió. Estàs segur que vols eixir? |
| Fra | Un téléchargement est en cours et un jeu est ouvert. Le téléchargement s'arrêtera (vous pourrez le reprendre plus tard) et le temps de jeu de cette session ne sera pas enregistré. Êtes-vous sûr de vouloir quitter ? |

#### `LibraryNoContent`

| Lang | Text |
| --- | --- |
| Esp | No disponible |
| Eng | Not available |
| Cat | No disponible |
| Eus | Ez dago eskuragarri |
| Gal | Non dispoñible |
| Por | Não disponível |
| Val | No disponible |
| Fra | Non disponible |

#### `GamesNoContent`

| Lang | Text |
| --- | --- |
| Esp | No tienes juegos instalados |
| Eng | No games installed |
| Cat | No tens cap joc instal·lat |
| Eus | Ez daukazu jokorik instalatuta |
| Gal | Non tes ningún xogo instalado |
| Por | Nenhum jogo instalado |
| Val | No tens cap joc instal·lat |
| Fra | Aucun jeu installé |

#### `GamesGoToLibrary`

| Lang | Text |
| --- | --- |
| Esp | Ir a la biblioteca |
| Eng | Go to Library |
| Cat | Anar a la biblioteca |
| Eus | Liburutegira joan |
| Gal | Ir á biblioteca |
| Por | Ir à biblioteca |
| Val | Anar a la biblioteca |
| Fra | Aller à la bibliothèque |

#### `HomeNews`

| Lang | Text |
| --- | --- |
| Esp | Novedades |
| Eng | News |
| Cat | Novetats |
| Eus | Berriak |
| Gal | Novidades |
| Por | Novidades |
| Val | Novetats |
| Fra | Actualités |

#### `HomeNotifications`

| Lang | Text |
| --- | --- |
| Esp | Notificaciones |
| Eng | Notifications |
| Cat | Notificacions |
| Eus | Jakinarazpenak |
| Gal | Notificacións |
| Por | Notificações |
| Val | Notificacions |
| Fra | Notifications |

#### `HomeNoContent`

| Lang | Text |
| --- | --- |
| Esp | Sin contenido |
| Eng | No content |
| Cat | Sense contingut |
| Eus | Eduki gabe |
| Gal | Sen contido |
| Por | Sem conteúdo |
| Val | Sense contingut |
| Fra | Aucun contenu |

#### `DownloadDialogTitle`

| Lang | Text |
| --- | --- |
| Esp | Confirmar descarga |
| Eng | Confirm download |
| Cat | Confirmar descàrrega |
| Eus | Deskarga berretsi |
| Gal | Confirmar descarga |
| Por | Confirmar download |
| Val | Confirmar descàrrega |
| Fra | Confirmer le téléchargement |

#### `DownloadDialogPath`

| Lang | Text |
| --- | --- |
| Esp | Ruta de descarga |
| Eng | Download path |
| Cat | Ruta de descàrrega |
| Eus | Deskarga bidea |
| Gal | Ruta de descarga |
| Por | Caminho de download |
| Val | Ruta de descàrrega |
| Fra | Chemin de téléchargement |

#### `DownloadDialogGameSize`

| Lang | Text |
| --- | --- |
| Esp | Tamaño |
| Eng | Size |
| Cat | Mida |
| Eus | Tamaina |
| Gal | Tamaño |
| Por | Tamanho |
| Val | Grandària |
| Fra | Taille |

#### `DownloadDialogFreeSpace`

| Lang | Text |
| --- | --- |
| Esp | Espacio libre |
| Eng | Free space |
| Cat | Espai lliure |
| Eus | Leku librea |
| Gal | Espazo libre |
| Por | Espaço livre |
| Val | Espai lliure |
| Fra | Espace libre |

#### `DownloadDialogViewPage`

| Lang | Text |
| --- | --- |
| Esp | Ver página del juego |
| Eng | View game page |
| Cat | Veure pàgina del joc |
| Eus | Jokoaren orria ikusi |
| Gal | Ver páxina do xogo |
| Por | Ver página do jogo |
| Val | Veure pàgina del joc |
| Fra | Voir la page du jeu |

#### `DownloadDialogNoDescription`

| Lang | Text |
| --- | --- |
| Esp | Sin descripción disponible. |
| Eng | No description available. |
| Cat | Sense descripció disponible. |
| Eus | Deskribapenik ez. |
| Gal | Sen descrición dispoñible. |
| Por | Sem descrição disponível. |
| Val | Sense descripció disponible. |
| Fra | Aucune description disponible. |

#### `DownloadDialogKey`

| Lang | Text |
| --- | --- |
| Esp | Clave para versiones especiales (opcional) |
| Eng | Access key for special versions (optional) |
| Cat | Clau per a versions especials (opcional) |
| Eus | Bertsio berezietarako gakoa (aukerakoa) |
| Gal | Clave para versións especiais (opcional) |
| Por | Chave para versões especiais (opcional) |
| Val | Clau per a versions especials (opcional) |
| Fra | Clé pour les versions spéciales (optionnel) |

#### `DownloadKeyInvalidTitle`

| Lang | Text |
| --- | --- |
| Esp | Clave no válida |
| Eng | Invalid key |
| Cat | Clau no vàlida |
| Eus | Gako baliogabea |
| Gal | Clave non válida |
| Por | Chave inválida |
| Val | Clau no vàlida |
| Fra | Clé invalide |

#### `DownloadKeyInvalidMessage`

| Lang | Text |
| --- | --- |
| Esp | El formato de la clave no es válido. Debe seguir el formato XXXX-XXXX-XXXX-XXXX-XXXX. |
| Eng | The key format is invalid. It must follow the format XXXX-XXXX-XXXX-XXXX-XXXX. |
| Cat | El format de la clau no és vàlid. Ha de tenir el format XXXX-XXXX-XXXX-XXXX-XXXX. |
| Eus | Gakoaren formatua ez da baliozkoa. Formatua XXXX-XXXX-XXXX-XXXX-XXXX izan behar da. |
| Gal | O formato da clave non é válido. Debe ter o formato XXXX-XXXX-XXXX-XXXX-XXXX. |
| Por | O formato da chave é inválido. Deve seguir o formato XXXX-XXXX-XXXX-XXXX-XXXX. |
| Val | El format de la clau no és vàlid. Ha de tindre el format XXXX-XXXX-XXXX-XXXX-XXXX. |
| Fra | Le format de la clé est invalide. Il doit suivre le format XXXX-XXXX-XXXX-XXXX-XXXX. |

#### `DownloadErrorTitle`

| Lang | Text |
| --- | --- |
| Esp | Error en la descarga |
| Eng | Download failed |
| Cat | Ha fallat la descàrrega |
| Eus | Deskargetak huts egin du |
| Gal | Erro na descarga |
| Por | Falha no download |
| Val | Ha fallat la descàrrega |
| Fra | Échec du téléchargement |

#### `DownloadErrorMessage`

| Lang | Text |
| --- | --- |
| Esp | No se pudo completar la descarga. Por favor, intenta más tarde. Si el problema persiste, escribe en #testeo-launcher en Discord. |
| Eng | The download could not be completed. Please try again later. If the problem persists, write in #testeo-launcher on Discord. |
| Cat | No s'ha pogut completar la descàrrega. Si us plau, intenta-ho més tard. Si el problema persiste, escriu a #testeo-launcher en Discord. |
| Eus | Deskargetak ezin izan du osatu. Mesedez, geroago saiatu. Arazoa jarraitzen badu, idatzi #testeo-launcher kanalean Discord-en. |
| Gal | Non foi posible completar a descarga. Inténtao de novo máis tarde. Se o problema persiste, escribe en #testeo-launcher en Discord. |
| Por | O download não pôde ser concluído. Por favor, tente mais tarde. Se o problema persistir, escreva em #testeo-launcher no Discord. |
| Val | No s'ha pogut completar la descàrrega. Si us plau, intenta-ho més tard. Si el problema persiste, escriu a #testeo-launcher en Discord. |
| Fra | Le téléchargement n'a pas pu être complété. Veuillez réessayer plus tard. Si le problème persiste, écrivez dans #testeo-launcher sur Discord. |

#### `DownloadPermissionDeniedTitle`

| Lang | Text |
| --- | --- |
| Esp | Permisos insuficientes |
| Eng | Insufficient permissions |
| Cat | Permisos insuficients |
| Eus | Baimen nahikorik ez |
| Gal | Permisos insuficientes |
| Por | Permissões insuficientes |
| Val | Permisos insuficients |
| Fra | Permissions insuffisantes |

#### `DownloadPermissionDeniedMessage`

| Lang | Text |
| --- | --- |
| Esp | El launcher no tiene permisos para instalar el juego en la ruta de descarga elegida. Prueba a cambiar a otra ruta en Ajustes. |
| Eng | The launcher doesn't have permission to install the game in the chosen download path. Try changing to a different path in Settings. |
| Cat | El launcher no té permisos per instal·lar el joc a la ruta de descàrrega triada. Prova a canviar a una altra ruta a Configuració. |
| Eus | Launcher-ak ez du baimenik jokoa aukeratutako deskarga bidean instalatzeko. Saiatu Ezarpenetan beste bide bat aukeratzen. |
| Gal | O launcher non ten permisos para instalar o xogo na ruta de descarga elixida. Proba a cambiar a outra ruta en Axustes. |
| Por | O launcher não tem permissão para instalar o jogo no caminho de download escolhido. Tente mudar para outro caminho em Configurações. |
| Val | El launcher no té permisos per a instal·lar el joc en la ruta de descàrrega triada. Prova a canviar a una altra ruta en Ajustos. |
| Fra | Le launcher n'a pas la permission d'installer le jeu dans le chemin de téléchargement choisi. Essayez de changer de chemin dans les Paramètres. |

#### `BtnCancel`

| Lang | Text |
| --- | --- |
| Esp | Cancelar |
| Eng | Cancel |
| Cat | Cancel·lar |
| Eus | Utzi |
| Gal | Cancelar |
| Por | Cancelar |
| Val | Cancel·lar |
| Fra | Annuler |

#### `CancelDownloadConfirmTitle`

| Lang | Text |
| --- | --- |
| Esp | Cancelar descarga |
| Eng | Cancel download |
| Cat | Cancel·lar descàrrega |
| Eus | Deskarga utzi |
| Gal | Cancelar descarga |
| Por | Cancelar download |
| Val | Cancel·lar descàrrega |
| Fra | Annuler le téléchargement |

#### `CancelDownloadConfirmMessage`

| Lang | Text |
| --- | --- |
| Esp | ¿Seguro que quieres cancelar la descarga? Se eliminarán los archivos parcialmente descargados. |
| Eng | Are you sure you want to cancel the download? Partially downloaded files will be deleted. |
| Cat | Estàs segur que vols cancel·lar la descàrrega? Els fitxers descarregats parcialment seran eliminats. |
| Eus | Ziur zaude deskarga utzi nahi duzula? Partzialki deskargatutako fitxategiak ezabatuko dira. |
| Gal | Seguro que queres cancelar a descarga? Os ficheiros parcialmente descargados serán eliminados. |
| Por | Tem certeza que deseja cancelar o download? Os arquivos parcialmente baixados serão excluídos. |
| Val | Estàs segur que vols cancel·lar la descàrrega? Els fitxers descarregats parcialment seran eliminats. |
| Fra | Êtes-vous sûr de vouloir annuler le téléchargement ? Les fichiers partiellement téléchargés seront supprimés. |

#### `StatusExtracting`

| Lang | Text |
| --- | --- |
| Esp | Descomprimiendo... |
| Eng | Extracting... |
| Cat | Descomprimint... |
| Eus | Deskonprimatzen... |
| Gal | Descomprimindo... |
| Por | Descomprimindo... |
| Val | Descomprimint... |
| Fra | Extraction en cours... |

#### `StatusVerifying`

| Lang | Text |
| --- | --- |
| Esp | Comprobando integridad... |
| Eng | Verifying integrity... |
| Cat | Comprovant integritat... |
| Eus | Osotasuna egiaztatzen... |
| Gal | Comprobando integridade... |
| Por | Verificando integridade... |
| Val | Comprovant integritat... |
| Fra | Vérification de l'intégrité... |

#### `StatusUninstalling`

| Lang | Text |
| --- | --- |
| Esp | Desinstalando... |
| Eng | Uninstalling... |
| Cat | Desinstal·lant... |
| Eus | Desinstalatzen... |
| Gal | Desinstalando... |
| Por | Desinstalando... |
| Val | Desinstal·lant... |
| Fra | Désinstallation en cours... |

#### `GameExeNotFoundTitle`

| Lang | Text |
| --- | --- |
| Esp | Juego no encontrado |
| Eng | Game not found |
| Cat | Joc no trobat |
| Eus | Jokoa ez da aurkitu |
| Gal | Xogo non atopado |
| Por | Jogo não encontrado |
| Val | Joc no trobat |
| Fra | Jeu non trouvé |

#### `GameExeNotFoundMessage`

| Lang | Text |
| --- | --- |
| Esp | No se encontró el ejecutable del juego. Intenta reinstalarlo. |
| Eng | The game executable was not found. Try reinstalling the game. |
| Cat | No s'ha trobat l'executable del joc. Prova a reinstal·lar-lo. |
| Eus | Jokoaren exekutagarria ez da aurkitu. Saiatu berrinstalatzea. |
| Gal | Non se atopou o executable do xogo. Intenta reinstalalo. |
| Por | O executável do jogo não foi encontrado. Tente reinstalá-lo. |
| Val | No s'ha trobat l'executable del joc. Prova a reinstal·lar-lo. |
| Fra | L'exécutable du jeu est introuvable. Essayez de le réinstaller. |

#### `HashMismatchTitle`

| Lang | Text |
| --- | --- |
| Esp | Error de integridad |
| Eng | Integrity error |
| Cat | Error d'integritat |
| Eus | Osotasun errorea |
| Gal | Erro de integridade |
| Por | Erro de integridade |
| Val | Error d'integritat |
| Fra | Erreur d'intégrité |

#### `HashMismatchMessage`

| Lang | Text |
| --- | --- |
| Esp | El archivo descargado está dañado o ha sido modificado. Por favor, intenta de nuevo. Si el problema persiste, escribe en #testeo-launcher en Discord. |
| Eng | The downloaded file is corrupted or has been tampered with. Please try again. If the problem persists, write in #testeo-launcher on Discord. |
| Cat | El fitxer descarregat està danyat o ha estat modificat. Si us plau, intenta-ho de nou. Si el problema persisteix, escriu a #testeo-launcher a Discord. |
| Eus | Deskargatutako fitxategia hondatuta edo aldatuta dago. Mesedez, saiatu berriro. Arazoa jarraitzen badu, idatzi #testeo-launcher kanalean Discord-en. |
| Gal | O ficheiro descargado está danado ou foi modificado. Por favor, téntao de novo. Se o problema persiste, escribe en #testeo-launcher en Discord. |
| Por | O arquivo baixado está corrompido ou foi modificado. Por favor, tente novamente. Se o problema persistir, escreva em #testeo-launcher no Discord. |
| Val | El fitxer descarregat està danyat o ha sigut modificat. Si us plau, intenta-ho de nou. Si el problema persisteix, escriu a #testeo-launcher a Discord. |
| Fra | Le fichier téléchargé est corrompu ou a été modifié. Veuillez réessayer. Si le problème persiste, écrivez dans #testeo-launcher sur Discord. |

#### `WelcomeDialogTitle`

| Lang | Text |
| --- | --- |
| Esp | ¡Bienvenido al Lostie Launcher! |
| Eng | Welcome to Lostie Launcher! |
| Cat | Benvingut al Lostie Launcher! |
| Eus | Ongi etorri Lostie Launcher-era! |
| Gal | Benvido ao Lostie Launcher! |
| Por | Bem-vindo ao Lostie Launcher! |
| Val | Benvingut al Lostie Launcher! |
| Fra | Bienvenue dans Lostie Launcher ! |

#### `WelcomeDialogDescription`

| Lang | Text |
| --- | --- |
| Esp | Descarga, actualiza y juega tus títulos favoritos en un solo lugar. Simple, rápido y sin complicaciones.<br><br>Tu privacidad es importante. No recopilamos ninguna información ni dato de ningún tipo.<br><br>Este proyecto es opensource. ¿Dudas sobre cómo funciona? Consulta el código fuente |
| Eng | Download, update, and play your favorite games in one place. Simple, fast, and hassle-free.<br><br>Your privacy is important. We don't collect any information or data of any kind.<br><br>This project is open source. Questions about how it works? Check the source code |
| Cat | Descarrega, actualitza i juga els teus jocs favorits en un sol lloc. Simple, ràpid i sense complicacions.<br><br>La teva privacitat és important. No recollim cap tipus d'informació ni de dades.<br><br>Aquest projecte és opensource. Dubtes sobre com funciona? Consulta el codi font |
| Eus | Deskargatu, eguneratu eta jolastu zure joko gogokoak leku batean. Sinplea, azkarra eta konplikazio gabe.<br><br>Zure pribatutasuna garrantzitsua da. Ez dugu inolako informaziorik ez daturik biltzen.<br><br>Proiektu hau opensource. Zalantzak nola funtzionatzen duen jakin nahi? Bilatu iturburu kodea |
| Gal | Descarga, actualiza e xoga os teus xogos favoritos nun só lugar. Simple, rápido e sen complicacións.<br><br>A túa privacidade é importante. Non recollemos ningún tipo de información nin de datos.<br><br>Este proxecto é opensource. Dúbidas sobre como funciona? Consulta o código fonte |
| Por | Baixe, atualize e jogue seus jogos favoritos em um único lugar. Simples, rápido e sem complicações.<br><br>Sua privacidade é importante. Não coletamos nenhum tipo de informação ou dado.<br><br>Este projeto é open source. Dúvidas sobre como funciona? Consulte o código-fonte |
| Val | Descarrega, actualitza i juga els teus jocs favorits en un sol lloc. Simple, ràpid i sense complicacions.<br><br>La teva privacitat és important. No recollim cap tipus d'informació ni de dades.<br><br>Est projecte és opensource. Dubtes sobre com funciona? Consulta el codi font |
| Fra | Téléchargez, mettez à jour et jouez à vos jeux préférés en un seul endroit. Simple, rapide et sans tracas.<br><br>Votre vie privée compte. Nous ne collectons aucune information ni donnée d'aucune sorte.<br><br>Ce projet est open source. Des questions sur le fonctionnement ? Consultez le code source |

#### `WelcomeDialogContinue`

| Lang | Text |
| --- | --- |
| Esp | Continuar |
| Eng | Continue |
| Cat | Continuar |
| Eus | Jarraitu |
| Gal | Continuar |
| Por | Continuar |
| Val | Continuar |
| Fra | Continuer |

#### `RepositoryUrl`

| Lang | Text |
| --- | --- |
| Esp | https://github.com/jagobainda/LostieLauncher |
| Eng | https://github.com/jagobainda/LostieLauncher |
| Cat | https://github.com/jagobainda/LostieLauncher |
| Eus | https://github.com/jagobainda/LostieLauncher |
| Gal | https://github.com/jagobainda/LostieLauncher |
| Por | https://github.com/jagobainda/LostieLauncher |
| Val | https://github.com/jagobainda/LostieLauncher |
| Fra | https://github.com/jagobainda/LostieLauncher |

#### `SpecialVersionDialogTitle`

| Lang | Text |
| --- | --- |
| Esp | Cambiar a versión especial |
| Eng | Switch to special version |
| Cat | Canviar a versió especial |
| Eus | Bertsio berezira aldatu |
| Gal | Cambiar a versión especial |
| Por | Mudar para versão especial |
| Val | Canviar a versió especial |
| Fra | Passer à la version spéciale |

#### `SpecialVersionDialogDescription`

| Lang | Text |
| --- | --- |
| Esp | Al cambiar a una versión especial no se pierde nada, funciona como una actualización normal. |
| Eng | Switching to a special version won't lose anything, it works like a normal update. |
| Cat | En canviar a una versió especial no es perd res, funciona com una actualització normal. |
| Eus | Bertsio berezi batera aldatzean ez da ezer galtzen, eguneraketa normal bat bezala funtzionatzen du. |
| Gal | Ao cambiar a unha versión especial non se perde nada, funciona como unha actualización normal. |
| Por | Ao mudar para uma versão especial não se perde nada, funciona como uma atualização normal. |
| Val | En canviar a una versió especial no es perd res, funciona com una actualització normal. |
| Fra | En passant à une version spéciale, vous ne perdez rien, cela fonctionne comme une mise à jour normale. |

#### `SpecialVersionDialogKeyLabel`

| Lang | Text |
| --- | --- |
| Esp | Clave de versión especial |
| Eng | Special version key |
| Cat | Clau de versió especial |
| Eus | Bertsio bereziaren gakoa |
| Gal | Clave de versión especial |
| Por | Chave de versão especial |
| Val | Clau de versió especial |
| Fra | Clé de version spéciale |

#### `BtnConfirm`

| Lang | Text |
| --- | --- |
| Esp | Confirmar |
| Eng | Confirm |
| Cat | Confirmar |
| Eus | Berretsi |
| Gal | Confirmar |
| Por | Confirmar |
| Val | Confirmar |
| Fra | Confirmer |

#### `DownloadKeyNotFoundTitle`

| Lang | Text |
| --- | --- |
| Esp | Clave no encontrada |
| Eng | Key not found |
| Cat | Clau no trobada |
| Eus | Gakoa ez da aurkitu |
| Gal | Clave non atopada |
| Por | Chave não encontrada |
| Val | Clau no trobada |
| Fra | Clé non trouvée |

#### `DownloadKeyNotFoundMessage`

| Lang | Text |
| --- | --- |
| Esp | No se ha encontrado una versión especial con esta clave. Comprueba la clave e inténtalo de nuevo. |
| Eng | No special version was found with this key. Please check the key and try again. |
| Cat | No s'ha trobat cap versió especial amb aquesta clau. Comprova la clau i torna-ho a intentar. |
| Eus | Ez da gako honekin bertsio berezirik aurkitu. Egiaztatu gakoa eta saiatu berriro. |
| Gal | Non se atopou ningunha versión especial con esta clave. Comproba a clave e téntao de novo. |
| Por | Não foi encontrada nenhuma versão especial com esta chave. Verifique a chave e tente novamente. |
| Val | No s'ha trobat cap versió especial amb esta clau. Comprova la clau i torna-ho a intentar. |
| Fra | Aucune version spéciale n'a été trouvée avec cette clé. Vérifiez la clé et réessayez. |

#### `DownloadKeyMismatchTitle`

| Lang | Text |
| --- | --- |
| Esp | Clave incorrecta |
| Eng | Incorrect key |
| Cat | Clau incorrecta |
| Eus | Gako okerra |
| Gal | Clave incorrecta |
| Por | Chave incorreta |
| Val | Clau incorrecta |
| Fra | Clé incorrecte |

#### `DownloadKeyMismatchMessage`

| Lang | Text |
| --- | --- |
| Esp | La clave no corresponde a este juego. |
| Eng | The key does not match this game. |
| Cat | La clau no correspon a aquest joc. |
| Eus | Gakoa ez dator bat joko honekin. |
| Gal | A clave non corresponde a este xogo. |
| Por | A chave não corresponde a este jogo. |
| Val | La clau no correspon a este joc. |
| Fra | La clé ne correspond pas à ce jeu. |

#### `TooltipSwitchSpecialVersion`

| Lang | Text |
| --- | --- |
| Esp | Cambiar a versión especial |
| Eng | Switch to special version |
| Cat | Canviar a versió especial |
| Eus | Bertsio berezira aldatu |
| Gal | Cambiar a versión especial |
| Por | Mudar para versão especial |
| Val | Canviar a versió especial |
| Fra | Passer à la version spéciale |

#### `ServerActionsUnavailableTitle`

| Lang | Text |
| --- | --- |
| Esp | Servidor en mantenimiento |
| Eng | Server maintenance |
| Cat | Servidor en manteniment |
| Eus | Zerbitzaria mantentze-lanetan |
| Gal | Servidor en mantemento |
| Por | Servidor em manutenção |
| Val | Servidor en manteniment |
| Fra | Serveur en maintenance |

#### `ServerActionsUnavailableMessage`

| Lang | Text |
| --- | --- |
| Esp | El servidor está en mantenimiento. Las descargas, actualizaciones y versiones especiales volverán en cuanto termine. |
| Eng | The server is under maintenance. Downloads, updates, and special versions will return as soon as it is finished. |
| Cat | El servidor està en manteniment. Les descàrregues, actualitzacions i versions especials tornaran quan acabe. |
| Eus | Zerbitzaria mantentze-lanetan dago. Deskargak, eguneraketak eta bertsio bereziak amaitzean itzuliko dira. |
| Gal | O servidor está en mantemento. As descargas, actualizacións e versións especiais volverán cando remate. |
| Por | O servidor está em manutenção. Downloads, atualizações e versões especiais voltarão assim que terminar. |
| Val | El servidor està en manteniment. Les descàrregues, actualitzacions i versions especials tornaran quan acabe. |
| Fra | Le serveur est en maintenance. Les téléchargements, mises à jour et versions spéciales reviendront dès que ce sera terminé. |

#### `OfflineModeLabel`

| Lang | Text |
| --- | --- |
| Esp | Modo offline |
| Eng | Offline mode |
| Cat | Mode offline |
| Eus | Offline modua |
| Gal | Modo offline |
| Por | Modo offline |
| Val | Mode offline |
| Fra | Mode hors ligne |

#### `ServerMaintenanceNotificationTitle`

| Lang | Text |
| --- | --- |
| Esp | Servidor en mantenimiento |
| Eng | Server maintenance |
| Cat | Servidor en manteniment |
| Eus | Zerbitzaria mantentze-lanetan |
| Gal | Servidor en mantemento |
| Por | Servidor em manutenção |
| Val | Servidor en manteniment |
| Fra | Serveur en maintenance |

#### `ServerMaintenanceNotificationMessage`

| Lang | Text |
| --- | --- |
| Esp | El launcher está en modo offline. Puedes seguir viendo tus juegos instalados; las descargas y actualizaciones se reactivarán automáticamente cuando el servicio vuelva. |
| Eng | The launcher is in offline mode. You can keep viewing your installed games; downloads and updates will reactivate automatically when the service returns. |
| Cat | El launcher està en mode offline. Pots continuar veient els jocs instal·lats; les descàrregues i actualitzacions es reactivaran automàticament quan torne el servei. |
| Eus | Launcher-a offline moduan dago. Instalatutako jokoak ikusten jarrai dezakezu; deskargak eta eguneraketak automatikoki berraktibatuko dira zerbitzua itzultzen denean. |
| Gal | O launcher está en modo offline. Podes seguir vendo os teus xogos instalados; as descargas e actualizacións reactivaranse automaticamente cando volva o servizo. |
| Por | O launcher está em modo offline. Você pode continuar vendo seus jogos instalados; downloads e atualizações serão reativados automaticamente quando o serviço voltar. |
| Val | El launcher està en mode offline. Pots continuar veient els jocs instal·lats; les descàrregues i actualitzacions es reactivaran automàticament quan torne el servei. |
| Fra | Le launcher est en mode hors ligne. Vous pouvez continuer à voir vos jeux installés ; les téléchargements et mises à jour se réactiveront automatiquement au retour du service. |

#### `HomeContentUnavailable`

| Lang | Text |
| --- | --- |
| Esp | No se ha podido cargar el contenido |
| Eng | Content could not be loaded |
| Cat | No s'ha pogut carregar el contingut |
| Eus | Ezin izan da edukia kargatu |
| Gal | Non se puido cargar o contido |
| Por | Não foi possível carregar o conteúdo |
| Val | No s'ha pogut carregar el contingut |
| Fra | Le contenu n'a pas pu être chargé |

#### `ContentOutOfDateLabel`

| Lang | Text |
| --- | --- |
| Esp | Contenido desactualizado |
| Eng | Content may be out of date |
| Cat | Contingut desactualitzat |
| Eus | Eduki zaharkitua |
| Gal | Contido desactualizado |
| Por | Conteúdo desatualizado |
| Val | Contingut desactualitzat |
| Fra | Contenu peut-être obsolète |

#### `ContentOutOfDateMessage`

| Lang | Text |
| --- | --- |
| Esp | No se ha podido conectar con el servidor. Se muestra el último contenido conocido, que puede estar desactualizado. |
| Eng | The server could not be reached. Showing the last known content, which may be out of date. |
| Cat | No s'ha pogut connectar amb el servidor. Es mostra l'últim contingut conegut, que pot estar desactualitzat. |
| Eus | Ezin izan da zerbitzariarekin konektatu. Ezagutzen den azken edukia erakusten da, eta zaharkituta egon daiteke. |
| Gal | Non se puido conectar co servidor. Amósase o último contido coñecido, que pode estar desactualizado. |
| Por | Não foi possível ligar ao servidor. A mostrar o último conteúdo conhecido, que pode estar desatualizado. |
| Val | No s'ha pogut connectar amb el servidor. Es mostra l'últim contingut conegut, que pot estar desactualitzat. |
| Fra | Le serveur est injoignable. Le dernier contenu connu est affiché ; il peut être obsolète. |

#### `SettingsGamesStoredIn`

| Lang | Text |
| --- | --- |
| Esp | Los juegos se guardan en: |
| Eng | Games are stored in: |
| Cat | Els jocs es desen a: |
| Eus | Jokoak hemen gordetzen dira: |
| Gal | Os xogos gárdanse en: |
| Por | Os jogos são guardados em: |
| Val | Els jocs es guarden en: |
| Fra | Les jeux sont stockés dans : |

#### `SettingsOneDriveWarning`

| Lang | Text |
| --- | --- |
| Esp | Esta carpeta la sincroniza OneDrive: los juegos ocuparán tu almacenamiento en la nube y pueden fallar al abrirse sin conexión. Es mejor elegir otra ubicación. |
| Eng | This folder is synced by OneDrive: games will use your cloud storage and may fail to start offline. Choosing another location is safer. |
| Cat | Aquesta carpeta la sincronitza OneDrive: els jocs ocuparan el teu emmagatzematge al núvol i poden fallar en obrir-se sense connexió. És millor triar una altra ubicació. |
| Eus | Karpeta hau OneDrivek sinkronizatzen du: jokoek zure hodeiko biltegia beteko dute eta baliteke konexiorik gabe ez irekitzea. Hobe da beste kokapen bat aukeratzea. |
| Gal | Esta carpeta sincronízaa OneDrive: os xogos ocuparán o teu almacenamento na nube e poden fallar ao abrirse sen conexión. É mellor escoller outra localización. |
| Por | Esta pasta é sincronizada pelo OneDrive: os jogos vão ocupar o teu armazenamento na nuvem e podem falhar ao abrir sem ligação. É melhor escolher outra localização. |
| Val | Esta carpeta la sincronitza OneDrive: els jocs ocuparan el teu emmagatzematge en el núvol i poden fallar en obrir-se sense connexió. És millor triar una altra ubicació. |
| Fra | Ce dossier est synchronisé par OneDrive : les jeux occuperont votre stockage cloud et peuvent ne pas démarrer hors ligne. Il vaut mieux choisir un autre emplacement. |

#### `OneDriveWarningTitle`

| Lang | Text |
| --- | --- |
| Esp | Carpeta sincronizada con OneDrive |
| Eng | OneDrive-synced folder |
| Cat | Carpeta sincronitzada amb OneDrive |
| Eus | OneDriverekin sinkronizatutako karpeta |
| Gal | Carpeta sincronizada con OneDrive |
| Por | Pasta sincronizada com o OneDrive |
| Val | Carpeta sincronitzada amb OneDrive |
| Fra | Dossier synchronisé par OneDrive |

#### `OneDriveWarningMessage`

Placeholders: `{0}`

| Lang | Text |
| --- | --- |
| Esp | Esta carpeta la sincroniza OneDrive:<br><br>{0}<br><br>Cada juego ocupa varios gigas, así que se subirían a tu almacenamiento en la nube, la sincronización competiría con las instalaciones y los archivos podrían quedarse solo en línea y no abrirse sin conexión.<br><br>¿Quieres usarla de todas formas? |
| Eng | This folder is synced by OneDrive:<br><br>{0}<br><br>Games are several gigabytes each, so they would be uploaded to your cloud storage, the sync engine would compete with installs, and files can be left online-only and fail to start offline.<br><br>Do you want to use it anyway? |
| Cat | Aquesta carpeta la sincronitza OneDrive:<br><br>{0}<br><br>Cada joc ocupa diversos gigues, així que es pujarien al teu emmagatzematge al núvol, la sincronització competiria amb les instal·lacions i els fitxers es podrien quedar només en línia i no obrir-se sense connexió.<br><br>Vols fer-la servir igualment? |
| Eus | Karpeta hau OneDrivek sinkronizatzen du:<br><br>{0}<br><br>Joko bakoitzak hainbat giga hartzen ditu, beraz zure hodeiko biltegira igoko lirateke, sinkronizazioak instalazioekin lehiatuko luke eta fitxategiak sarean bakarrik gera litezke, konexiorik gabe ireki ezinik.<br><br>Hala ere erabili nahi duzu? |
| Gal | Esta carpeta sincronízaa OneDrive:<br><br>{0}<br><br>Cada xogo ocupa varios xigas, así que se subirían ao teu almacenamento na nube, a sincronización competiría coas instalacións e os ficheiros poderían quedar só en liña e non abrirse sen conexión.<br><br>Queres usala de todos os xeitos? |
| Por | Esta pasta é sincronizada pelo OneDrive:<br><br>{0}<br><br>Cada jogo ocupa vários gigabytes, por isso seriam enviados para o teu armazenamento na nuvem, a sincronização competiria com as instalações e os ficheiros podem ficar apenas online e não abrir sem ligação.<br><br>Queres usá-la mesmo assim? |
| Val | Esta carpeta la sincronitza OneDrive:<br><br>{0}<br><br>Cada joc ocupa diversos gigues, així que es pujarien al teu emmagatzematge en el núvol, la sincronització competiria amb les instal·lacions i els fitxers es podrien quedar només en línia i no obrir-se sense connexió.<br><br>Vols utilitzar-la igualment? |
| Fra | Ce dossier est synchronisé par OneDrive :<br><br>{0}<br><br>Chaque jeu pèse plusieurs gigaoctets ; ils seraient donc envoyés vers votre stockage cloud, la synchronisation entrerait en concurrence avec les installations et les fichiers pourraient rester en ligne uniquement et ne pas démarrer hors ligne.<br><br>Voulez-vous l'utiliser quand même ? |

#### `DownloadDirNotUsableTitle`

| Lang | Text |
| --- | --- |
| Esp | Carpeta de descargas no válida |
| Eng | Download folder not usable |
| Cat | Carpeta de descàrregues no vàlida |
| Eus | Deskarga-karpeta baliogabea |
| Gal | Carpeta de descargas non válida |
| Por | Pasta de downloads inválida |
| Val | Carpeta de descàrregues no vàlida |
| Fra | Dossier de téléchargement invalide |

#### `DownloadDirNotUsableMessage`

Placeholders: `{0}`, `{1}`

| Lang | Text |
| --- | --- |
| Esp | El launcher no puede usar esta carpeta:<br><br>{0}<br><br>La comprobación falló al {1}.<br><br>Elige otra carpeta en Ajustes. |
| Eng | The launcher cannot use this folder:<br><br>{0}<br><br>The check failed while {1}.<br><br>Choose another folder in Settings. |
| Cat | El launcher no pot fer servir aquesta carpeta:<br><br>{0}<br><br>La comprovació ha fallat en {1}.<br><br>Tria una altra carpeta a Configuració. |
| Eus | Launcher-ak ezin du karpeta hau erabili:<br><br>{0}<br><br>Egiaztapenak huts egin du {1}.<br><br>Aukeratu beste karpeta bat Ezarpenetan. |
| Gal | O launcher non pode usar esta carpeta:<br><br>{0}<br><br>A comprobación fallou ao {1}.<br><br>Escolle outra carpeta en Axustes. |
| Por | O launcher não pode usar esta pasta:<br><br>{0}<br><br>A verificação falhou ao {1}.<br><br>Escolhe outra pasta em Configurações. |
| Val | El launcher no pot utilitzar esta carpeta:<br><br>{0}<br><br>La comprovació ha fallat en {1}.<br><br>Tria una altra carpeta en Ajustos. |
| Fra | Le launcher ne peut pas utiliser ce dossier :<br><br>{0}<br><br>La vérification a échoué lors de {1}.<br><br>Choisissez un autre dossier dans les Paramètres. |

#### `DownloadDirStepCreate`

| Lang | Text |
| --- | --- |
| Esp | crear la carpeta |
| Eng | creating the folder |
| Cat | crear la carpeta |
| Eus | karpeta sortzean |
| Gal | crear a carpeta |
| Por | criar a pasta |
| Val | crear la carpeta |
| Fra | la création du dossier |

#### `DownloadDirStepWrite`

| Lang | Text |
| --- | --- |
| Esp | escribir un archivo de prueba |
| Eng | writing a test file |
| Cat | escriure un fitxer de prova |
| Eus | proba-fitxategi bat idaztean |
| Gal | escribir un ficheiro de proba |
| Por | escrever um ficheiro de teste |
| Val | escriure un fitxer de prova |
| Fra | l'écriture d'un fichier de test |

#### `DownloadDirStepRename`

| Lang | Text |
| --- | --- |
| Esp | renombrar un archivo de prueba (la carpeta permite escribir, pero no renombrar ni borrar) |
| Eng | renaming a test file (the folder allows writing, but not renaming or deleting) |
| Cat | reanomenar un fitxer de prova (la carpeta permet escriure, però no reanomenar ni esborrar) |
| Eus | proba-fitxategi bat berrizendatzean (karpetak idaztea onartzen du, baina ez berrizendatzea edo ezabatzea) |
| Gal | renomear un ficheiro de proba (a carpeta permite escribir, pero non renomear nin borrar) |
| Por | mudar o nome de um ficheiro de teste (a pasta permite escrever, mas não mudar o nome nem apagar) |
| Val | reanomenar un fitxer de prova (la carpeta permet escriure, però no reanomenar ni esborrar) |
| Fra | le renommage d'un fichier de test (le dossier autorise l'écriture, mais pas le renommage ni la suppression) |


---

## FAQ entries

Six entries per language, in fixed order, selected by a single switch on the
language. They are plain question/answer pairs with no keys: position is the
identity, so entry 3 is the same question in all eight languages.

Two behaviours belong to the FAQ text rather than to the screen:

- **Answers are scanned for links** and rendered with the URLs as clickable
  hyperlinks. The parser and its exact rules are in section 09; the entries
  below contain bare domains such as `github.com/...` and channel names such as
  `#testeo-launcher`, and only the former become links.
- **Search highlights matches** inside both question and answer, ignoring case
  and diacritics.

#### Entry 1

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | ¿Cómo descargo un juego? | Ve a la Biblioteca, elige el juego que quieras y pulsa Descargar. Cuando termine la instalación, aparecerá en Mis Juegos listo para jugar. |
| Eng | How do I download a game? | Go to the Library, pick the game you want and press Download. Once the installation finishes, it will appear in My Games ready to play. |
| Cat | Com descarrego un joc? | Ves a la Biblioteca, tria el joc que vulguis i prem Descarregar. Quan acabi la instal·lació, apareixerà a Els meus jocs a punt per jugar. |
| Eus | Nola deskargatzen dut joko bat? | Joan Liburutegira, aukeratu nahi duzun jokoa eta sakatu Deskargatu. Instalazioa amaitzean, Nire Jokoak atalean agertuko da jolasteko prest. |
| Gal | Como descargo un xogo? | Vai á Biblioteca, escolle o xogo que queiras e preme Descargar. Cando remate a instalación, aparecerá en Os meus xogos listo para xogar. |
| Por | Como baixo um jogo? | Vá à Biblioteca, escolha o jogo que deseja e pressione Baixar. Quando a instalação terminar, ele aparecerá em Meus Jogos pronto para jogar. |
| Val | Com descarregue un joc? | Ves a la Biblioteca, tria el joc que vulgues i prem Descarregar. Quan acabe la instal·lació, apareixerà a Els meus jocs a punt per a jugar. |
| Fra | Comment télécharger un jeu ? | Allez dans la Bibliothèque, choisissez le jeu souhaité et appuyez sur Télécharger. Une fois l'installation terminée, il apparaîtra dans Mes jeux, prêt à jouer. |

#### Entry 2

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | ¿Dónde se instalan los juegos y cómo cambio la carpeta? | Los juegos se instalan en el directorio de descargas configurado. Puedes cambiarlo en Ajustes, en la opción Directorio de descargas. Si ya tienes juegos instalados, tendrás que moverlos manualmente a la nueva ruta. |
| Eng | Where are games installed and how do I change the folder? | Games are installed in the configured download directory. You can change it in Settings, under Download directory. If you already have games installed, you will need to move them manually to the new path. |
| Cat | On s'instal·len els jocs i com canvio la carpeta? | Els jocs s'instal·len al directori de descàrregues configurat. Pots canviar-lo a Configuració, a l'opció Directori de descàrregues. Si ja tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta. |
| Eus | Non instalatzen dira jokoak eta nola aldatzen dut karpeta? | Jokoak konfiguratutako deskarga direktorioan instalatzen dira. Ezarpenetan alda dezakezu, Deskarga direktorioa aukeran. Dagoeneko jokoak instalatuta badituzu, eskuz mugitu beharko dituzu bide berrira. |
| Gal | Onde se instalan os xogos e como cambio o cartafol? | Os xogos instálanse no directorio de descargas configurado. Podes cambialo en Axustes, na opción Directorio de descargas. Se xa tes xogos instalados, terás que movelos manualmente á nova ruta. |
| Por | Onde os jogos são instalados e como mudo a pasta? | Os jogos são instalados no diretório de downloads configurado. Você pode alterá-lo em Configurações, na opção Diretório de downloads. Se você já tiver jogos instalados, precisará movê-los manualmente para o novo caminho. |
| Val | On s'instal·len els jocs i com canvie la carpeta? | Els jocs s'instal·len al directori de descàrregues configurat. Pots canviar-lo a Ajustos, a l'opció Directori de descàrregues. Si ja tens jocs instal·lats, hauràs de moure'ls manualment a la nova ruta. |
| Fra | Où les jeux sont-ils installés et comment changer de dossier ? | Les jeux sont installés dans le répertoire de téléchargement configuré. Vous pouvez le modifier dans Paramètres, sous Répertoire de téléchargement. Si vous avez déjà des jeux installés, vous devrez les déplacer manuellement vers le nouveau chemin. |

#### Entry 3

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | ¿Perderé mis partidas guardadas al actualizar o desinstalar un juego? | No. Las partidas guardadas y el registro de tiempo jugado se conservan siempre, tanto al actualizar como al desinstalar un juego. |
| Eng | Will I lose my saved games when updating or uninstalling a game? | No. Saved games and the playtime record are always kept, both when updating and when uninstalling a game. |
| Cat | Perdré les meves partides desades en actualitzar o desinstal·lar un joc? | No. Les partides desades i el registre de temps jugat es conserven sempre, tant en actualitzar com en desinstal·lar un joc. |
| Eus | Gordetako partidak galduko ditut joko bat eguneratzean edo desinstalatzean? | Ez. Gordetako partidak eta jolasdenboraren erregistroa beti mantentzen dira, bai eguneratzean bai desinstalatzean. |
| Gal | Perderei as miñas partidas gardadas ao actualizar ou desinstalar un xogo? | Non. As partidas gardadas e o rexistro de tempo xogado consérvanse sempre, tanto ao actualizar como ao desinstalar un xogo. |
| Por | Vou perder meus saves ao atualizar ou desinstalar um jogo? | Não. Os saves e o registro de tempo de jogo são sempre mantidos, tanto ao atualizar quanto ao desinstalar um jogo. |
| Val | Perdré les meues partides guardades en actualitzar o desinstal·lar un joc? | No. Les partides guardades i el registre de temps jugat es conserven sempre, tant en actualitzar com en desinstal·lar un joc. |
| Fra | Vais-je perdre mes sauvegardes en mettant à jour ou en désinstallant un jeu ? | Non. Les sauvegardes et le registre de temps de jeu sont toujours conservés, aussi bien lors d'une mise à jour que d'une désinstallation. |

#### Entry 4

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | ¿Qué es una versión especial y cómo la activo? | Es una versión alternativa de un juego que se desbloquea con una clave con formato XXXX-XXXX-XXXX-XXXX-XXXX. Puedes introducir la clave al descargar el juego o cambiar a la versión especial desde Mis Juegos sin perder nada. |
| Eng | What is a special version and how do I activate it? | It is an alternative version of a game unlocked with a key in the format XXXX-XXXX-XXXX-XXXX-XXXX. You can enter the key when downloading the game or switch to the special version from My Games without losing anything. |
| Cat | Què és una versió especial i com l'activo? | És una versió alternativa d'un joc que es desbloqueja amb una clau amb format XXXX-XXXX-XXXX-XXXX-XXXX. Pots introduir la clau en descarregar el joc o canviar a la versió especial des d'Els meus jocs sense perdre res. |
| Eus | Zer da bertsio berezi bat eta nola aktibatzen dut? | Jokoaren bertsio alternatibo bat da, XXXX-XXXX-XXXX-XXXX-XXXX formatuko gako batekin desblokeatzen dena. Gakoa jokoa deskargatzean sar dezakezu, edo bertsio berezira aldatu Nire Jokoak ataletik ezer galdu gabe. |
| Gal | Que é unha versión especial e como a activo? | É unha versión alternativa dun xogo que se desbloquea cunha clave co formato XXXX-XXXX-XXXX-XXXX-XXXX. Podes introducir a clave ao descargar o xogo ou cambiar á versión especial desde Os meus xogos sen perder nada. |
| Por | O que é uma versão especial e como a ativo? | É uma versão alternativa de um jogo desbloqueada com uma chave no formato XXXX-XXXX-XXXX-XXXX-XXXX. Você pode inserir a chave ao baixar o jogo ou mudar para a versão especial em Meus Jogos sem perder nada. |
| Val | Què és una versió especial i com l'active? | És una versió alternativa d'un joc que es desbloqueja amb una clau amb format XXXX-XXXX-XXXX-XXXX-XXXX. Pots introduir la clau en descarregar el joc o canviar a la versió especial des d'Els meus jocs sense perdre res. |
| Fra | Qu'est-ce qu'une version spéciale et comment l'activer ? | C'est une version alternative d'un jeu qui se débloque avec une clé au format XXXX-XXXX-XXXX-XXXX-XXXX. Vous pouvez saisir la clé lors du téléchargement du jeu ou passer à la version spéciale depuis Mes jeux sans rien perdre. |

#### Entry 5

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | ¿Qué significa el modo offline? | Significa que el launcher no puede conectar con el servidor, ya sea porque no tienes conexión a internet o porque el servidor está en mantenimiento. Puedes seguir jugando a tus juegos instalados; las descargas, actualizaciones y versiones especiales se reactivarán automáticamente cuando vuelva la conexión. |
| Eng | What does offline mode mean? | It means the launcher can't reach the server, either because you have no internet connection or because the server is under maintenance. You can keep playing your installed games; downloads, updates and special versions will reactivate automatically when the connection returns. |
| Cat | Què significa el mode offline? | Significa que el launcher no pot connectar amb el servidor, ja sigui perquè no tens connexió a internet o perquè el servidor està en manteniment. Pots seguir jugant als teus jocs instal·lats; les descàrregues, actualitzacions i versions especials es reactivaran automàticament quan torni la connexió. |
| Eus | Zer esan nahi du offline moduak? | Launcher-a zerbitzariarekin konektatu ezin dela esan nahi du, interneteko konexiorik ez duzulako edo zerbitzaria mantentze-lanetan dagoelako. Instalatutako jokoetan jolasten jarrai dezakezu; deskargak, eguneraketak eta bertsio bereziak automatikoki berraktibatuko dira konexioa itzultzean. |
| Gal | Que significa o modo offline? | Significa que o launcher non pode conectar co servidor, xa sexa porque non tes conexión a internet ou porque o servidor está en mantemento. Podes seguir xogando aos teus xogos instalados; as descargas, actualizacións e versións especiais reactivaranse automaticamente cando volva a conexión. |
| Por | O que significa o modo offline? | Significa que o launcher não consegue se conectar ao servidor, seja porque você está sem conexão com a internet ou porque o servidor está em manutenção. Você pode continuar jogando seus jogos instalados; downloads, atualizações e versões especiais serão reativados automaticamente quando a conexão voltar. |
| Val | Què significa el mode offline? | Significa que el launcher no pot connectar amb el servidor, ja siga perquè no tens connexió a internet o perquè el servidor està en manteniment. Pots continuar jugant als teus jocs instal·lats; les descàrregues, actualitzacions i versions especials es reactivaran automàticament quan torne la connexió. |
| Fra | Que signifie le mode hors ligne ? | Cela signifie que le launcher ne peut pas se connecter au serveur, soit parce que vous n'avez pas de connexion internet, soit parce que le serveur est en maintenance. Vous pouvez continuer à jouer à vos jeux installés ; les téléchargements, mises à jour et versions spéciales se réactiveront automatiquement au retour de la connexion. |

#### Entry 6

| Lang | Question | Answer |
| --- | --- | --- |
| Esp | He encontrado un error, ¿dónde lo reporto? | Escribe en el canal #testeo-launcher del Discord de la comunidad contando qué ha pasado y qué estabas haciendo. Cuanto más detalle des, más fácil será arreglarlo. |
| Eng | I found a bug, where do I report it? | Write in the #testeo-launcher channel of the community Discord explaining what happened and what you were doing. The more detail you give, the easier it will be to fix. |
| Cat | He trobat un error, on el reporto? | Escriu al canal #testeo-launcher del Discord de la comunitat explicant què ha passat i què estaves fent. Com més detall donis, més fàcil serà arreglar-ho. |
| Eus | Errore bat aurkitu dut, non jakinarazten dut? | Idatzi komunitatearen Discord-eko #testeo-launcher kanalean, zer gertatu den eta zer egiten ari zinen azalduz. Zenbat eta xehetasun gehiago eman, orduan eta errazagoa izango da konpontzea. |
| Gal | Atopei un erro, onde o reporto? | Escribe na canle #testeo-launcher do Discord da comunidade contando que pasou e que estabas a facer. Canto máis detalle deas, máis fácil será arranxalo. |
| Por | Encontrei um erro, onde o reporto? | Escreva no canal #testeo-launcher do Discord da comunidade contando o que aconteceu e o que você estava fazendo. Quanto mais detalhes você der, mais fácil será corrigir. |
| Val | He trobat un error, on el reporte? | Escriu al canal #testeo-launcher del Discord de la comunitat explicant què ha passat i què estaves fent. Com més detall dones, més fàcil serà arreglar-ho. |
| Fra | J'ai trouvé un bug, où le signaler ? | Écrivez dans le canal #testeo-launcher du Discord de la communauté en expliquant ce qui s'est passé et ce que vous faisiez. Plus vous donnez de détails, plus il sera facile de le corriger. |

