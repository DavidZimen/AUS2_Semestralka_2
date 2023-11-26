package sk.zimen.semestralka.structures.trie.nodes

import sk.zimen.semestralka.structures.trie.enums.Binary

/**
 * External node, which represents leaf in Trie.
 * Holds information about its block.
 * @author David Zimen
 */
class ExternalTrieNode(
    key: Binary,
    parent: InternalTrieNode,
    blockAddress: Long,
    level: Int,
    route: String
) : TrieNode(key, parent, level) {

    /**
     * Address in file, where to find block.
     */
    val blockAddress: Long

    /**
     * How many blocks are chained on address of this block.
     * Initial value is 1, because external node already has a block.
     */
    var chainLength: Int = 1
        private set

    /**
     * Count of elements inside of whole chain for current block.
     */
    var size: Int = 0
        private set

    init {
        this.blockAddress = blockAddress
        this.route = route
    }

    fun isLeft() = parent?.left == this

    fun isRight() = parent?.right == this

    fun getBrother(): TrieNode? {
        return if (isLeft()) parent?.right else parent?.left
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

        // when null throw exception
        if (newParent == null)
            throw IllegalStateException("External node has to have a parent !!")

        // create new sons and return their parent
        newParent.createLeftSon(newAddress)
        newParent.createRightSon(blockAddress)
        return newParent
    }

    /**
     * Increases [size] by 1.
     * @return New size of blocks in chain.
     */
    fun increaseSize() = ++size

    /**
     * Decreases [size] by 1.
     * @return New size of blocks in chain.
     */
    fun decreaseSize() = --size

    /**
     * Increases [chainLength] by 1.
     * @return New length of chain of blocks.
     */
    fun increaseChainLength() = ++chainLength

    /**
     * Decreases [chainLength] by 1.
     * @return New length of chain of blocks.
     */
    fun decreaseChainLength() = --chainLength
}