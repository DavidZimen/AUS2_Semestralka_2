package sk.zimen.semestralka.utils

import java.io.RandomAccessFile
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path


fun initializeDirectory(path: String) {
    try {
        Files.createDirectories(Path.of(path))
    } catch (_: FileAlreadyExistsException) { }
}

fun RandomAccessFile.writeAtPosition(position: Long, bytesToWrite: ByteArray) {
    seek(position)
    write(bytesToWrite)
}

fun RandomAccessFile.readAtPosition(position: Long, bytesCount: Int): ByteArray {
    seek(position)
    val bytes = ByteArray(bytesCount)
    read(bytes)
    return bytes
}