@file:Suppress("ktlint:standard:max-line-length")

package dev.jagoba.lostielauncher.content

import dev.jagoba.lostielauncher.model.AppLanguage

fun faqsFor(language: AppLanguage): List<FaqEntry> = when (language) {
    AppLanguage.ESP -> EspFaqs
    AppLanguage.ENG -> EngFaqs
    AppLanguage.CAT -> CatFaqs
    AppLanguage.EUS -> EusFaqs
    AppLanguage.GAL -> GalFaqs
    AppLanguage.POR -> PorFaqs
    AppLanguage.VAL -> ValFaqs
    AppLanguage.FRA -> FraFaqs
}

private val EspFaqs = listOf(
    FaqEntry(
        "¿Cómo descargo un juego?",
        "Ve a la Biblioteca, elige el juego que quieras y pulsa Descargar. Cuando termine la instalación, aparecerá en Mis Juegos listo para jugar.",
    ),
    FaqEntry(
        "¿Dónde se instalan los juegos y cómo cambio la carpeta?",
        "En Android, los juegos se guardan en el almacenamiento propio del launcher y esa carpeta no se puede cambiar. Si desinstalas el launcher, los juegos descargados se borran con él.",
    ),
    FaqEntry(
        "¿Perderé mis partidas guardadas al actualizar o desinstalar un juego?",
        "No. Las partidas guardadas y el registro de tiempo jugado se conservan siempre, tanto al actualizar como al desinstalar un juego.",
    ),
    FaqEntry(
        "¿Qué es una versión especial y cómo la activo?",
        "Es una versión alternativa de un juego que se desbloquea con una clave con formato XXXX-XXXX-XXXX-XXXX-XXXX. Puedes introducir la clave al descargar el juego o cambiar a la versión especial desde Mis Juegos sin perder nada.",
    ),
    FaqEntry(
        "¿Qué significa el modo offline?",
        "Significa que el launcher no puede conectar con el servidor, ya sea porque no tienes conexión a internet o porque el servidor está en mantenimiento. Puedes seguir jugando a tus juegos instalados; las descargas, actualizaciones y versiones especiales se reactivarán automáticamente cuando vuelva la conexión.",
    ),
    FaqEntry(
        "He encontrado un error, ¿dónde lo reporto?",
        "Escribe en el canal #testeo-launcher del Discord de la comunidad contando qué ha pasado y qué estabas haciendo. Cuanto más detalle des, más fácil será arreglarlo.",
    ),
)

private val EngFaqs = listOf(
    FaqEntry(
        "How do I download a game?",
        "Go to the Library, pick the game you want and press Download. Once the installation finishes, it will appear in My Games ready to play.",
    ),
    FaqEntry(
        "Where are games installed and how do I change the folder?",
        "On Android, games are stored in the launcher's own storage, and that folder cannot be changed. If you uninstall the launcher, the downloaded games are removed with it.",
    ),
    FaqEntry(
        "Will I lose my saved games when updating or uninstalling a game?",
        "No. Saved games and the playtime record are always kept, both when updating and when uninstalling a game.",
    ),
    FaqEntry(
        "What is a special version and how do I activate it?",
        "It is an alternative version of a game unlocked with a key in the format XXXX-XXXX-XXXX-XXXX-XXXX. You can enter the key when downloading the game or switch to the special version from My Games without losing anything.",
    ),
    FaqEntry(
        "What does offline mode mean?",
        "It means the launcher can't reach the server, either because you have no internet connection or because the server is under maintenance. You can keep playing your installed games; downloads, updates and special versions will reactivate automatically when the connection returns.",
    ),
    FaqEntry(
        "I found a bug, where do I report it?",
        "Write in the #testeo-launcher channel of the community Discord explaining what happened and what you were doing. The more detail you give, the easier it will be to fix.",
    ),
)

private val CatFaqs = listOf(
    FaqEntry(
        "Com descarrego un joc?",
        "Ves a la Biblioteca, tria el joc que vulguis i prem Descarregar. Quan acabi la instal·lació, apareixerà a Els meus jocs a punt per jugar.",
    ),
    FaqEntry(
        "On s'instal·len els jocs i com canvio la carpeta?",
        "A Android, els jocs es guarden a l'emmagatzematge propi del launcher i aquesta carpeta no es pot canviar. Si desinstal·les el launcher, els jocs descarregats s'esborren amb ell.",
    ),
    FaqEntry(
        "Perdré les meves partides desades en actualitzar o desinstal·lar un joc?",
        "No. Les partides desades i el registre de temps jugat es conserven sempre, tant en actualitzar com en desinstal·lar un joc.",
    ),
    FaqEntry(
        "Què és una versió especial i com l'activo?",
        "És una versió alternativa d'un joc que es desbloqueja amb una clau amb format XXXX-XXXX-XXXX-XXXX-XXXX. Pots introduir la clau en descarregar el joc o canviar a la versió especial des d'Els meus jocs sense perdre res.",
    ),
    FaqEntry(
        "Què significa el mode offline?",
        "Significa que el launcher no pot connectar amb el servidor, ja sigui perquè no tens connexió a internet o perquè el servidor està en manteniment. Pots seguir jugant als teus jocs instal·lats; les descàrregues, actualitzacions i versions especials es reactivaran automàticament quan torni la connexió.",
    ),
    FaqEntry(
        "He trobat un error, on el reporto?",
        "Escriu al canal #testeo-launcher del Discord de la comunitat explicant què ha passat i què estaves fent. Com més detall donis, més fàcil serà arreglar-ho.",
    ),
)

private val EusFaqs = listOf(
    FaqEntry(
        "Nola deskargatzen dut joko bat?",
        "Joan Liburutegira, aukeratu nahi duzun jokoa eta sakatu Deskargatu. Instalazioa amaitzean, Nire Jokoak atalean agertuko da jolasteko prest.",
    ),
    FaqEntry(
        "Non instalatzen dira jokoak eta nola aldatzen dut karpeta?",
        "Android-en, jokoak launcher-aren biltegiratze propioan gordetzen dira, eta karpeta hori ezin da aldatu. Launcher-a desinstalatzen baduzu, deskargatutako jokoak ere ezabatu egiten dira.",
    ),
    FaqEntry(
        "Gordetako partidak galduko ditut joko bat eguneratzean edo desinstalatzean?",
        "Ez. Gordetako partidak eta jolasdenboraren erregistroa beti mantentzen dira, bai eguneratzean bai desinstalatzean.",
    ),
    FaqEntry(
        "Zer da bertsio berezi bat eta nola aktibatzen dut?",
        "Jokoaren bertsio alternatibo bat da, XXXX-XXXX-XXXX-XXXX-XXXX formatuko gako batekin desblokeatzen dena. Gakoa jokoa deskargatzean sar dezakezu, edo bertsio berezira aldatu Nire Jokoak ataletik ezer galdu gabe.",
    ),
    FaqEntry(
        "Zer esan nahi du offline moduak?",
        "Launcher-a zerbitzariarekin konektatu ezin dela esan nahi du, interneteko konexiorik ez duzulako edo zerbitzaria mantentze-lanetan dagoelako. Instalatutako jokoetan jolasten jarrai dezakezu; deskargak, eguneraketak eta bertsio bereziak automatikoki berraktibatuko dira konexioa itzultzean.",
    ),
    FaqEntry(
        "Errore bat aurkitu dut, non jakinarazten dut?",
        "Idatzi komunitatearen Discord-eko #testeo-launcher kanalean, zer gertatu den eta zer egiten ari zinen azalduz. Zenbat eta xehetasun gehiago eman, orduan eta errazagoa izango da konpontzea.",
    ),
)

private val GalFaqs = listOf(
    FaqEntry(
        "Como descargo un xogo?",
        "Vai á Biblioteca, escolle o xogo que queiras e preme Descargar. Cando remate a instalación, aparecerá en Os meus xogos listo para xogar.",
    ),
    FaqEntry(
        "Onde se instalan os xogos e como cambio o cartafol?",
        "En Android, os xogos gárdanse no almacenamento propio do launcher e ese cartafol non se pode cambiar. Se desinstalas o launcher, os xogos descargados bórranse con el.",
    ),
    FaqEntry(
        "Perderei as miñas partidas gardadas ao actualizar ou desinstalar un xogo?",
        "Non. As partidas gardadas e o rexistro de tempo xogado consérvanse sempre, tanto ao actualizar como ao desinstalar un xogo.",
    ),
    FaqEntry(
        "Que é unha versión especial e como a activo?",
        "É unha versión alternativa dun xogo que se desbloquea cunha clave co formato XXXX-XXXX-XXXX-XXXX-XXXX. Podes introducir a clave ao descargar o xogo ou cambiar á versión especial desde Os meus xogos sen perder nada.",
    ),
    FaqEntry(
        "Que significa o modo offline?",
        "Significa que o launcher non pode conectar co servidor, xa sexa porque non tes conexión a internet ou porque o servidor está en mantemento. Podes seguir xogando aos teus xogos instalados; as descargas, actualizacións e versións especiais reactivaranse automaticamente cando volva a conexión.",
    ),
    FaqEntry(
        "Atopei un erro, onde o reporto?",
        "Escribe na canle #testeo-launcher do Discord da comunidade contando que pasou e que estabas a facer. Canto máis detalle deas, máis fácil será arranxalo.",
    ),
)

private val PorFaqs = listOf(
    FaqEntry(
        "Como baixo um jogo?",
        "Vá à Biblioteca, escolha o jogo que deseja e pressione Baixar. Quando a instalação terminar, ele aparecerá em Meus Jogos pronto para jogar.",
    ),
    FaqEntry(
        "Onde os jogos são instalados e como mudo a pasta?",
        "No Android, os jogos são salvos no armazenamento próprio do launcher e essa pasta não pode ser alterada. Se você desinstalar o launcher, os jogos baixados são removidos junto com ele.",
    ),
    FaqEntry(
        "Vou perder meus saves ao atualizar ou desinstalar um jogo?",
        "Não. Os saves e o registro de tempo de jogo são sempre mantidos, tanto ao atualizar quanto ao desinstalar um jogo.",
    ),
    FaqEntry(
        "O que é uma versão especial e como a ativo?",
        "É uma versão alternativa de um jogo desbloqueada com uma chave no formato XXXX-XXXX-XXXX-XXXX-XXXX. Você pode inserir a chave ao baixar o jogo ou mudar para a versão especial em Meus Jogos sem perder nada.",
    ),
    FaqEntry(
        "O que significa o modo offline?",
        "Significa que o launcher não consegue se conectar ao servidor, seja porque você está sem conexão com a internet ou porque o servidor está em manutenção. Você pode continuar jogando seus jogos instalados; downloads, atualizações e versões especiais serão reativados automaticamente quando a conexão voltar.",
    ),
    FaqEntry(
        "Encontrei um erro, onde o reporto?",
        "Escreva no canal #testeo-launcher do Discord da comunidade contando o que aconteceu e o que você estava fazendo. Quanto mais detalhes você der, mais fácil será corrigir.",
    ),
)

private val ValFaqs = listOf(
    FaqEntry(
        "Com descarregue un joc?",
        "Ves a la Biblioteca, tria el joc que vulgues i prem Descarregar. Quan acabe la instal·lació, apareixerà a Els meus jocs a punt per a jugar.",
    ),
    FaqEntry(
        "On s'instal·len els jocs i com canvie la carpeta?",
        "En Android, els jocs es guarden en l'emmagatzematge propi del launcher i eixa carpeta no es pot canviar. Si desinstal·les el launcher, els jocs descarregats s'esborren amb ell.",
    ),
    FaqEntry(
        "Perdré les meues partides guardades en actualitzar o desinstal·lar un joc?",
        "No. Les partides guardades i el registre de temps jugat es conserven sempre, tant en actualitzar com en desinstal·lar un joc.",
    ),
    FaqEntry(
        "Què és una versió especial i com l'active?",
        "És una versió alternativa d'un joc que es desbloqueja amb una clau amb format XXXX-XXXX-XXXX-XXXX-XXXX. Pots introduir la clau en descarregar el joc o canviar a la versió especial des d'Els meus jocs sense perdre res.",
    ),
    FaqEntry(
        "Què significa el mode offline?",
        "Significa que el launcher no pot connectar amb el servidor, ja siga perquè no tens connexió a internet o perquè el servidor està en manteniment. Pots continuar jugant als teus jocs instal·lats; les descàrregues, actualitzacions i versions especials es reactivaran automàticament quan torne la connexió.",
    ),
    FaqEntry(
        "He trobat un error, on el reporte?",
        "Escriu al canal #testeo-launcher del Discord de la comunitat explicant què ha passat i què estaves fent. Com més detall dones, més fàcil serà arreglar-ho.",
    ),
)

private val FraFaqs = listOf(
    FaqEntry(
        "Comment télécharger un jeu ?",
        "Allez dans la Bibliothèque, choisissez le jeu souhaité et appuyez sur Télécharger. Une fois l'installation terminée, il apparaîtra dans Mes jeux, prêt à jouer.",
    ),
    FaqEntry(
        "Où les jeux sont-ils installés et comment changer de dossier ?",
        "Sur Android, les jeux sont enregistrés dans le stockage propre du launcher et ce dossier ne peut pas être modifié. Si vous désinstallez le launcher, les jeux téléchargés sont supprimés avec lui.",
    ),
    FaqEntry(
        "Vais-je perdre mes sauvegardes en mettant à jour ou en désinstallant un jeu ?",
        "Non. Les sauvegardes et le registre de temps de jeu sont toujours conservés, aussi bien lors d'une mise à jour que d'une désinstallation.",
    ),
    FaqEntry(
        "Qu'est-ce qu'une version spéciale et comment l'activer ?",
        "C'est une version alternative d'un jeu qui se débloque avec une clé au format XXXX-XXXX-XXXX-XXXX-XXXX. Vous pouvez saisir la clé lors du téléchargement du jeu ou passer à la version spéciale depuis Mes jeux sans rien perdre.",
    ),
    FaqEntry(
        "Que signifie le mode hors ligne ?",
        "Cela signifie que le launcher ne peut pas se connecter au serveur, soit parce que vous n'avez pas de connexion internet, soit parce que le serveur est en maintenance. Vous pouvez continuer à jouer à vos jeux installés ; les téléchargements, mises à jour et versions spéciales se réactiveront automatiquement au retour de la connexion.",
    ),
    FaqEntry(
        "J'ai trouvé un bug, où le signaler ?",
        "Écrivez dans le canal #testeo-launcher du Discord de la communauté en expliquant ce qui s'est passé et ce que vous faisiez. Plus vous donnez de détails, plus il sera facile de le corriger.",
    ),
)
