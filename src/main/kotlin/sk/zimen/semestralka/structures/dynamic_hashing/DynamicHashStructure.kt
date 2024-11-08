package sk.zimen.semestralka.structures.dynamic_hashing

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import sk.zimen.semestralka.exceptions.BlockIsFullException
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.dynamic_hashing.util.*
import sk.zimen.semestralka.structures.trie.Trie
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.TrieNode
import sk.zimen.semestralka.utils.*
import sk.zimen.semestralka.utils.file.existsFileInDirectory
import sk.zimen.semestralka.utils.file.initializeDirectory
import sk.zimen.semestralka.utils.file.loadFromCsv
import sk.zimen.semestralka.utils.file.writeToCsv
import java.io.RandomAccessFile
import java.util.*
import kotlin.reflect.KClass

/**
 * Class that represents Dynamic hash data structure.
 */
class DynamicHashStructure<K, T : HashData<K>>(
    name: String,
    blockFactor: Int,
    overloadBlockFactor: Int,
    clazz: KClass<T>,
    hashFunction: (K) -> BitSet,
    hashTrieDepth: Int = 5
) : HashStructure<K, T>(
    name,
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
    private var hashTrie = Trie(0, blockSize.toLong(), hashTrieDepth)

    /**
     * File for storing colliding [Block]s, when [Trie.maxDepth] level has been hit.
     */
    private val overloadStructure = OverloadHashStructure(name, overloadBlockFactor, clazz)

    /**
     * Function to be used together with key, to make [BitSet] from key of type [K].
     */
    private val hashFunction: (K) -> BitSet = hashFunction

    init {
        initialize()
    }

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
            block.writeBlock()
        } catch (e: BlockIsFullException) {
            if (!block.hasNext()) {
                block.next = overloadStructure.firstEmpty
                block.writeBlock()
            }
            if (hashNode.level < hashTrie.maxDepth)
                throw IllegalStateException("Dont insert into overload yet")
            overloadStructure.insert(block.next, hashNode, item)
        }
        size++
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
    fun edit(oldItem: T, newItem: T) {
        if (oldItem.key != newItem.key)
            throw IllegalStateException("Provided items have different keys.")

        val hashNode = getTrieNode(hashFunction.invoke(oldItem.key), false)
        if (hashNode is InternalTrieNode)
            throw NoResultFoundException("Item with key: ${oldItem.key.toString()} is not in structure.")

        val block = loadBlock((hashNode as ExternalTrieNode).blockAddress)
        try {
            block.edit(oldItem, newItem)
            block.writeBlock()
        } catch (e: NoResultFoundException) {
            if (!block.hasNext())
                throw e
            overloadStructure.edit(block.next, oldItem, newItem)
        }
    }

    /**
     * Deletes item for corresponding [key].
     * @throws NoSuchElementException when no element with such key exists.
     */
    @Throws(NoSuchElementException::class)
    fun delete(key: K): T {
        var hashNode = getTrieNode(hashFunction.invoke(key), false)
        if (hashNode is InternalTrieNode)
            throw NoSuchElementException("Element with key ${key.toString()} does not exist.")

        hashNode = hashNode as ExternalTrieNode
        val block = loadBlock(hashNode.blockAddress)

        val deleteBlock = block.delete(key)
        val deleteOverload = if (deleteBlock == null) {
            overloadStructure.delete(block.next, hashNode, key)
        } else {
            null
        }

        if (deleteBlock != null) {
            hashNode.mainSize--
        }

        if (deleteBlock == null && deleteOverload == null) {
            throw NoSuchElementException("Element with key ${key.toString()} does not exist.")
        }

        //merge overloading block into main block if possible
        mergeWithOverloads(hashNode, block)

        // merge tho brother blocks if possible, block is written to file in method
        mergeBlocks(hashNode, block)
        size--

        return deleteBlock ?: deleteOverload!!
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

    // OVERRIDE FUNCTIONS
    /**
     * Closes the files and saves metadata into separate text file.
     * Saves [hashTrie] into separate csv file.
     */
    override fun save() {
        super.save()
        val metaData = DynamicHashMetadata(blockFactor, firstEmpty, blockSize, size, hashTrie.maxDepth)
        writeToCsv("$ROOT_DIRECTORY/$dirName", MAIN_META_DATA, DynamicHashMetadata::class, listOf(metaData))
        overloadStructure.save()
        hashTrie.saveToFile(dirName)
    }

    override fun initialize() {
        val dir = "$ROOT_DIRECTORY/$dirName"
        if (existsFileInDirectory(dir, MAIN_META_DATA)) {
            val metadata = loadFromCsv(dir, MAIN_META_DATA, DynamicHashMetadata::class)[0]
            blockFactor = metadata.blockFactor
            blockSize = metadata.blockSize
            firstEmpty = metadata.firstEmptyBlock
            size = metadata.size
            hashTrie.maxDepth = metadata.trieDepth
            hashTrie.loadFromFile(dir)
            file = RandomAccessFile("$dir/$MAIN_FILE", "rw")
        } else {
            initializeDirectory(dir)
            file = RandomAccessFile("$dir/$MAIN_FILE", "rw")
            file.setLength(blockSize.toLong() * 2)
            firstEmpty = file.length()
            val block = Block(blockFactor, clazz)
            block.writeBlock()
            block.apply { address = blockSize.toLong() }.writeBlock()
        }
    }

    override fun reset(metaData: HashMetadata) {
        val meta = metaData as DynamicHashMetadata
        val block = Block(meta.blockFactor, clazz)
        blockFactor = meta.blockFactor
        blockSize = block.getSize()
        hashTrie = Trie(0, blockSize.toLong(), meta.trieDepth)
        file.setLength(2 * blockSize.toLong())
        firstEmpty = file.length()
        size = 0
        block.writeBlock()
        block.apply { address = blockSize.toLong() }.writeBlock()

        overloadStructure.reset(HashMetadata(meta.overloadBlockFactor, -1, -1))
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
     * Merges overload blocks from [overloadStructure] into main structure.
     * All blocks in [overloadStructure] will be added to chain of empty blocks
     * in that structure.
     */
    private fun mergeWithOverloads(node: ExternalTrieNode, block: Block<K, T>) {
        if (node.canMergeWithOverloads()) {
            overloadStructure.deleteChain(block.next).forEach {
                block.insert(it)
            }
            block.next = -1L
            node.mainSize = node.size
            node.overloadsSize = 0
            node.chainLength = 1
        } else if (node.mainSize == 0 && node.chainLength > 1) {
            val items = mutableListOf<T>()
            val newOverload = overloadStructure.moveElementsToMain(block.next, blockFactor, items)
            items.forEach {
                block.insert(it)
                node.mainSize++
                node.overloadsSize--
            }
            newOverload?.let {
                block.next = newOverload
                node.chainLength --
            }
        }
    }

    /**
     * If possible on [node], it performs node merging to upper level
     * and writes results to the [file].
     * @return Block with data to be written into file.
     */
    private fun mergeBlocks(node: ExternalTrieNode, block: Block<K, T>) {
        // merge tho brother blocks if possible
        var currentNode = node
        var currentBlock = block
        var brotherNode = currentNode.getBrother()

        while (currentNode.canMergeWithBrother(brotherNode)) {
            // return when nodes parent is root
            val mergedNode: ExternalTrieNode
            try {
                mergedNode = hashTrie.mergeNodes(currentNode, (brotherNode as ExternalTrieNode?))
            } catch (e: IllegalStateException) {
                break
            }

            // initialize blocks
            val (sourceBlock, targetBlock) = if (mergedNode.blockAddress == currentNode.blockAddress) {
                if (brotherNode == null) {
                    null to currentBlock
                } else {
                    loadBlock(brotherNode.blockAddress) to currentBlock
                }
            } else {
                currentBlock to loadBlock(mergedNode.blockAddress)
            }

            // move elements
            sourceBlock?.getAllData()?.forEach {
                targetBlock.insert(it)
            }

            // remove empty source block
            sourceBlock?.addToEmptyBlocks()

            currentNode = mergedNode
            currentBlock = targetBlock
            brotherNode = currentNode.getBrother()
        }

        if (hashTrie.removeEmptyNode(currentNode))
            currentBlock.addToEmptyBlocks()
        else
            currentBlock.writeBlock()
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
     * Checks if two sibling nodes can be merged into one.
     */
    private fun ExternalTrieNode.canMergeWithBrother(brother: TrieNode?): Boolean {
        if (brother == null)
            return true

        if (brother !is ExternalTrieNode?)
            return false

        return chainLength == 1
                && brother.chainLength == 1
                && overloadsSize == 0
                && brother.overloadsSize == 0
                && mainSize + brother.mainSize <= blockFactor
    }

    /**
     * Checks if data in overloading blocks, can be merged into node in main structure.
     */
    private fun ExternalTrieNode.canMergeWithOverloads(): Boolean{
        return size <= blockFactor && chainLength > 1
    }

    // TESTING METHODS
    override fun isLastBlockOccupied(): Boolean {
        val occupied = if (file.length() <= 2 * blockSize) true else super.isLastBlockOccupied()
        return occupied && overloadStructure.isLastBlockOccupied()
    }

    fun isStateInitial(): Boolean {
        val length = file.length()
        return hashTrie.root.left != null
                && hashTrie.root.right != null
                && hashTrie.root.left is ExternalTrieNode
                && hashTrie.root.right is ExternalTrieNode
                && (hashTrie.root.left as ExternalTrieNode).size == 0
                && (hashTrie.root.right as ExternalTrieNode).size == 0
                && file.length() == 2L * blockSize
    }

    fun sequentialPrint(): HashPrint {
        return runBlocking {
            val main = async { getSequentialString() }
            val overload = async { overloadStructure.getSequentialString() }
            HashPrint(main.await(), overload.await())
        }
    }
}
