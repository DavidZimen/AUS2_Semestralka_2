package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.utils.readAtPosition
import sk.zimen.semestralka.utils.writeAtPosition
import java.io.RandomAccessFile
import kotlin.reflect.KClass

/**
 * Generic structure for hashing in files.
 */
abstract class HashStructure<K, T : IData<K>>(
    val dirName: String,
    fileName: String,
    protected val blockFactor: Int,
    protected val clazz: KClass<T>
) {
    /**
     * Address of a first block in chain of empty blocks.
     */
    var firstEmpty: Long = 0L
        protected set

    /**
     * Number of items, that can be inserted into one block.
     */
    protected val blockSize: Int = Block(blockFactor, clazz).getSize()

    /**
     * File where data are being written and read from.
     */
    protected lateinit var file: RandomAccessFile

    init {
        @Suppress("LeakingThis")
        initFile(dirName, fileName)
    }

    // ABSTRACT FUNCTIONS
    /**
     * Check whether block contains provided [item].
     */
    abstract fun contains(address: Long, item: T): Boolean

    /**
     * Initializes file based on every implementing class needs.
     */
    protected abstract fun initFile(dirName: String, fileName: String)

    // PUBLIC FUNCTIONS
    /**
     * Function for testing purposes.
     * Return boolean whether last block in [file] has some items.
     */
    fun isLastBlockOccupied(): Boolean {
        return loadBlock(file.length() - blockSize).validElements > 0
    }

    /**
     * Closes [file], so the buffer for it is released.
     */
    open fun save() = file.close()

    // PROTECTED FUNCTIONS
    /**
     * Load block from [file] from provided [address] with size of [blockSize].
     */
    protected fun loadBlock(address: Long): Block<K, T> {
        return Block(blockFactor, clazz).also {
            it.formData(file.readAtPosition(address, blockSize))
            it.address = address
        }
    }

    /**
     * Gets block at the [firstEmpty].
     * Also updates chain of empty blocks in the [file].
     */
    @Throws(IllegalArgumentException::class)
    protected fun getEmptyBlock(): Block<K, T> {
        // if at the end of file
        if (firstEmpty == file.length()) {
            file.setLength(file.length() + blockSize)
            val freeBlock = Block(blockFactor, clazz).also {
                it.address = firstEmpty
                firstEmpty = file.length()
            }
            freeBlock.writeBlock()
            return freeBlock
        }

        // read block from position in file
        val length = file.length()
        val freeBlock = loadBlock(firstEmpty)

        // check if it is really empty and first
        if (freeBlock.validElements > 0)
            throw IllegalArgumentException("Block is not empty !!!")
//        if (freeBlock.previous != -1L)
//            throw IllegalArgumentException("Block has some predecessor !!!")

        //adjust empty blocks in chain
        if (freeBlock.hasNext()) {
            loadBlock(freeBlock.next)
                .apply { previous = -1L }
                .writeBlock()
            firstEmpty = freeBlock.next
            freeBlock.next = -1L
        } else if (!freeBlock.hasNext()) {
            firstEmpty = file.length()
        } else {
            throw IllegalArgumentException("Empty block next is less than -1 !!!")
        }

        return freeBlock
    }

    //EXTENSION FUNCTIONS
    /**
     * Extension function to add [Block] into chain of empty blocks.
     */
    protected fun Block<K, T>.addToEmptyBlocks() {
        val length = file.length()
        if (address + blockSize == file.length()) {
            file.setLength(address)
            return
            // TODO code to clear all empty from the end of file
        }

        makeEmpty()
        if (firstEmpty != file.length()) {
            loadBlock(firstEmpty)
                .apply { previous = address }
                .writeBlock()
            this.apply { next = firstEmpty}
                .writeBlock()
        }
        firstEmpty = address
    }

    /**
     * Writes block to [file].
     */
    protected fun Block<K, T>.writeBlock() {
        file.writeAtPosition(address, getData())
    }
}