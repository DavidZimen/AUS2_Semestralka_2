package sk.zimen.semestralka.utils

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.BitSet
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

fun numberToBitSet(number: Number): BitSet = BitSet.valueOf(numberToByteArray(number).reversedArray())

fun <T : Number> byteArrayToNumber(byteArray: ByteArray, index: Int, clazz: KClass<T>): ByteToNumber {
    when (clazz) {
        Int::class -> {
            return ByteToNumber(
                putBytesAndRewind(byteArray, Int.SIZE_BYTES).getInt(),
                index + Int.SIZE_BYTES
            )
        }
        Long::class -> {
            return ByteToNumber(
                putBytesAndRewind(byteArray, Long.SIZE_BYTES).getLong(),
                index + Long.SIZE_BYTES
            )
        }
        Double::class -> {
            return ByteToNumber(
                putBytesAndRewind(byteArray, Double.SIZE_BYTES).getDouble(),
                index + Double.SIZE_BYTES
            )
        }
        Float::class -> {
            return ByteToNumber(
                putBytesAndRewind(byteArray, Float.SIZE_BYTES).getFloat(),
                index + Float.SIZE_BYTES
            )
        }
        Short::class -> {
            return ByteToNumber(
                putBytesAndRewind(byteArray, Short.SIZE_BYTES).getShort(),
                index + Short.SIZE_BYTES
            )
        }
        else -> return ByteToNumber(-1, -1)
    }
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

fun putBytesAndRewind(byteArray: ByteArray, size: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(size)
    buffer.put(byteArray)
    buffer.rewind()
    return buffer
}

class ByteToNumber(
    val number: Number,
    val newIndex: Int
)
