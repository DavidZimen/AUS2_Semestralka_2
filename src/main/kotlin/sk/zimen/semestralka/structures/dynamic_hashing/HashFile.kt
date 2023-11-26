package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.utils.readAtPosition
import sk.zimen.semestralka.utils.writeAtPosition
import java.io.RandomAccessFile
import kotlin.reflect.KClass

abstract class HashFile<K, T : IData<K>>(
    dirName: String,
    fileName: String,
    protected val blockFactor: Int,
    protected val clazz: KClass<T>
) {
    protected val blockSize: Int = Block(blockFactor, clazz).getSize()
    protected var firstEmptyBlockAddress: Long = 0L
    protected lateinit var file: RandomAccessFile

    init {
        @Suppress("LeakingThis")
        initFile(dirName, fileName)
    }

    /**
     * Initializes file based on every implementing class needs.
     */
    protected abstract fun initFile(dirName: String, fileName: String)

    /**
     * Gets block at the [firstEmptyBlockAddress].
     * Also updates chain of empty blocks in the [file].
     */
    @Throws(IllegalArgumentException::class)
    protected fun getEmptyBlock(): Block<K, T> {
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
    protected fun Block<K, T>.addToEmptyBlocks() {
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
     * Writes block to [file].
     */
    protected fun Block<K, T>.writeBlock() {
        file.writeAtPosition(address, getData())
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
}