package sk.zimen.semestralka.structures.trie.nodes

import sk.zimen.semestralka.structures.trie.enums.Binary

/**
 * Abstract node for the digital trie data structure.
 * @author David Zimen
 */
abstract class TrieNode(
    key: Binary? = null,
    parent: InternalTrieNode? = null,
    level: Int
) {
    val key: Binary?
        get() = if (parent == null) null else field

    val parent: InternalTrieNode?

    val level: Int

    /**
     * Represents route of 0 and 1 to get from root to leaf.
     */
    var route: String = ""

    init {
        this.key = key
        this.parent = parent
        this.level = level
    }

    fun isLeft() = parent?.left == this

    fun isRight() = parent?.right == this

    fun getBrother(): TrieNode? {
        return if (isLeft()) parent?.right else parent?.left
    }
}
