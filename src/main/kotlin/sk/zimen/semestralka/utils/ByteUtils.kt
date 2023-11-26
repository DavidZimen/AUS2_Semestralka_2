package sk.zimen.semestralka.utils

import java.nio.ByteBuffer
import java.util.*
import kotlin.reflect.KClass

/**
 * Class, that returns number loaded from [ByteArray] and new value of index.
 */
class ByteToNumber(
    val number: Number,
    val newIndex: Int
)

fun ByteArray.append(array: ByteArray, index: Int): Int {
    array.copyInto(this, index)
    return index + array.size
}

fun <T : Number> ByteArray.toNumber(index: Int, clazz: KClass<T>): ByteToNumber {
    return when (clazz) {
        Int::class -> run { ByteToNumber(rewind(Int.SIZE_BYTES).getInt(), index + Int.SIZE_BYTES) }
        Long::class -> run { ByteToNumber(rewind(Long.SIZE_BYTES).getLong(), index + Long.SIZE_BYTES) }
        Double::class -> run { ByteToNumber(rewind(Double.SIZE_BYTES).getDouble(), index + Double.SIZE_BYTES) }
        Float::class -> run { ByteToNumber(rewind(Float.SIZE_BYTES).getFloat(), index + Float.SIZE_BYTES) }
        Short::class -> run { ByteToNumber(rewind(Short.SIZE_BYTES).getShort(), index + Short.SIZE_BYTES) }
        Byte::class -> run { ByteToNumber(rewind(Byte.SIZE_BYTES).get(), index + Byte.SIZE_BYTES) }
        else -> ByteToNumber(-1, -1)
    }
}

fun ByteArray.rewind(size: Int): ByteBuffer {
    val buffer = ByteBuffer.allocate(size)
    buffer.put(this)
    buffer.rewind()
    return buffer
}

fun Number.toBitSet(): BitSet = BitSet.valueOf(toByteArray().reversedArray())

fun Number.toByteArray(): ByteArray {
    return when (this) {
        is Int -> ByteBuffer.allocate(Int.SIZE_BYTES).putInt(this)
        is Long -> ByteBuffer.allocate(Long.SIZE_BYTES).putLong(this)
        is Double -> ByteBuffer.allocate(Double.SIZE_BYTES).putDouble(this)
        is Float -> ByteBuffer.allocate(Float.SIZE_BYTES).putFloat(this)
        is Short -> ByteBuffer.allocate(Short.SIZE_BYTES).putShort(this)
        is Byte -> ByteBuffer.allocate(Byte.SIZE_BYTES).put(this)
        else -> throw IllegalArgumentException("Unsupported numeric type")
    }.array()
}

/**
 * Converts [BitSet]
 */
fun BitSet.toOwnString(): String {
    var result = ""
    for (i in 0 until length()) {
        result += if (get(i)) '1' else '0'
    }
    return result
}

