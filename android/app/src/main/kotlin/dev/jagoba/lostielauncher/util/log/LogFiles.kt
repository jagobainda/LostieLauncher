package dev.jagoba.lostielauncher.util.log

import java.io.File
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

internal data class ActiveLogFile(val path: File, val month: String, val index: Int, val size: Long)

internal data class LogProbe(val index: Int, val size: Long)

internal object LogFiles {
    const val EXTENSION = ".log"

    private val monthFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT)

    fun buildFileName(month: String, index: Int): String =
        if (index <= 0) month + EXTENSION else "$month.$index$EXTENSION"

    fun tryParseIndex(fileName: String, month: String): Int? {
        if (fileName.equals(month + EXTENSION, ignoreCase = true)) return 0
        val prefix = "$month."
        if (!fileName.startsWith(prefix, ignoreCase = true) ||
            !fileName.endsWith(EXTENSION, ignoreCase = true)
        ) {
            return null
        }
        val middle = fileName.substring(prefix.length, fileName.length - EXTENSION.length)
        if (middle.any { it !in '0'..'9' }) return null
        val index = middle.toIntOrNull() ?: return null
        return if (index > 0) index else null
    }

    fun selectExpired(fileNames: Iterable<String>, now: Instant, zone: ZoneId, retentionMonths: Int): List<String> {
        val cutoff = YearMonth.from(now.atZone(zone)).minusMonths(retentionMonths.toLong()).atDay(1)
        return fileNames.filter { name ->
            val month = tryParseMonth(name) ?: return@filter false
            month.atDay(1).isBefore(cutoff)
        }
    }

    fun resolveActiveFile(
        directory: File,
        month: String,
        maxBytes: Long,
        activePath: File?,
        activeMonth: String?,
        activeIndex: Int,
        activeSize: Long,
    ): ActiveLogFile {
        if (activePath == null || activeMonth != month) {
            val probed = probeMonth(directory, month, maxBytes)
            return ActiveLogFile(File(directory, buildFileName(month, probed.index)), month, probed.index, probed.size)
        }
        if (activeSize >= maxBytes) {
            val rolled = activeIndex + 1
            return ActiveLogFile(File(directory, buildFileName(month, rolled)), month, rolled, 0)
        }
        return ActiveLogFile(activePath, month, activeIndex, activeSize)
    }

    fun probeMonth(directory: File, month: String, maxBytes: Long): LogProbe {
        var highest = -1
        for (file in enumerateLogFiles(directory)) {
            val index = tryParseIndex(file.name, month)
            if (index != null && index > highest) highest = index
        }
        if (highest < 0) return LogProbe(0, 0)
        val size = fileLength(File(directory, buildFileName(month, highest)))
        return if (size >= maxBytes) LogProbe(highest + 1, 0) else LogProbe(highest, size)
    }

    fun purgeExpired(directory: File, now: Instant, zone: ZoneId, retentionMonths: Int): Int {
        if (!directory.isDirectory) return 0
        val names = enumerateLogFiles(directory).map { it.name }
        var removed = 0
        for (name in selectExpired(names, now, zone, retentionMonths)) {
            try {
                if (File(directory, name).delete()) removed++
            } catch (_: Exception) {
            }
        }
        return removed
    }

    fun formatLine(timestamp: String, level: String, message: String): String = "$timestamp [$level] -> $message"

    private fun tryParseMonth(fileName: String): YearMonth? {
        val dot = fileName.indexOf('.')
        val prefix = if (dot < 0) fileName else fileName.substring(0, dot)
        return try {
            YearMonth.parse(prefix, monthFormat)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun enumerateLogFiles(directory: File): List<File> {
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles { _, name -> name.endsWith(EXTENSION, ignoreCase = true) }?.toList().orEmpty()
    }

    private fun fileLength(file: File): Long = try {
        if (file.isFile) file.length() else 0L
    } catch (_: Exception) {
        0L
    }
}
