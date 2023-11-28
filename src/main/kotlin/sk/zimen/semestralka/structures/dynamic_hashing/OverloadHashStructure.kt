package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.initializeDirectory
import java.io.RandomAccessFile
import kotlin.reflect.KClass

/**
 * Implementation of overloading hash structure for [DynamicHashStructure].
 */
class OverloadHashStructure<K, T : IData<K>>(
    name: String,
    blockFactor: Int,
    clazz: KClass<T>
) : HashStructure<K, T>(
    name,
    "overload_file",
    blockFactor,
    0,
    clazz
) {

    /**
     * Inserts [item] to the chain of overloading blocks
     * starting with [address].
     * @return - true if chain length must have been increased
     * - false otherwise
     */
    fun insert(address: Long, item: T): Boolean {
        var newBlockInChain = false
        var block = if (address == firstEmpty) {
            newBlockInChain = true
            getEmptyBlock()
        } else {
            loadBlock(address)
        }

        while (true) {
            try {
                block.insert(item)
                block.writeBlock()
                return newBlockInChain
            } catch (e: BlockIsFullException) {
                block = if (block.hasNext()) {
                    loadBlock(block.next)
                } else {
                    newBlockInChain = true
                    block.next = firstEmpty
                    block.writeBlock()
                    getEmptyBlock()
                }
            }
        }
    }

    /**
     * Finds item for provided key in the chain of
     * overloading blocks.
     */
    fun find(address: Long, key: K): T? {
        var block = loadBlock(address)
        var item = block.find(key)

        while (item == null && block.hasNext()) {
            block = loadBlock(block.next)
            item = block.find(key)
        }

        return item
    }

    /**
     * Finds item for provided [key] in chain of
     * overload block starting with [address].
     * @return - true when item was deleted
     *      *  - false when item was not deleted
     */
    fun delete(address: Long, key: K): Boolean {
        // TODO implement logic
        return false
    }

    /**
     * Check whether block contains provided [item].
     */
    fun contains(address: Long, item: T): Boolean {
        var block = loadBlock(address)

        while (true) {
            if (block.contains(item))
                return true

            if (block.hasNext()) {
                block = loadBlock(block.next)
            } else {
                break
            }
        }

        return false
    }

    // OVERRIDE FUNCTIONS
    override fun initFile(dirName: String, fileName: String) {
        val dir = "data/${dirName}"
        initializeDirectory(dir)
        file = RandomAccessFile("${dir}/${fileName}.bin", "rw")
        file.setLength(0)
        firstEmpty = file.length()

        //TODO logic when file is not empty at the start
    }
}