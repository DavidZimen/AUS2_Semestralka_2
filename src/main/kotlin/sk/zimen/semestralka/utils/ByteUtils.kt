package sk.zimen.semestralka.utils

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.reflect.KClass

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

fun ByteArray.append(array: ByteArray, index: Int): Int {
    array.copyInto(this, index)
    return index + array.size
}

fun <T : Number>byteArrayToNumber(array: ByteArray, clazz: KClass<T>): Number {
    return 1
}

fun numberToByteArray(number: Number): ByteArray {
    var buffer: ByteBuffer = ByteBuffer.allocate(1)
    when (number) {
        is Int -> {
            buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            buffer.putInt(number)
        }
        is Long -> {
            buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
            buffer.putLong(number)
        }
        is Double -> {
            buffer = ByteBuffer.allocate(Double.SIZE_BYTES)
            buffer.putDouble(number)
        }
        is Float -> {
            buffer = ByteBuffer.allocate(Float.SIZE_BYTES)
            buffer.putFloat(number)
        }
        is Short -> {
            buffer = ByteBuffer.allocate(Short.SIZE_BYTES)
            buffer.putShort(number)
        }
    }
    return buffer.array()
}
