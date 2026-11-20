package dev.jagoba.lostielauncher.service.game

import dev.jagoba.lostielauncher.core.coroutines.DispatcherProvider
import dev.jagoba.lostielauncher.model.KirinOptions
import dev.jagoba.lostielauncher.util.file.AtomicFileWriter
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

@Singleton
internal class KirinGameLibrary @Inject constructor(
    private val options: KirinOptions,
    private val dispatchers: DispatcherProvider,
) {
    private val json = Json { prettyPrint = true }
    private val gamesGate = Mutex()

    suspend fun register(title: String, gameDirectory: File) = withContext(dispatchers.io) {
        gamesGate.withLock {
            val file = options.libraryFile
            val path = gameDirectory.absolutePath
            val entries = if (file.exists()) read(file) else mutableListOf()
            val index = entries.indexOfFirst { it.path == path }
            val existing = entries.getOrNull(index)?.jsonObject.orEmpty()
            val entry = JsonObject(
                existing + mapOf(
                    TITLE to JsonPrimitive(title),
                    PATH to JsonPrimitive(path),
                    EXECUTABLE_NAME to JsonPrimitive(options.executableName),
                    RUNTIME_MODE to JsonPrimitive(options.runtimeMode),
                ),
            )
            if (index >= 0) entries[index] = entry else entries.add(entry)
            write(file, entries)
        }
    }

    suspend fun unregister(gameDirectory: File) = withContext(dispatchers.io) {
        gamesGate.withLock {
            val file = options.libraryFile
            if (!file.exists()) return@withLock
            val path = gameDirectory.absolutePath
            val entries = read(file)
            if (entries.removeAll { it.path == path }) write(file, entries)
        }
    }

    private fun read(file: File): MutableList<JsonElement> =
        (json.parseToJsonElement(file.readText()) as? JsonArray)?.toMutableList()
            ?: throw IOException("Kirin library '$file' is not a JSON array.")

    private val JsonElement.path: String? get() = ((this as? JsonObject)?.get(PATH) as? JsonPrimitive)?.content

    private fun write(file: File, entries: List<JsonElement>) =
        AtomicFileWriter.writeText(file, json.encodeToString(JsonArray.serializer(), JsonArray(entries)))

    private companion object {
        const val TITLE = "title"
        const val PATH = "path"
        const val EXECUTABLE_NAME = "executableName"
        const val RUNTIME_MODE = "runtimeMode"
    }
}
