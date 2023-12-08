package sk.zimen.semestralka.utils.file

import java.io.RandomAccessFile

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