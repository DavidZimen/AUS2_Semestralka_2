package sk.zimen.semestralka.structures.dynamic_hashing

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
 * Class that represents Dynamic hash data structure, where collision
 * are
 */
class DynamicHash<K, T : IData<K>>(
    name: String,
    blockFactor: Int,
    overloadBlockFactor: Int,
    clazz: KClass<T>,
    hashFunction: (key: K) -> BitSet,
    hashTrieDepth: Int = 5
) : HashFile<K, T>(
    name,
    "main_file",
    blockFactor,
    clazz
) {

    /**
     * Trie to quickly find correct [Block] from [hashFunction] function.
     */
    private val hashTrie = Trie(0, blockSize.toLong(), hashTrieDepth)

    /**
     * File for storing colliding [Block]s, when [Trie.maxDepth] level has been hit.
     */
    private val overloadFile = OverloadFile(name, overloadBlockFactor, clazz)

    /**
     * Function to be used together with key, to make [BitSet] from key of type [K].
     */
    private val hashFunction: (key: K) -> BitSet = hashFunction

    /**
     * Inserts [item] to the correct block in [file].
     * [item] has to have a unique key.
     * @throws IllegalArgumentException when item with same key is already present in structure.
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun insert(item: T) {
        // find node in trie and divide if necessary
        val hashNode = getHashNode(hashFunction.invoke(item.key)).divide(item)

        // check if item with same key is already present
        if (loadBlock(hashNode.blockAddress).contains(item))
            throw IllegalArgumentException("Item is already present.")

        // go to overload file
        if (!hashNode.canGoFurther(hashTrie.maxDepth)) {
            // TODO Here is if branch where it should go to Overload file, so far not implemented
            throw IllegalStateException("Overload file logic not implemented.")
        } else {
            loadBlock(hashNode.blockAddress)
                .also { it.insert(item) }
                .writeBlock()
            hashNode.increaseSize()
        }
    }

    /**
     * Finds item according to provided [key].
     * @throws NoResultFoundException if no item was found.
     */
    @Throws(NoResultFoundException::class)
    fun find(key: K): T {
        val hashNode = getHashNode(hashFunction.invoke(key), false)
        val block = loadBlock(hashNode.blockAddress)
        var item: T?

        //find in main block
        item = block.find(key)

        //try overload block if not found
        if (item == null && block.overloadBlock > -1) {
            // TODO search in overload blocks
            throw NoResultFoundException("Overloading block not yet implemented for searching.")
        }

        return item ?: throw NoResultFoundException("No result for provided key: ${key}.")
    }

    /**
     * Closes the files and saves metadata into separate text file.
     */
    fun save() {
        file.close()
        //TODO write metadata into separate yaml file
    }

    /**
     * Prints structure to the console for purposes of checking,
     * whether everything works correctly.
     */
    fun printStructure() {
        hashTrie.actionOnLeafs { address ->
            loadBlock(address).printBlock()
        }
        println("-------------------------------------------------------------------")
        println("File size: ${file.length()}")
        println("First empty block at: $firstEmptyBlockAddress")
    }

    // OVERRIDE FUNCTIONS
    override fun initFile(dirName: String, fileName: String) {
        val dir = "data/${dirName}"
        initializeDirectory(dir)
        file = RandomAccessFile("${dir}/${fileName}.bin", "rw")
        file.setLength(blockSize.toLong() * 2)
        firstEmptyBlockAddress = file.length()
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
    fun getHashNode(hash: BitSet, shouldDivide: Boolean = true): ExternalTrieNode {
        var hashNode = hashTrie.getLeaf(hash)

        // find correct node from hash and load its block
        if (shouldDivide && hashNode is InternalTrieNode) {
            if (hashNode.left == null) {
                hashNode = hashNode.createLeftSon(firstEmptyBlockAddress)
            } else if (hashNode.right == null) {
                hashNode = hashNode.createRightSon(firstEmptyBlockAddress)
            }
            getEmptyBlock()
        }

        return hashNode as ExternalTrieNode
    }

    // EXTENSION FUNCTIONS
    /**
     * Checks if block contains provided [item].
     */
    private fun Block<K, T>.contains(item: T): Boolean {
        var isPresent = false
        for (i in 0 until validElements) {
            isPresent = data[i] == item
        }

        //TODO logic for chaining blocks in overload file

        return isPresent
    }

    /**
     * Divides [ExternalTrieNode] into two and changes it to [InternalTrieNode].
     * - Node will be divided, when its [ExternalTrieNode.size] is equal to [blockFactor]
     *   and its [ExternalTrieNode.level] is less than [Trie.maxDepth].
     */
    private fun ExternalTrieNode.divide(item: T): ExternalTrieNode {
        var currentNode = this

        // if full, divide node into two in cycle
        while (currentNode.size == blockFactor && currentNode.canGoFurther(hashTrie.maxDepth)) {
            //load data from that block
            val dataList = loadBlock(currentNode.blockAddress).run {
                validElements = 0
                writeBlock()
                data
            }

            // divide external node
            val newParent = currentNode.divideNode(firstEmptyBlockAddress).also {
                getEmptyBlock()
            }

            // load left and right block of parent
            val right = newParent.right as ExternalTrieNode
            val left = newParent.left as ExternalTrieNode
            val rightBlock = loadBlock(right.blockAddress)
            val leftBlock = loadBlock(left.blockAddress)

            // reinsert data
            dataList.forEach {
                when (hashFunction.invoke(it.key)[newParent.level]) {
                    true -> {
                        rightBlock.insert(it)
                        right.increaseSize()
                    }
                    false -> {
                        leftBlock.insert(it)
                        left.increaseSize()
                    }
                }
            }

            // get correct node where to insert item
            currentNode = when (hashFunction.invoke(item.key)[newParent.level]) {
                true -> {
                    if (left.size == 0) {
                        leftBlock.addToEmptyBlocks()
                        newParent.left = null
                    }
                    right
                }
                false -> {
                    if (right.size == 0) {
                        rightBlock.addToEmptyBlocks()
                        newParent.right = null
                    }
                    left
                }
            }
            rightBlock.writeBlock()
            leftBlock.writeBlock()
        }

        return currentNode
    }
}
