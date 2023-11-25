package sk.zimen.semestralka.structures.trie

import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.TrieNode
import java.util.BitSet

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

//    fun getLeaf(hash: BitSet): TrieNode {
//        var node: TrieNode = root
//        var index = 0
//        var bit: Boolean
//        var canGoFurther = true
//
//        while (canGoFurther) {
//            bit = hash[index++]
//
//            if (node is InternalTrieNode) {
//                if (bit) {
//                    if (node.right != null) {
//                        node = node.right!!
//                        canGoFurther = node is InternalTrieNode
//                    } else {
//                        canGoFurther = false
//                    }
//                } else {
//                    if (node.left != null) {
//                        node = node.left!!
//                        canGoFurther = node is InternalTrieNode
//                    } else {
//                        canGoFurther = false
//                    }
//                }
//            }
//        }
//
//        return node
//    }
}