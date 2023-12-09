package sk.zimen.semestralka.utils.file

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path


fun initializeDirectory(path: String) {
    try {
        Files.createDirectories(Path.of(path))
    } catch (_: FileAlreadyExistsException) { }
}

fun deleteDirectory(path: String) {
    try {
        Files.walk(Path.of(path))
                .sorted(Comparator.reverseOrder())
                .map { it.toFile() }
                .forEach { it.delete() }
    } catch (_: NoSuchFileException) { }
}

fun existsFileInDirectory(directory: String, filename: String): Boolean {
    return Files.exists(Path.of(directory, filename))
}
