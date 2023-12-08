package sk.zimen.semestralka.structures.trie.nodes

import sk.zimen.semestralka.structures.trie.enums.Binary

/**
 * Internal node of the Trie, which only hols references to its left and right son.
 * @author David Zimen
 */
class InternalTrieNode(
    key: Binary?,
    parent: InternalTrieNode?,
    level: Int,
    route: String = ""
) : TrieNode(key, parent, level) {

    /**
     * Left son of [InternalTrieNode].
     * Its key value should be [Binary.ZERO]
     */
    var left: TrieNode? = null

    /**
     * Right son of [InternalTrieNode].
     * Its key value should be [Binary.ONE]
     */
    var right: TrieNode? = null

    init {
        this.route = route
    }

    fun hasSon() = right != null || left != null

    /**
     * Creates son to the left.
     */
    fun createLeftSon(blockAddress: Long): TrieNode {
        left = ExternalTrieNode(Binary.ZERO, this, blockAddress, level + 1, route + '0')
        return left!!
    }

    /**
     * Creates son to the right.
     */
    fun createRightSon(blockAddress: Long): TrieNode {
        right = ExternalTrieNode(Binary.ONE, this, blockAddress, level + 1, route + '1')
        return right!!
    }
}