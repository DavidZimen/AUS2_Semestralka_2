package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
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
class Block<K, T : HashData<K>>(
    private val blockFactor: Int,
    private val clazz: KClass<T>
) : HashBlock {

    var address = 0L
    var validElements = 0
    var next = -1L
    var previous = -1L
    val data: MutableList<T> = ArrayList(blockFactor)

    /**
     * Inserts [item] into [data] if list size is less than [blockFactor].
     * @throws IllegalArgumentException when block contains element with [item.key].
     * @throws BlockIsFullException When no more items can be inserted into block.
     */
    @Throws(BlockIsFullException::class, IllegalArgumentException::class)
    fun insert(item: T) {
        if (validElements < blockFactor) {
            if (contains(item))
                throw IllegalArgumentException("Item is already present in block.")

            data.add(validElements++, item)
        } else {
            throw BlockIsFullException("Current block is at its maximum capacity !!!")
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
     * Replaces [oldItem] with [newItem] in block.
     * @throws NoResultFoundException if [oldItem] is not present in block.
     */
    @Throws(NoResultFoundException::class)
    fun replace(oldItem: T, newItem: T) {
        for (i in 0 until validElements) {
            if (data[i] == oldItem) {
                data[i] = newItem
                return
            }
        }

        throw NoResultFoundException("Old item not present in current block.")
    }

    /**
     * Deletes item with provided [key].
     * @return
     *  - true when item was deleted
     *  - false when item was not deleted
     */
    fun delete(key: K): T? {
        for (i in 0 until validElements) {
            if (data[i].key == key) {
                val delItem = data[i]
                data[i] = data[validElements - 1]
                validElements--
                return delItem
            }
        }
        return null
    }

    /**
     * Check whether block contains provided [item].
     */
    fun contains(item: T): Boolean {
        return (0 until validElements).any { data[it] == item }
    }

    /**
     * Returns all valid elements is list of [data].
     */
    fun getAllData(): List<T> {
        val items = ArrayList<T>(validElements)
        for (i in 0 until validElements) {
            items.add(data[i])
        }
        return items
    }

    /**
     * Returns boolean whether [Block] is empty or not.
     */
    fun isEmpty(): Boolean {
        return validElements == 0
                && (previous > -1L || next > -1L)
    }

    /**
     * Return boolean flag whether blockchain continues after current block.
     */
    fun hasNext(): Boolean = next > -1L

    /**
     * Return boolean flag whether block has some predecessor.
     */
    fun hasPrevious(): Boolean = previous > -1L

    //OVERRIDE FUNCTIONS
    override fun getSize(): Int {
        return 2 * Int.SIZE_BYTES +
                3 * Long.SIZE_BYTES +
                blockFactor * clazz.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())
        index = bytes.append(validElements.toByteArray(), index)
        index = bytes.append(next.toByteArray(), index)
        index = bytes.append(previous.toByteArray(), index)

        for (i in 0 until validElements) {
            val element = data[i]
            element.getData().copyInto(bytes, index)
            index += element.getSize()
        }

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        bytes.copyOfRange(index, Int.SIZE_BYTES).toNumber(index, Int::class).also {
            validElements = it.number as Int
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            next = it.number as Long
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            previous = it.number as Long
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

    override fun toString(): String {
        var result = "Address: $address, Previous: $previous, Next: $next, Valid count: $validElements \n\tData:"
        for (i in 0 until validElements) {
            result += "\n\t\t${data[i]}"
        }
        return result
    }
}