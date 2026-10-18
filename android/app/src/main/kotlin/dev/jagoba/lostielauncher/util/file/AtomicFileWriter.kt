package dev.jagoba.lostielauncher.util.file

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AtomicFileWriter {
    fun writeText(file: File, content: String) {
        val directory = file.absoluteFile.parentFile ?: throw IOException("'$file' has no directory.")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Could not create '$directory'.")
        val temp = File(directory, "${file.name}.tmp")
        temp.writeText(content)
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
