package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.dynamic_hashing.util.*
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.utils.file.existsFileInDirectory
import sk.zimen.semestralka.utils.file.initializeDirectory
import sk.zimen.semestralka.utils.file.loadFromCsv
import sk.zimen.semestralka.utils.file.writeToCsv
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
                    val newBlock = getEmptyBlock()
                    newBlock.previous = block.address
                    newBlock
                }
            }
        }

        if (newBlockInChain) {
            trieNode.chainLength++
        }
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
     * Find [oldItem] in the chain of blocks and replaces it with [newItem].
     */
    @Throws(NoResultFoundException::class)
    fun replace(address: Long, oldItem: T, newItem: T) {
        var block = loadBlock(address)

        while (true) {
            try {
                block.replace(oldItem, newItem)
                block.writeBlock()
                return
            } catch (e: NoResultFoundException) {
                if (!block.hasNext())
                    throw e
                block = loadBlock(block.next)
            }
        }
    }

    /**
     * Finds item for provided [key] in chain of
     * overload block starting with [address].
     * @return
     *  - true when item was delete
     *  - false when item was not deleted
     */
    fun delete(address: Long, trieNode: ExternalTrieNode, key: K): Boolean {
        if (address <= -1L)
            return false

        var deleted: Boolean
        var previousBlock: Block<K, T>? = null
        var block = loadBlock(address)

        while (true) {
            deleted = block.delete(key)
            if (deleted || !block.hasNext())
                break
            previousBlock = block
            block = loadBlock(block.next)
        }

        if (deleted) {
            mergeBlocks(previousBlock, block, trieNode)
            trieNode.overloadsSize--
        }

        return deleted
    }

    /**
     * Deletes all blocks in chain and returns data from all blocks.
     */
    fun deleteChain(address: Long): List<T> {
        var block: Block<K, T>
        val dataList: MutableList<T> = mutableListOf()
        var next = address

        while (next > -1L) {
            block = loadBlock(next)
            next = block.next
            dataList.addAll(block.getAllData())
            block.addToEmptyBlocks()
        }

        return dataList
    }

    /**
     * Moves [elementsCount] items from overload structure into main.
     * @return New address of first block in chain, if it was changed.
     */
    fun moveElementsToMain(address: Long, itemsCount: Int, items: MutableList<T>): Long? {
        val block = loadBlock(address)
        val validElements = block.validElements
        val endIndex = if (itemsCount < validElements) validElements - itemsCount else 0

        for (i in validElements - 1 downTo endIndex) {
            items.add(block.data.removeAt(i))
            block.validElements--
        }

        return if (block.isEmpty()) {
            val nextBlock = block.next
            block.addToEmptyBlocks()
            nextBlock
        } else {
            block.writeBlock()
            null
        }
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
    /**
     * Closes the files and saves metadata into separate text file.
     */
    override fun save() {
        super.save()
        val metadata = HashMetadata(blockFactor, firstEmpty, blockSize)
        writeToCsv("$ROOT_DIRECTORY/$dirName", OVERLOAD_META_DATA, HashMetadata::class, listOf(metadata))
    }

    override fun initialize() {
        val dir = "$ROOT_DIRECTORY/$dirName"

        if (existsFileInDirectory(dir, OVERLOAD_META_DATA)) {
            val metaData = loadFromCsv(dir, OVERLOAD_META_DATA, HashMetadata::class)[0]
            compareMetaData(metaData)
            firstEmpty = metaData.firstEmptyBlock
            file = RandomAccessFile("$dir/$OVERLOAD_FILE", "rw")
            println("Overload file length: ${file.length()}")
        } else {
            initializeDirectory(dir)
            file = RandomAccessFile("${dir}/$OVERLOAD_FILE", "rw")
            file.setLength(0)
            firstEmpty = file.length()
        }
    }

    //PRIVATE FUNCTIONS
    private fun mergeBlocks(previous: Block<K, T>?, block: Block<K, T>, trieNode: ExternalTrieNode) {
        if (previous == null) {
            block.writeBlock()
            return
        }

        if (previous.validElements + block.validElements <= blockFactor) {
            block.getAllData().forEach {
                previous.insert(it)
            }
            previous.writeBlock()
            block.addToEmptyBlocks()
            trieNode.chainLength--
        } else {
            block.writeBlock()
        }
    }
}