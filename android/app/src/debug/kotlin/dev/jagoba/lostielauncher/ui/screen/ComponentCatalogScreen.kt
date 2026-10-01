package dev.jagoba.lostielauncher.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.jagoba.lostielauncher.content.faqsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import dev.jagoba.lostielauncher.model.AppTheme
import dev.jagoba.lostielauncher.model.LibraryCardStatus
import dev.jagoba.lostielauncher.model.NotificationType
import dev.jagoba.lostielauncher.ui.LocalAppLanguage
import dev.jagoba.lostielauncher.ui.component.FaqCard
import dev.jagoba.lostielauncher.ui.component.GameCardSkeleton
import dev.jagoba.lostielauncher.ui.component.InstalledGameCard
import dev.jagoba.lostielauncher.ui.component.InstalledGameCardState
import dev.jagoba.lostielauncher.ui.component.LibraryGameCard
import dev.jagoba.lostielauncher.ui.component.LibraryGameCardState
import dev.jagoba.lostielauncher.ui.component.NewsCard
import dev.jagoba.lostielauncher.ui.component.NewsCardSkeleton
import dev.jagoba.lostielauncher.ui.component.NotificationCard
import dev.jagoba.lostielauncher.ui.component.NotificationCardSkeleton
import dev.jagoba.lostielauncher.ui.theme.LauncherSpacing
import dev.jagoba.lostielauncher.ui.theme.LocalLauncherColors
import dev.jagoba.lostielauncher.util.format.PlaytimeFormatter
import dev.jagoba.lostielauncher.util.format.RemainingTimeFormatter
import java.time.LocalDateTime

private const val LOGO_URL = "https://ericlostie-launcher.jagoba.dev/public/imgs/4.png"
private const val INSECURE_LOGO_URL = "http://ericlostie-launcher.jagoba.dev/public/imgs/2.png"
private val SampleDate: LocalDateTime = LocalDateTime.of(2026, 9, 26, 18, 30)

private val SampleLibrary = LibraryGameCardState(
    title = "Pokémon Añil",
    logoUrl = LOGO_URL,
    size = "588 MB",
    version = "v4.13.0",
    playtime = PlaytimeFormatter.format(754),
    status = LibraryCardStatus.AVAILABLE,
    progress = 42.4f,
    remainingTime = RemainingTimeFormatter.format(318_000_000, 1_050_000.0),
    speed = "1.0 MB/s",
)

private val SampleInstalled = InstalledGameCardState(
    title = "Pokémon Añil",
    logoUrl = LOGO_URL,
    installedVersion = "v4.12.0",
    specialType = null,
    updateVersion = null,
    playtime = PlaytimeFormatter.format(754),
    showHelp = false,
)

private val LibraryVariants: List<Pair<String, LibraryGameCardState>> = listOf(
    "AVAILABLE" to SampleLibrary,
    "AVAILABLE · disabled (canStart = false)" to SampleLibrary.copy(canStart = false),
    "AVAILABLE · no logo, no playtime" to SampleLibrary.copy(logoUrl = null, playtime = ""),
    "AVAILABLE · non-HTTPS logo is refused" to SampleLibrary.copy(logoUrl = INSECURE_LOGO_URL),
    "AVAILABLE · long title" to SampleLibrary.copy(title = "Pokémon Añil Edición Definitiva Coleccionista"),
    "DOWNLOADING" to SampleLibrary.copy(status = LibraryCardStatus.DOWNLOADING),
    "DOWNLOADING · no speed sample yet" to
        SampleLibrary.copy(status = LibraryCardStatus.DOWNLOADING, remainingTime = null, speed = "0 KB/s"),
    "DOWNLOADING · pause and cancel disabled" to
        SampleLibrary.copy(status = LibraryCardStatus.DOWNLOADING, canPause = false, canCancel = false),
    "PAUSED" to SampleLibrary.copy(status = LibraryCardStatus.PAUSED),
    "VERIFYING_INTEGRITY" to SampleLibrary.copy(status = LibraryCardStatus.VERIFYING_INTEGRITY),
    "EXTRACTING" to SampleLibrary.copy(status = LibraryCardStatus.EXTRACTING),
    "INSTALLATION_PENDING" to SampleLibrary.copy(status = LibraryCardStatus.INSTALLATION_PENDING),
    "INSTALLATION_UNSUPPORTED" to SampleLibrary.copy(status = LibraryCardStatus.INSTALLATION_UNSUPPORTED),
    "INSTALLATION_FAILED" to SampleLibrary.copy(status = LibraryCardStatus.INSTALLATION_FAILED),
    "DOWNLOADED" to SampleLibrary.copy(status = LibraryCardStatus.DOWNLOADED),
    "UPDATE_AVAILABLE" to SampleLibrary.copy(status = LibraryCardStatus.UPDATE_AVAILABLE),
    "UPDATE_AVAILABLE · disabled (canUpdate = false)" to
        SampleLibrary.copy(status = LibraryCardStatus.UPDATE_AVAILABLE, canUpdate = false),
)

private val InstalledVariants: List<Pair<String, InstalledGameCardState>> = listOf(
    "installed" to SampleInstalled,
    "no playtime, no logo" to SampleInstalled.copy(playtime = "", logoUrl = null),
    "update available · play disabled" to SampleInstalled.copy(updateVersion = "v4.13.0", canPlay = false),
    "special version with update · play enabled" to
        SampleInstalled.copy(specialType = "NUZLOCKE", updateVersion = "v4.13.0"),
    "help folder" to SampleInstalled.copy(showHelp = true),
    "updating · play, special version and folder disabled" to SampleInstalled.copy(
        updateVersion = "v4.13.0",
        canPlay = false,
        canUpdate = false,
        canSwitchSpecialVersion = false,
        canOpenFolder = false,
    ),
    "uninstalling" to SampleInstalled.copy(isUninstalling = true),
    "runtime not supported yet (today's Android)" to SampleInstalled.copy(showHelp = true, runtimeSupported = false),
)

private val NotificationVariants = listOf(
    Triple("Nueva versión disponible", "Pokémon Añil v4.13.0 ya se puede descargar.", NotificationType.INFO),
    Triple(
        "Mantenimiento programado",
        "El servidor de descargas estará en mantenimiento el domingo de 10:00 a 12:00. " +
            "Durante ese tiempo no se podrán descargar ni actualizar juegos.",
        NotificationType.WARNING,
    ),
    Triple("Descargas interrumpidas", "Estamos investigando un fallo en el CDN.", NotificationType.EXCLAMATION),
)

@Composable
fun ComponentCatalogScreen(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    var demoStatus by rememberSaveable { mutableStateOf(LibraryCardStatus.AVAILABLE) }
    var faqExpanded by rememberSaveable { mutableStateOf(false) }
    var lastLink by rememberSaveable { mutableStateOf("") }
    val openLink: (String) -> Unit = { lastLink = it }
    val faqs = faqsFor(language)
    val faq = faqs.first()
    val faqTerm = faq.question.split(' ').maxBy { it.length }.trim('¿', '?', '¡', '!')

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalLauncherColors.current.primaryBg),
        contentPadding = PaddingValues(vertical = LauncherSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(LauncherSpacing.MediumLarge),
    ) {
        item { AppearancePickers(theme, onThemeSelected, onLanguageSelected) }

        item { SectionHeader("LibraryGameCard · interactive (Download, Pause, Resume, Cancel)") }
        item {
            LibraryGameCard(
                state = SampleLibrary.copy(status = demoStatus),
                onDownload = { demoStatus = LibraryCardStatus.DOWNLOADING },
                onPause = { demoStatus = LibraryCardStatus.PAUSED },
                onCancel = { demoStatus = LibraryCardStatus.AVAILABLE },
                onUpdate = { demoStatus = LibraryCardStatus.DOWNLOADING },
            )
        }

        item { SectionHeader("LibraryGameCard · every state") }
        items(LibraryVariants) { (name, state) ->
            Sample(name) { LibraryGameCard(state, onDownload = {}, onPause = {}, onCancel = {}, onUpdate = {}) }
        }

        item { SectionHeader("InstalledGameCard · every state") }
        items(InstalledVariants) { (name, state) ->
            Sample(name) {
                InstalledGameCard(
                    state = state,
                    onPlay = {},
                    onUpdate = {},
                    onOpenHelp = {},
                    onSwitchSpecialVersion = {},
                    onOpenFolder = {},
                    onUninstall = {},
                )
            }
        }

        item { SectionHeader("GameCardSkeleton") }
        items(2) { GameCardSkeleton() }

        item { SectionHeader("NewsCard · tap a link, long-press it for its address") }
        item {
            NewsCard(
                tag = "Novedad",
                date = SampleDate,
                title = "Pokémon Añil v4.13.0",
                description = "Nueva ruta, gimnasio rediseñado y correcciones. Notas completas en " +
                    "lostiefangames.blogspot.com/p/pokemon-anil.html. Dudas en soporte@jagoba.dev.",
                onOpenLink = openLink,
            )
        }
        item {
            NewsCard(
                tag = "Evento",
                date = SampleDate.minusDays(40),
                title = "Torneo de la comunidad",
                description = "Sin enlaces: el texto se muestra tal cual.",
                onOpenLink = openLink,
            )
        }
        item { Caption("last link: ${lastLink.ifEmpty { "none" }}") }

        item { SectionHeader("NewsCardSkeleton") }
        items(2) { NewsCardSkeleton() }

        item { SectionHeader("NotificationCard · Info, Warning, Exclamation") }
        items(NotificationVariants) { (title, message, type) ->
            NotificationCard(title = title, message = message, type = type, date = SampleDate)
        }

        item { SectionHeader("NotificationCardSkeleton") }
        items(2) { NotificationCardSkeleton() }

        item { SectionHeader("FaqCard · interactive, collapsed and expanded") }
        item {
            FaqCard(
                question = faq.question,
                answer = faq.answer,
                highlight = "",
                expanded = faqExpanded,
                onToggle = { faqExpanded = !faqExpanded },
                onOpenLink = openLink,
            )
        }
        item { Caption("highlight: \"$faqTerm\"") }
        item {
            FaqCard(
                question = faq.question,
                answer = faq.answer,
                highlight = faqTerm,
                expanded = true,
                onToggle = {},
                onOpenLink = openLink,
            )
        }
        item {
            FaqCard(
                question = faqs.last().question,
                answer = "Consulta github.com/jagobainda/LostieLauncher para ver el código fuente.",
                highlight = "codigo",
                expanded = true,
                onToggle = {},
                onOpenLink = openLink,
            )
        }
    }
}

@Composable
private fun Sample(name: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LauncherSpacing.ExtraSmall)) {
        Caption(name)
        content()
    }
}
