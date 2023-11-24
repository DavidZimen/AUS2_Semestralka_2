package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.numberToByteArray
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Class that represents one block inside file for dynamic hashing structure.
 * @author David Zimen
 */
class Block<T : IData>(private var blockFactor: Int, private var clazz: KClass<T>) : IBlock {

    private var validElements = 0
    private var overloadBlock = -1L
    private val data: Array<T?> = arrayOfNulls<Any?>(blockFactor) as Array<T?>

    override fun getSize(): Int {
        Byte.SIZE_BYTES
        return 2 * Int.SIZE_BYTES +
                Long.SIZE_BYTES +
                blockFactor * clazz.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())
        index = bytes.append(numberToByteArray(validElements), index)
        index = bytes.append(numberToByteArray(overloadBlock), index)

        for (i in 0 until validElements) {
            val element = data[i] ?: break
            element.getData().copyInto(bytes, index)
            index += element.getSize()
        }

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        validElements = bytes[index++].toInt()
        overloadBlock = bytes[index++].toLong()

        for (i in 0 until validElements) {
            val element = clazz.createInstance()
            val startIndex = index
            index += element.getSize()
            element.formData(bytes.copyOfRange(startIndex, index))
        }
    }

    override fun createInstance(): IBlock = Block(data.size, clazz)
}