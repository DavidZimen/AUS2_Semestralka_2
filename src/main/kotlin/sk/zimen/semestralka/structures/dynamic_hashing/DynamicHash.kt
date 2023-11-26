package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.trie.Trie
import sk.zimen.semestralka.structures.trie.nodes.ExternalTrieNode
import sk.zimen.semestralka.structures.trie.nodes.InternalTrieNode
import sk.zimen.semestralka.utils.*
import java.io.RandomAccessFile
import java.util.*
import kotlin.reflect.KClass

class DynamicHash<K, T : IData<K>>(
    name: String,
    blockFactor: Int,
    overloadBlockFactor: Int,
    clazz: KClass<T>,
    hashTrieDepth: Int = 5
) : HashFile<K, T>(
    name,
    "main_file",
    blockFactor,
    clazz
) {

    /**
     * Trie to quickly find correct [Block] from [IData.hash] function.
     */
    private val hashTrie = Trie(0, blockSize.toLong(), hashTrieDepth)
    private val overloadFile = OverloadFile(name, overloadBlockFactor, clazz)

    /**
     * Inserts [item] to the correct block in [file].
     * [item] has to have a unique key.
     * @throws IllegalArgumentException when item with same key is already present in structure.
     */
    @Throws(IllegalArgumentException::class)
    fun insert(item: T) {
        var hashNode = hashTrie.getLeaf(item.hash())

        // find correct node from hash and load its block
        if (hashNode is InternalTrieNode) {
            if (hashNode.left == null) {
                hashNode = hashNode.createLeftSon(firstEmptyBlockAddress)
            } else if (hashNode.right == null) {
                hashNode = hashNode.createRightSon(firstEmptyBlockAddress)
            }
            getEmptyBlock()
        }

        // cast to external node, now it is possible
        var externalNode = hashNode as ExternalTrieNode

        // check if item with same key is already present
        if (loadBlock(externalNode.blockAddress).contains(item))
            throw IllegalArgumentException("Item is already present.")

        // if full, divide node into two in cycle
        while (externalNode.size == blockFactor && externalNode.canGoFurther(hashTrie.maxDepth)) {
            //load data from that block
            val dataList = loadBlock(externalNode.blockAddress).run {
                validElements = 0
                writeBlock()
                data
            }

            // divide external node
            val newParent = externalNode.divideNode(firstEmptyBlockAddress).also {
                getEmptyBlock()
            }

            // load left and right block of parent
            val right = newParent.right as ExternalTrieNode
            val left = newParent.left as ExternalTrieNode
            val rightBlock = loadBlock(right.blockAddress)
            val leftBlock = loadBlock(left.blockAddress)

            // reinsert data
            dataList.forEach {
                when (it.hash()[newParent.level]) {
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
            externalNode = when (item.hash()[newParent.level]) {
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

        // go to overload file
        if (!externalNode.canGoFurther(hashTrie.maxDepth)) {
            throw IllegalStateException("Overload file logic not implemented.")
            // TODO Here is if branch where it should go to Overload file, so far not implemented
        } else {
            loadBlock(externalNode.blockAddress)
                .also { it.insert(item) }
                .writeBlock()
            externalNode.increaseSize()
        }
    }

    /**
     * Finds item according to provided [key].
     */
    fun find(key: K) {
        TODO("Not yet implemented")
    }

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

    private fun Block<K, T>.contains(item: T): Boolean {
        var isPresent = false
        for (i in 0 until validElements) {
            isPresent = data[i] == item
        }

        //TODO logic for chaining blocks in overload file

        return isPresent
    }

    /**
     * Closes the files and saves metadata into separate text file.
     */
    fun save() {
        file.close()
        //TODO write metadata into separate yaml file
    }

    fun printStructure() {
        hashTrie.actionOnLeafs { address ->
            loadBlock(address).printBlock()
        }
        println("-------------------------------------------------------------------")
        println("File size: ${file.length()}")
        println("First empty block at: ${firstEmptyBlockAddress}")
    }
}
