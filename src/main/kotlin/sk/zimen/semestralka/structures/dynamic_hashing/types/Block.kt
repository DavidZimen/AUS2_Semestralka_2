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
class Block<K, T : IData<K>>(
    private val blockFactor: Int,
    private val clazz: KClass<T>
) : IBlock {

    var address = 0L
    var validElements = 0
    var overloadBlock = -1L
    var previousEmpty = -1L
    var nextEmpty = -1L
    val data: MutableList<T> = ArrayList(blockFactor)

    /**
     * Inserts [item] into [data] if list size is less than [blockFactor].
     */
    fun insert(item: T) {
        if (data.size < blockFactor) {
            data.add(validElements++, item)
        }
    }

    /**
     * Finds item, whose key matches provided [key].
     * If no item found, returns null.
     */
    fun find(key: K): T? {
        for (i in 0 until validElements) {
            if (data[i].key == key)
                return data[i]
        }
        return null
    }

    /**
     * Makes [Block] empty and ready to be written to file.
     */
    fun makeEmpty() {
        validElements = 0
        overloadBlock = -1L
        previousEmpty = -1L
        nextEmpty = -1L
    }

    /**
     * Returns boolean whether [Block] is empty or not.
     */
    fun isEmpty(): Boolean {
        return validElements == 0
                && overloadBlock == -1L
                && (previousEmpty > -1L || nextEmpty > -1L)
    }

    fun printBlock() {
        println("-------------------------------------------------------------------")
        println("Address: ${address}, Valid items: ${validElements}, Prev: ${previousEmpty}, Next: ${nextEmpty}")
        for (i in 0 until  validElements) {
            print("\t\t")
            data[i].printData()
        }
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
            val element = data[i]
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

    override fun createInstance(): IBlock = Block(blockFactor, clazz)
}