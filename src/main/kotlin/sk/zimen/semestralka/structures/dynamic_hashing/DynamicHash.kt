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

class DynamicHash<T : IData>(
    name: String,
    private val blockFactor: Int,
    private val clazz: KClass<T>,
    hashTrieDepth: Int = 5
) {

    val file: RandomAccessFile
    val blockSize: Int = Block(blockFactor, clazz).getSize()
    private var firstEmptyBlockAddress: Long = 0L
    private val hashTrie = Trie(0, blockSize.toLong(), hashTrieDepth)

    init {
        val directory = "data/${name}"
        initializeDirectory(directory)
        file = RandomAccessFile("${directory}/main_file.bin", "rw")
        file.setLength(blockSize.toLong() * 2)
        firstEmptyBlockAddress = file.length()
        val block = Block(blockFactor, clazz)
        block.writeBlock()
        block.apply { address = blockSize.toLong() }.writeBlock()
    }

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
            println("Item not inserted.")
            return
            // TODO Here is if branch where it should go to Overload file, so far not implemented
        } else {
            loadBlock(externalNode.blockAddress)
                .also { it.insert(item) }
                .writeBlock()
            externalNode.increaseSize()
        }
    }

    fun getBlock(blockIndex: Int) : Block<T> {
        val block = Block(blockFactor, clazz)
        block.formData(file.readAtPosition((blockIndex * blockSize).toLong(), blockSize))
        return block
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
    }

    /**
     * Gets block at the [firstEmptyBlockAddress].
     * Also updates chain of empty blocks in the [file].
     */
    @Throws(IllegalArgumentException::class)
    private fun getEmptyBlock(): Block<T> {
        // if at the end of file
        if (firstEmptyBlockAddress == file.length()) {
            file.setLength(file.length() + blockSize)
            val freeBlock = Block(blockFactor, clazz).also {
                it.address = firstEmptyBlockAddress
                firstEmptyBlockAddress = file.length()
            }
            freeBlock.writeBlock()
            return freeBlock
        }

        // read block from position in file
        val freeBlock = loadBlock(firstEmptyBlockAddress)

        // check if it is really empty and first
        if (freeBlock.validElements > 0) throw IllegalArgumentException("Block is not empty !!!")
        if (freeBlock.previousEmpty != -1L) throw IllegalArgumentException("Block has some predecessor !!!")

        //adjust empty blocks in chain
        if (freeBlock.nextEmpty > -1L) {
            loadBlock(freeBlock.nextEmpty)
                .apply { previousEmpty = -1L }
                .writeBlock()
            firstEmptyBlockAddress = freeBlock.nextEmpty
            freeBlock.nextEmpty = -1L
        } else if (freeBlock.nextEmpty == -1L) {
            firstEmptyBlockAddress = file.length()
        } else {
            throw IllegalArgumentException("Empty block next is less than -1 !!!")
        }

        return freeBlock
    }

    /**
     * Extension function to add [Block] into chain of empty blocks
     * on provided [address].
     */
    private fun <T : IData> Block<T>.addToEmptyBlocks() {
        if (address + blockSize == file.length()) {
            file.setLength(address)
            return
            // TODO code to clear all empty from the end of file
        }

        makeEmpty()
        if (firstEmptyBlockAddress != file.length()) {
            loadBlock(firstEmptyBlockAddress)
                .apply { previousEmpty = address }
                .writeBlock()
            apply { nextEmpty = firstEmptyBlockAddress}
                .writeBlock()
        }
        firstEmptyBlockAddress = address
    }

    /**
     * Load block from [file] from provided [address] with size of [blockSize].
     */
    private fun loadBlock(address: Long): Block<T> {
        return Block(blockFactor, clazz).also {
            it.formData(file.readAtPosition(address, blockSize))
            it.address = address
        }
    }

    /**
     * Writes block to [file].
     */
    private fun <T : IData> Block<T>.writeBlock() {
        file.writeAtPosition(address, getData())
    }
}

class TestItem() : IData {

    var id: Int = 0
    var desc: String = ""

    constructor(number: Int, desc: String) : this() {
        this.id = number
        this.desc = desc
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is TestItem && id == other.id && desc == other.desc
    }

    override fun hash(): BitSet = numberToBitSet(id % 3)

    override fun getSize(): Int {
        return Int.SIZE_BYTES + STRING_LENGTH
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(numberToByteArray(id), index)
        index = bytes.append(fillRemainingString(desc, STRING_LENGTH).toByteArray(), index)

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        with(byteArrayToNumber(bytes.copyOfRange(index, index + Int.SIZE_BYTES), index, Int::class)) {
            id = number as Int
            index = newIndex
        }
        desc = getValidString(String(bytes.copyOfRange(index, index + STRING_LENGTH)), STRING_LENGTH)
    }

    companion object {
        const val STRING_LENGTH = 20
    }
}
