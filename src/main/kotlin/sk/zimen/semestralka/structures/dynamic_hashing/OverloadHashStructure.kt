package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.initializeDirectory
import java.io.RandomAccessFile
import kotlin.reflect.KClass

class OverloadHashStructure<K, T : IData<K>>(
    name: String,
    blockFactor: Int,
    clazz: KClass<T>
) : HashStructure<K, T>(
    name,
    "overload_file",
    blockFactor,
    clazz
) {

    /**
     * Inserts [item] to the chain of overloading blocks
     * starting with [address].
     * @return - true if chain length must have been increased
     * - false otherwise
     */
    fun insert(address: Long, item: T): Boolean {
        // TODO implement logic
        // allocate new block if address is -1
        // allocate new block if all blocks in chain are full
        return false
    }

    // OVERRIDE FUNCTIONS
    override fun initFile(dirName: String, fileName: String) {
        val dir = "data/${dirName}"
        initializeDirectory(dir)
        file = RandomAccessFile("${dir}/${fileName}.bin", "rw")
        file.setLength(blockSize.toLong())
        firstEmptyBlockAddress = file.length()

        //TODO logic when file is not empty at the start
    }

    override fun contains(address: Long, item: T): Boolean {
        var block = loadBlock(address)

        while (true) {
            if (block.contains(item))
                return true

            if (block.hasNext()) {
                block = loadBlock(block.next)
            } else {
                break
            }
        }

        return false
    }
}