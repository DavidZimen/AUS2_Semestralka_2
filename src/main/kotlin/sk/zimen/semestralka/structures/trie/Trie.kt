package sk.zimen.semestralka.structures.trie

import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.TrieNode
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
    val maxDepth: Int

    /**
     * Root of the [Trie], which is represented by [InternalTrieNode].
     */
    private val root = InternalTrieNode(null, null, 0)

    init {
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
     */
    fun mergeNodes(node1: ExternalTrieNode, node2: ExternalTrieNode): ExternalTrieNode {
        if (node1.parent != node2.parent)
            throw IllegalArgumentException("Cannot merge two nodes, that don't have same parent.")

        val newParent = node1.parent?.parent
                ?: throw IllegalArgumentException("Cannot merge further. Nodes parent is root of the trie.")

        val parent = node1.parent
        val newAddress = if (node1.blockAddress < node2.blockAddress) node1.blockAddress else node2.blockAddress
        val newNode = ExternalTrieNode(parent.key!!, parent, newAddress, parent.level, parent.route).apply {
            mainSize = node1.size + node2.size
            chainLength = 1
        }

        if (parent.isLeft()) {
            newParent.left = newNode
        } else {
            newParent.right = newNode
        }

        return newNode
    }

    /**
     * Traverses [Trie] and performs provided [func] on each
     * [ExternalTrieNode].
     */
    fun actionOnLeafs(isPrintout: Boolean = false, func: (address: Long) -> Unit) {
        val stack = Stack<TrieNode>()
        stack.push(root)

        while (stack.isNotEmpty()) {
            val node = stack.pop()

            when (node is ExternalTrieNode) {
                true -> {
                    if (isPrintout)
                        node.printNode()
                    func.invoke(node.blockAddress)
                }
                false -> {
                    val internal = node as InternalTrieNode
                    internal.right?.let { stack.push(it) }
                    internal.left?.let { stack.push(it) }
                }
            }
        }
    }
}