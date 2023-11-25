package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.byteArrayToNumber
import sk.zimen.semestralka.utils.numberToByteArray
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Class that represents one block inside file for dynamic hashing structure.
 * @author David Zimen
 */
class Block<T : IData>(
    blockFactor: Int,
    clazz: KClass<T>
) : IBlock {

    val blockFactor: Int
    var validElements = 0
    var overloadBlock = -1L
    var previousEmpty = -1L
    var nextEmpty = -1L
    val data: MutableList<T> = ArrayList(blockFactor)
    private val clazz: KClass<T>

    init {
        this.blockFactor = blockFactor
        this.clazz = clazz
    }

    fun insert(item: T) {
        if (data.size < blockFactor) {
            data.add(validElements++, item)
        }
    }

    fun makeEmpty() {
        validElements = 0
        overloadBlock = -1L
        previousEmpty = -1L
        nextEmpty = -1L
    }

    //OVERRIDE FUNCTIONS

    override fun getSize(): Int {
        Byte.SIZE_BYTES
        return 2 * Int.SIZE_BYTES +
                3 * Long.SIZE_BYTES +
                blockFactor * clazz.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())
        index = bytes.append(numberToByteArray(validElements), index)
        index = bytes.append(numberToByteArray(overloadBlock), index)
        index = bytes.append(numberToByteArray(nextEmpty), index)
        index = bytes.append(numberToByteArray(previousEmpty), index)

        for (i in 0 until validElements) {
            val element = data[i] ?: break
            element.getData().copyInto(bytes, index)
            index += element.getSize()
        }

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        with(byteArrayToNumber(bytes.copyOfRange(index, index + Int.SIZE_BYTES), index, Int::class)) {
            validElements = number as Int
            index = newIndex
        }
        with (byteArrayToNumber(bytes.copyOfRange(index, index + Long.SIZE_BYTES), index, Long::class)) {
            overloadBlock = number as Long
            index = newIndex
        }
        with (byteArrayToNumber(bytes.copyOfRange(index, index + Long.SIZE_BYTES), index, Long::class)) {
            nextEmpty = number as Long
            index = newIndex
        }
        with (byteArrayToNumber(bytes.copyOfRange(index, index + Long.SIZE_BYTES), index, Long::class)) {
            previousEmpty = number as Long
            index = newIndex
        }

        for (i in 0 until validElements) {
            val element = clazz.createInstance()
            val startIndex = index
            index += element.getSize()
            element.formData(bytes.copyOfRange(startIndex, index))
            data.add(i, element)
        }
    }

    override fun createInstance(): IBlock = Block(data.size, clazz)
}