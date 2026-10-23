package dev.jagoba.lostielauncher.util.log

import dev.jagoba.lostielauncher.model.LogOptions
import dev.jagoba.lostielauncher.service.storage.StorageLocations
import java.io.File
import java.time.Clock
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FileLogger @Inject constructor(
    private val locations: StorageLocations,
    private val options: LogOptions,
    private val clock: Clock,
) : Logger,
    LogMaintenance {
    private val lock = Any()
    private var directoryEnsured = false
    private var activeMonth: String? = null
    private var activeIndex = 0
    private var activeSize = 0L
    private var activePath: File? = null

    override fun debug(message: String) = add("DEBUG", message)

    override fun info(message: String) = add("INFO", message)

    override fun error(message: String, throwable: Throwable?) {
        val text = throwable?.let { "$message${System.lineSeparator()}${it.stackTraceToString().trimEnd()}" } ?: message
        add("ERROR", text)
    }

    override fun purgeExpired() {
        try {
            LogFiles.purgeExpired(locations.logsDirectory, clock.instant(), options.zoneId, options.retentionMonths)
        } catch (_: Exception) {
        }
    }

    private fun add(level: String, message: String) {
        try {
            val payload = formatLine(level, message) + System.lineSeparator()
            synchronized(lock) {
                ensureDirectory()
                val month = YearMonth.from(clock.instant().atZone(options.zoneId)).toString()
                val active = LogFiles.resolveActiveFile(
                    locations.logsDirectory,
                    month,
                    options.maxFileSizeBytes,
                    activePath,
                    activeMonth,
                    activeIndex,
                    activeSize,
                )
                activePath = active.path
                activeMonth = active.month
                activeIndex = active.index
                activeSize = active.size
                active.path.appendText(payload, Charsets.UTF_8)
                activeSize += payload.toByteArray(Charsets.UTF_8).size
            }
        } catch (_: Exception) {
        }
    }

    private fun formatLine(level: String, message: String): String {
        val timestamp = timestampFormat.format(clock.instant())
        return LogFiles.formatLine(timestamp, level, message)
    }

    private fun ensureDirectory() {
        if (directoryEnsured) return
        locations.logsDirectory.mkdirs()
        directoryEnsured = true
    }

    private val timestampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(options.zoneId)
}
