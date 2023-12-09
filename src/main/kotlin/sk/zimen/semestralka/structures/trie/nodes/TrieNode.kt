package sk.zimen.semestralka.structures.trie.nodes

import sk.zimen.semestralka.structures.trie.enums.Binary
import sk.zimen.semestralka.utils.file.CsvExclude

/**
 * Abstract node for the digital trie data structure.
 * @author David Zimen
 */
abstract class TrieNode() {

    @CsvExclude
    var key: Binary? = null
        get() = if (parent == null) null else field

    @CsvExclude
    var parent: InternalTrieNode? = null
        protected set

    var level: Int = 0

    /**
     * Represents route of 0 and 1 to get from root to leaf.
     */
    var route: String = ""

    constructor(key: Binary? = null, parent: InternalTrieNode? = null, level: Int) : this() {
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
