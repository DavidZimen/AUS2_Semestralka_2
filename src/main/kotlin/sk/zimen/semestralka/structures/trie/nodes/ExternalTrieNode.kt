package sk.zimen.semestralka.structures.trie.nodes

import sk.zimen.semestralka.structures.trie.enums.Binary
import sk.zimen.semestralka.utils.file.CsvExclude

/**
 * External node, which represents leaf in Trie.
 * Holds information about its block.
 * @author David Zimen
 */
class ExternalTrieNode() : TrieNode() {

    /**
     * Address in file, where to find block.
     */
    var blockAddress: Long = -1L

    /**
     * How many blocks are chained on address of this block.
     * Initial value is 1, because external node already has a block.
     */
    var chainLength: Int = 1

    /**
     * Number of elements in block from main structure.
     */
    var mainSize: Int = 0

    /**
     * Number of elements in overload chain.
     */
    var overloadsSize: Int = 0

    /**
     * Number of elements inside of whole chain for current block.
     */
    @CsvExclude
    val size: Int
        get() = mainSize + overloadsSize

    constructor(key: Binary, parent: InternalTrieNode, blockAddress: Long, level: Int, route: String) : this() {
        this.key = key
        this.parent = parent
        this.blockAddress = blockAddress
        this.level = level
        this.route = route
    }

    fun canGoFurther(maxDepth: Int) = level < maxDepth

    /**
     * Divides [ExternalTrieNode] into two of them
     * and returns their parents as [InternalTrieNode].
     */
    @Throws(IllegalStateException::class)
    fun divideNode(newAddress: Long): InternalTrieNode {
        var newParent: InternalTrieNode? = null
        if (isLeft()) {
            parent?.left = InternalTrieNode(key, parent, level, route)
            newParent = parent?.left as InternalTrieNode
        } else if (isRight()) {
            parent?.right = InternalTrieNode(key, parent, level, route)
            newParent = parent?.right as InternalTrieNode
        }
        parent = null

        // when null throw exception
        if (newParent == null)
            throw IllegalStateException("External node has to have a parent !!")

        // create new sons and return their parent
        newParent.createLeftSon(newAddress)
        newParent.createRightSon(blockAddress)
        return newParent
    }

    fun printNode() {
        println("-------------------------------------------------------------------")
        println("Hash route: $route, Items: $size, Chain length: $chainLength")
    }

    // OVERRIDE FUNCTIONS
    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true

        if (other !is ExternalTrieNode)
            return false

        return blockAddress == other.blockAddress
                && mainSize == other.mainSize
                && overloadsSize == other.overloadsSize
                && chainLength == other.chainLength
    }
}