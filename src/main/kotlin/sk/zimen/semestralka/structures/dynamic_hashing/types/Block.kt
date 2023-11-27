package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.toByteArray
import sk.zimen.semestralka.utils.toNumber
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Class that represents one block inside file for dynamic hashing structure.
 * @author David Zimen
 */
class Block<K, T : IData<K>>(
    private val blockFactor: Int,
    private val clazz: KClass<T>
) : IBlock<K> {

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
        index = bytes.append(validElements.toByteArray(), index)
        index = bytes.append(overloadBlock.toByteArray(), index)
        index = bytes.append(nextEmpty.toByteArray(), index)
        index = bytes.append(previousEmpty.toByteArray(), index)

        for (i in 0 until validElements) {
            val element = data[i]
            element.getData().copyInto(bytes, index)
            index += element.getSize()
        }

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        bytes.copyOfRange(index, index + Int.SIZE_BYTES).toNumber(index, Int::class).also {
            validElements = it.number as Int
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            overloadBlock = it.number as Long
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            nextEmpty = it.number as Long
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            previousEmpty = it.number as Long
            index = it.newIndex
        }

        for (i in 0 until validElements) {
            val element = clazz.createInstance()
            val startIndex = index
            index += element.getSize()
            element.formData(bytes.copyOfRange(startIndex, index))
            data.add(i, element)
        }
    }

    override fun printData(hashFunc: (K) -> BitSet) {
        println("Address: $address, Valid items: $validElements, Prev: $previousEmpty, Next: $nextEmpty")
        println("Data items:")
        for (i in 0 until  validElements) {
            print("\t")
            data[i].printData(hashFunc)
        }
    }

    override fun createInstance(): IBlock<K> = Block(blockFactor, clazz)
}