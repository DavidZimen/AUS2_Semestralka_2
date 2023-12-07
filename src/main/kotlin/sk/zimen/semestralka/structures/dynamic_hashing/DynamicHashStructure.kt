package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.DynamicHashData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.trie.Trie
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.TrieNode
import sk.zimen.semestralka.utils.*
import java.io.RandomAccessFile
import java.util.*
import kotlin.reflect.KClass

/**
 * Class that represents Dynamic hash data structure.
 */
class DynamicHashStructure<K, T : DynamicHashData<K>>(
    name: String,
    blockFactor: Int,
    overloadBlockFactor: Int,
    clazz: KClass<T>,
    hashFunction: (K) -> BitSet,
    hashTrieDepth: Int = 5
) : HashStructure<K, T>(
    name,
    "main_file",
    blockFactor,
    2,
    clazz
) {
    /**
     * Number of elements in structure.
     */
    var size: Int = 0
        private set

    /**
     * Trie to quickly find correct [Block] from [hashFunction] function.
     */
    private val hashTrie = Trie(0, blockSize.toLong(), hashTrieDepth)

    /**
     * File for storing colliding [Block]s, when [Trie.maxDepth] level has been hit.
     */
    private val overloadStructure = OverloadHashStructure(name, overloadBlockFactor, clazz)

    /**
     * Function to be used together with key, to make [BitSet] from key of type [K].
     */
    @Suppress("CanBePrimaryConstructorProperty")
    private val hashFunction: (K) -> BitSet = hashFunction

    /**
     * Inserts [item] to the correct block in [file].
     * [item] has to have a unique key.
     * @throws IllegalArgumentException when item with same key is already present in structure.
     */
    @Throws(IllegalArgumentException::class)
    fun insert(item: T) {
        // find node in trie and divide if necessary
        val hashNode = (getTrieNode(hashFunction.invoke(item.key)) as ExternalTrieNode).divide(item)

        val block = loadBlock(hashNode.blockAddress)
        try {
            block.insert(item)
            hashNode.mainSize++
        } catch (e: BlockIsFullException) {
            if (!block.hasNext()) {
                block.next = overloadStructure.firstEmpty
            }
            overloadStructure.insert(block.next, hashNode, item)
        }
        size++
        block.writeBlock()
    }

    /**
     * Finds item according to provided [key].
     * @throws NoResultFoundException if no item was found.
     */
    @Throws(NoResultFoundException::class)
    fun find(key: K): T {
        val hashNode = getTrieNode(hashFunction.invoke(key), false)
        if (hashNode is InternalTrieNode)
            throw NoResultFoundException("No result for provided key: ${key.toString()}.")

        val block = loadBlock((hashNode as ExternalTrieNode).blockAddress)

        //find in main block
        var item = block.find(key)

        //try overload block if not found
        if (item == null && block.hasNext()) {
            item = overloadStructure.find(block.next, key)
        }

        return item ?: throw NoResultFoundException("No result for provided key: ${key.toString()}.")
    }

    /**
     * Replaces [oldItem] with [newItem].
     * @throws IllegalStateException when items have different keys.
     * @throws NoResultFoundException when [oldItem] was not in structure.
     */
    @Throws(IllegalStateException::class)
    fun replace(oldItem: T, newItem: T) {
        if (oldItem.key != newItem.key)
            throw IllegalStateException("Provided items have different keys.")

        val hashNode = getTrieNode(hashFunction.invoke(oldItem.key), false)
        if (hashNode is InternalTrieNode)
            throw NoResultFoundException("Item with key: ${oldItem.key.toString()} is not in structure.")

        val block = loadBlock((hashNode as ExternalTrieNode).blockAddress)
        try {
            block.replace(oldItem, newItem)
            block.writeBlock()
        } catch (e: NoResultFoundException) {
            if (!block.hasNext())
                throw e
            overloadStructure.replace(block.next, oldItem, newItem)
        }
    }

    /**
     * Deletes item for corresponding [key].
     * @throws NoSuchElementException when no element with such key exists.
     */
    @Throws(NoSuchElementException::class)
    fun delete(key: K) {
        var hashNode = getTrieNode(hashFunction.invoke(key), false)
        if (hashNode is InternalTrieNode)
            throw NoSuchElementException("Element with key ${key.toString()} does not exist.")

        hashNode = hashNode as ExternalTrieNode
        val block = loadBlock(hashNode.blockAddress)

        val deleteBlock = block.delete(key)
        val deleteOverload = if (!deleteBlock) {
            overloadStructure.delete(block.next, hashNode, key)
        } else {
            false
        }

        if (deleteBlock) {
            hashNode.mainSize--
        }

        if (!deleteBlock && !deleteOverload) {
            throw NoSuchElementException("Element with key ${key.toString()} does not exist.")
        }

        //merge overloading block into main block if possible
        if (hashNode.canMergeWithOverloads()) {
            overloadStructure.deleteChain(block.next).forEach {
                block.insert(it)
            }
            block.next = -1L
            hashNode.mainSize = hashNode.size
            hashNode.overloadsSize = 0
            hashNode.chainLength = 1
        }

        // merge tho brother blocks if possible
        if (!mergeBlocks(hashNode, block))
            block.writeBlock()
        size--
    }

    /**
     * Check whether block contains provided [item].
     */
    fun contains(item: T): Boolean {
        val hashNode = getTrieNode(hashFunction.invoke(item.key), false)
        if (hashNode is InternalTrieNode)
            return false

        val block = loadBlock((hashNode as ExternalTrieNode).blockAddress)
        val isPresent = block.contains(item)
        return if (isPresent) {
            true
        } else if (!block.hasNext()) {
            false
        } else {
            overloadStructure.contains(block.next, item)
        }
    }

    /**
     * Closes the files and saves metadata into separate text file.
     */
    override fun save() {
        super.save()
        overloadStructure.save()
    }

    /**
     * Prints structure to the console for purposes of checking,
     * whether everything works correctly.
     */
    fun printStructure() {
        println("\nDynamic hash structure: $dirName")
        println("File size: ${file.length()}")
        println("First empty block at: $firstEmpty")
        println("Size: $size")
        hashTrie.actionOnLeafs(true) { address ->
            val block = loadBlock(address)
            if (block.hasNext())
                block.printData(hashFunction)
        }
        println("-------------------------------------------------------------------\n")
    }

    // OVERRIDE FUNCTIONS
    override fun initFile(dirName: String, fileName: String) {
        val dir = "data/${dirName}"
        initializeDirectory(dir)
        file = RandomAccessFile("${dir}/${fileName}.bin", "rw")
        file.setLength(blockSize.toLong() * 2)
        firstEmpty = file.length()
        val block = Block(blockFactor, clazz)
        block.writeBlock()
        block.apply { address = blockSize.toLong() }.writeBlock()

        //TODO logic when file is not empty at the start
    }

    override fun isLastBlockOccupied(): Boolean {
        return super.isLastBlockOccupied() && overloadStructure.isLastBlockOccupied()
    }

    // PRIVATE FUNCTIONS
    /**
     * Traverses [Trie] and find node, which corresponds to [hash].
     * - [shouldDivide] is only used when inserting and node is internal.
     */
    private fun getTrieNode(hash: BitSet, shouldDivide: Boolean = true): TrieNode {
        var node = hashTrie.getLeaf(hash)

        // find correct node from hash and load its block
        if (shouldDivide && node is InternalTrieNode) {
            if (node.left == null) {
                node = node.createLeftSon(firstEmpty)
            } else if (node.right == null) {
                node = node.createRightSon(firstEmpty)
            }
            getEmptyBlock()
        }

        return node
    }

    /**
     * If possible on [node], it performs node merging to upper level
     * and writes results to the [file].
     * @return
     *  - true if blocks were merged and changes written to file
     *  - false otherwise
     */
    private fun mergeBlocks(node: ExternalTrieNode, block: Block<K, T>): Boolean {
        // merge tho brother blocks if possible
        var brotherNode = node.getBrother()
        if (brotherNode is InternalTrieNode?)
            return false

        brotherNode = brotherNode as ExternalTrieNode

        if (node.canMergeWithBrother(brotherNode)) {
            val mergedNode = hashTrie.mergeNodes(node, brotherNode)

            // initialize blocks
            val (sourceBlock, targetBlock) = if (mergedNode.blockAddress == node.blockAddress) {
                loadBlock(brotherNode.blockAddress) to block
            } else {
                block to loadBlock(mergedNode.blockAddress)
            }

            // move elements
            sourceBlock.getAllData().forEach {
                targetBlock.insert(it)
            }

            // write blocks
            sourceBlock.addToEmptyBlocks()
            targetBlock.writeBlock()

            return true
        }
        return false
    }

    // EXTENSION FUNCTIONS
    /**
     * Divides [ExternalTrieNode] into two and changes it to [InternalTrieNode].
     * - Node will be divided, when its [ExternalTrieNode.size] is equal to [blockFactor]
     *   and its [ExternalTrieNode.level] is less than [Trie.maxDepth].
     */
    private fun ExternalTrieNode.divide(item: T): ExternalTrieNode {
        var currentNode = this

        // if full, divide node into two in cycle
        while (currentNode.mainSize == blockFactor && currentNode.canGoFurther(hashTrie.maxDepth)) {
            //load data from that block
            val block = loadBlock(currentNode.blockAddress)
            val dataList = block.getAllData()
            block.validElements = 0

            // divide external node
            val emptyBlock = getEmptyBlock()
            val newParent = currentNode.divideNode(emptyBlock.address)

            // load left and right block of parent
            val right = newParent.right as ExternalTrieNode
            val left = newParent.left as ExternalTrieNode
            val (rightBlock, leftBlock) = if (block.address == right.blockAddress) {
                block to emptyBlock
            } else {
                emptyBlock to block
            }

            // reinsert data
            dataList.forEach {
                if (hashFunction.invoke(it.key)[newParent.level]) {
                    rightBlock.insert(it)
                    right.mainSize++
                } else {
                    leftBlock.insert(it)
                    left.mainSize++
                }
            }

            // get correct node where to insert item
            currentNode = if (hashFunction.invoke(item.key)[newParent.level]) {
                if (left.size == 0) {
                    leftBlock.addToEmptyBlocks()
                    newParent.left = null
                }
                right
            } else {
                if (right.size == 0) {
                    rightBlock.addToEmptyBlocks()
                    newParent.right = null
                }
                left
            }

            // write child block if child is not null
            newParent.left?.let { leftBlock.writeBlock() }
            newParent.right?.let { rightBlock.writeBlock() }
        }

        return currentNode
    }

    /**
     * Checks if data in overloading blocks, can be merged into node in main structure.
     */
    private fun ExternalTrieNode.canMergeWithOverloads(): Boolean{
        return size <= blockFactor && chainLength > 1
    }

    /**
     * Checks if two sibling nodes can be merged into one.
     */
    private fun ExternalTrieNode.canMergeWithBrother(brother: ExternalTrieNode?): Boolean {
        return brother != null && size + brother.size <= blockFactor
    }
}
