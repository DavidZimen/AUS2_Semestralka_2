package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.trie.Trie
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.utils.*
import java.io.RandomAccessFile
import java.util.*
import kotlin.reflect.KClass

/**
 * Class that represents Dynamic hash data structure.
 */
class DynamicHashStructure<K, T : IData<K>>(
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
        // check if item with same key is already present
        if (contains(item))
            throw IllegalArgumentException("Item is already present.")

        // find node in trie and divide if necessary
        val hashNode = getTrieNode(hashFunction.invoke(item.key)).divide(item)

        val block = loadBlock(hashNode.blockAddress)
        try {
            block.insert(item)
            hashNode.mainSize++
        } catch (e: BlockIsFullException) {
            if (!block.hasNext()) {
                block.next = overloadStructure.firstEmpty
            }
            if (overloadStructure.insert(block.next, item)) {
                hashNode.chainLength++
            }
            hashNode.overloadsSize++
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
        val block = loadBlock(hashNode.blockAddress)

        //find in main block
        var item = block.find(key)

        //try overload block if not found
        if (item == null && block.hasNext()) {
            item = overloadStructure.find(block.next, key)
        }

        return item ?: throw NoResultFoundException("No result for provided key: ${key.toString()}.")
    }

    /**
     * Deletes item for corresponding [key].
     * @throws NoSuchElementException when no element with such key exists.
     */
    @Throws(NoSuchElementException::class)
    fun delete(key: K) {
        val hashNode = getTrieNode(hashFunction.invoke(key), false)
        val block = loadBlock(hashNode.blockAddress)

        val deleteBlock = block.delete(key)
        val deleteOverload = if (!deleteBlock && block.hasNext()) {
            overloadStructure.delete(block.next, key)
        } else {
            false
        }

        when {
            deleteBlock -> hashNode.mainSize--
            deleteOverload -> hashNode.overloadsSize--
            else -> throw NoSuchElementException("Element with key ${key.toString()} does not exist.")
        }

        //merge overloading block into main block if possible
        if (hashNode.canMergeOverloads()) {
            overloadStructure.getAllData(block.next).forEach {
                block.insert(it)
            }
            hashNode.mainSize = hashNode.size
            hashNode.overloadsSize = 0
            hashNode.chainLength = 1
        }

        // merge tho brother blocks if possible
        val brotherNode = hashNode.getBrother() as ExternalTrieNode?
        if (brotherNode != null && hashNode.size + brotherNode.size <= blockFactor) {
            val mergedNode = hashTrie.mergeNodes(hashNode, brotherNode)

            // initialize blocks
            val (sourceBlock, targetBlock) = if (mergedNode.blockAddress == hashNode.blockAddress) {
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
        } else {
            block.writeBlock()
        }
    }

    /**
     * Check whether block contains provided [item].
     */
    fun contains(item: T): Boolean {
        val hashNode = getTrieNode(hashFunction.invoke(item.key), false)
        val block = loadBlock(hashNode.blockAddress)
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

    // PRIVATE FUNCTIONS
    /**
     * Traverses [Trie] and find node, which corresponds to [hash].
     * - [shouldDivide] is only used when inserting and node is internal.
     */
    private fun getTrieNode(hash: BitSet, shouldDivide: Boolean = true): ExternalTrieNode {
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

        return node as ExternalTrieNode
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
            val dataList = with(loadBlock(currentNode.blockAddress)) {
                validElements = 0
                writeBlock()
                data
            }

            // divide external node
            val newParent = currentNode.divideNode(firstEmpty).also {
                getEmptyBlock()
            }

            // load left and right block of parent
            val right = newParent.right as ExternalTrieNode
            val left = newParent.left as ExternalTrieNode
            val rightBlock = loadBlock(right.blockAddress)
            val leftBlock = loadBlock(left.blockAddress)

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
    private fun ExternalTrieNode.canMergeOverloads() = size <= blockFactor && chainLength > 1
}
