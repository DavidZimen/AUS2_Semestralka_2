package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.structures.dynamic_hashing.util.HashMetadata
import sk.zimen.semestralka.utils.file.readAtPosition
import sk.zimen.semestralka.utils.file.writeAtPosition
import java.io.RandomAccessFile
import kotlin.reflect.KClass

/**
 * Generic structure for hashing in files.
 */
abstract class HashStructure<K, T : HashData<K>>(
    val dirName: String,
    protected var blockFactor: Int,
    private val allowedEmptyBlocks: Int,
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
    protected var blockSize: Int = Block(blockFactor, clazz).getSize()

    /**
     * File where data are being written and read from.
     */
    protected lateinit var file: RandomAccessFile

    // ABSTRACT FUNCTIONS
    /**
     * Initializes file based on every implementing class needs.
     */
    protected abstract fun initialize()

    // PUBLIC FUNCTIONS
    /**
     * Closes [file], so the buffer for it is released.
     */
    open fun save() = file.close()

    /**
     * Resets structure according to [metaData] with loosing all data.
     */
    open fun reset(metaData: HashMetadata) {
        file.setLength(0)
        blockFactor = metaData.blockFactor
        firstEmpty = 0L
        blockSize = Block(blockFactor, clazz).getSize()
    }

    // PROTECTED FUNCTIONS
    /**
     * Compares loaded metadata to created instance in runtime.
     * @throws IllegalStateException when some parameters are not correct.
     */
    @Throws(IllegalStateException::class)
    protected open fun compareMetaData(metaData: HashMetadata) {
        if (metaData.blockSize != blockSize || metaData.blockFactor != blockFactor)
            throw IllegalStateException("Inserted item are different size than ones saved in file.")
    }

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
        val freeBlock = loadBlock(firstEmpty)

        // check if it is really empty and first
        if (freeBlock.validElements > 0)
            throw IllegalArgumentException("Block is not empty !!!")

        //adjust empty blocks in chain
        if (freeBlock.hasNext()) {
            freeBlock.removeFromChain()
            firstEmpty = freeBlock.next
            freeBlock.next = -1L
        } else if (!freeBlock.hasNext()) {
            firstEmpty = file.length()
        } else {
            throw IllegalArgumentException("Empty block next is less than -1 !!!")
        }

        return freeBlock
    }

    //PRIVATE FUNCTIONS
    /**
     * Functions which clears empty block from the end of file,
     * so last block will contain data.
     */
    private fun clearEmptyBlockFromEnd() {
        var newLength = file.length() - blockSize
        file.setLength(newLength)

        while (newLength > allowedEmptyBlocks * blockSize) {
            val block = loadBlock(newLength - blockSize)

            if (!block.emptyAtEnd()) {
                break
            }

            block.removeFromChain()
            newLength -= blockSize
            file.setLength(newLength)
        }

        if (firstEmpty > newLength) {
            firstEmpty = newLength
        }
    }

    //EXTENSION FUNCTIONS
    /**
     * Extension function to add [Block] into chain of empty blocks.
     */
    protected fun Block<K, T>.addToEmptyBlocks() {
        removeFromChain()
        validElements = 0

        if (emptyAtEnd()) {
            clearEmptyBlockFromEnd()
            return
        }

        if (firstEmpty != file.length()) {
            loadBlock(firstEmpty).apply { previous = this@addToEmptyBlocks.address }
                .writeBlock()
            this.apply { next = firstEmpty }
                .writeBlock()
        } else {
            writeBlock()
        }

        firstEmpty = address
    }

    /**
     * Writes block to [file].
     */
    protected fun Block<K, T>.writeBlock() {
        if (address == next || address == previous)
            throw IllegalStateException("Wrong addresses at block")
        file.writeAtPosition(address, getData())
    }

    /**
     * Removes block from chain, by changing previous blocks next address
     * and next blocks previous address.
     */
    private fun Block<K, T>.removeFromChain() {
        if (hasPrevious()) {
            loadBlock(previous)
                .apply { next = this@removeFromChain.next }
                .writeBlock()
        }

        if (hasNext()) {
            loadBlock(next)
                .apply { previous = this@removeFromChain.previous }
                .writeBlock()
        }
    }

    /**
     * Returns information if block is empty and at the end of file.
     */
    private fun Block<K, T>.emptyAtEnd(): Boolean {
        return validElements == 0
                && address + blockSize == file.length()
    }

    //TESTING FUNCTIONS
    /**
     * Function for testing purposes.
     * Return boolean whether last block in [file] has some items.
     */
    open fun isLastBlockOccupied(): Boolean {
        if (file.length() < blockSize)
            return true
        return loadBlock(file.length() - blockSize).validElements > 0
    }
}