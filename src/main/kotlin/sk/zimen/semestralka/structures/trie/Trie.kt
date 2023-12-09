package sk.zimen.semestralka.structures.trie

import sk.zimen.semestralka.structures.dynamic_hashing.util.ROOT_DIRECTORY
import sk.zimen.semestralka.structures.dynamic_hashing.util.TRIE_FILE
import sk.zimen.semestralka.structures.trie.enums.Binary
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.TrieNode
import sk.zimen.semestralka.utils.file.existsFileInDirectory
import sk.zimen.semestralka.utils.file.loadFromCsv
import sk.zimen.semestralka.utils.file.writeToCsv
import java.util.*

/**
 * Class that represents Digital Trie data structure.
 * Initial state is root with left and right son.
 * @author David Zimen
 */
class Trie(
    leftBlockAddress: Long,
    rightBlockAddress: Long,
    maxDepth: Int
) {

    /**
     * Maximum allowed depth for the Trie.
     */
    var maxDepth: Int

    /**
     * Root of the [Trie], which is represented by [InternalTrieNode].
     */
    var root = InternalTrieNode(null, null, 0)
        private set

    init {
        if (maxDepth < 1)
            throw IllegalArgumentException("Max depth cannot be less than 1.")

        root.apply {
            createLeftSon(leftBlockAddress)
            createRightSon(rightBlockAddress)
        }
        this.maxDepth = maxDepth
    }

    /**
     * @return Node leaf node matches provided [hash].
     */
    fun getLeaf(hash: BitSet): TrieNode {
        var node: TrieNode = root
        var index = 0

        while (node is InternalTrieNode) {
            node = when (hash[index++]) {
                true -> node.right ?: break
                false -> node.left ?: break
            }
        }
        return node
    }

    /**
     * Merges two [ExternalTrieNode]s into one, on upper level.
     * @throws IllegalStateException when external nodes are on level 1.
     * @throws IllegalArgumentException when nodes have different parent
     */
    @Throws(IllegalArgumentException::class, IllegalStateException::class)
    fun mergeNodes(node: ExternalTrieNode, brother: ExternalTrieNode?): ExternalTrieNode {
        if (brother != null && node.parent != brother.parent)
            throw IllegalArgumentException("Cannot merge two nodes, that don't have same parent.")

        val newParent = node.parent?.parent
            ?: throw IllegalStateException("Cannot merge further. Nodes parent is root of the trie.")

        val parent = node.parent!!
        val newAddress = brother?.let { minOf(node.blockAddress, it.blockAddress) }
            ?: node.blockAddress

        val newNode = ExternalTrieNode(parent.key!!, newParent, newAddress, parent.level, parent.route).apply {
            if (brother == null) {
                mainSize = node.mainSize
                chainLength = node.chainLength
                overloadsSize = node.overloadsSize
            } else {
                mainSize = node.size + (brother.size)
            }
        }

        setNewSonToParent(parent, newNode)
        return newNode
    }

    /**
     * Removes empty node from [Trie] if node level is greater than 1.
     */
    fun removeEmptyNode(node: ExternalTrieNode): Boolean {
        return node.size == 0 && node.level > 1 && setNewSonToParent(node, null)
    }

    /**
     * Traverses [Trie] and performs provided [func] on each
     * [ExternalTrieNode].
     */
    fun actionOnLeafs(isPrintout: Boolean = false, func: (() -> ExternalTrieNode)? = null): List<ExternalTrieNode> {
        val stack = Stack<TrieNode>()
        val leafs = mutableListOf<ExternalTrieNode>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val node = stack.pop()

            when (node is ExternalTrieNode) {
                true -> {
                    if (isPrintout)
                        node.printNode()
                    func?.invoke()
                    leafs.add(node)
                }
                false -> {
                    val internal = node as InternalTrieNode
                    internal.right?.let { stack.push(it) }
                    internal.left?.let { stack.push(it) }
                }
            }
        }

        return leafs
    }

    fun saveToFile(directory: String) {
        writeToCsv("$ROOT_DIRECTORY/$directory", TRIE_FILE, ExternalTrieNode::class, actionOnLeafs())
    }

    @Throws(IllegalStateException::class)
    fun loadFromFile(directory: String) {
        if (!existsFileInDirectory(directory, TRIE_FILE))
            throw IllegalStateException("File does not exists in provided directory.")

        root = InternalTrieNode(null, null, 0)
        val leafs = loadFromCsv(directory, TRIE_FILE, ExternalTrieNode::class)
        leafs.forEach {
            var parent = root
            for (i in 0 until it.route.length) {
                if (parent.level == it.level - 1) {
                    if (it.route[i] == '0')
                        parent.left = it
                    else
                        parent.right = it
                    it.parent = parent
                    break
                }

                if (it.route[i] == '0') {
                    if (parent.left == null)
                        parent.left = InternalTrieNode(Binary.ZERO, parent, parent.level + 1, parent.route + "0")
                    parent = parent.left!! as InternalTrieNode
                }
                else {
                    if (parent.right == null)
                        parent.right = InternalTrieNode(Binary.ONE, parent, parent.level + 1, parent.route + "1")
                    parent = parent.right!! as InternalTrieNode
                }
            }
        }
    }

    // PRIVATE FUNCTIONS
    private fun setNewSonToParent(node: TrieNode, newSon: ExternalTrieNode?): Boolean {
        val parent = node.parent ?: return false

        if (node.isLeft()) {
            parent.left = newSon
        } else {
            parent.right = newSon
        }
        return true
    }
}