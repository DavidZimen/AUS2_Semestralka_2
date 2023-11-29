package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
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
     */
    fun insert(address: Long, trieNode: ExternalTrieNode, item: T) {
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
                break
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

        if (newBlockInChain)
            trieNode.chainLength++
        trieNode.overloadsSize++
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
     * @return
     *  - true when item was delete
     *  - false when item was not deleted
     */
    fun delete(address: Long, trieNode: ExternalTrieNode, key: K): Boolean {
        // TODO implement logic
        return false
    }

    /**
     * @return All data in chain of overloading blocks,
     *  starting with [address].
     */
    fun getAllData(address: Long): List<T> {
        var block = loadBlock(address)
        val dataList = block.getAllData() as MutableList<T>

        while (block.hasNext()) {
            block = loadBlock(block.next)
            dataList.addAll(block.getAllData())
        }

        return dataList
    }

    /**
     * Deletes all blocks in chain and returns data from all blocks.
     */
    fun deleteChain(address: Long): List<T> {
        var block = loadBlock(address)
        val dataList = block.getAllData() as MutableList<T>
        var next = block.next

        while (next > -1L) {
            block = loadBlock(next)
            next = block.next
            dataList.addAll(block.getAllData())
            block.addToEmptyBlocks()
        }

        return dataList
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