package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData

/**
 * Class that represents one block inside file for dynamic hashing structure.
 * @author David Zimen
 */
class Block<T : IData>(blockFactor: Int) : IBlock {

    private val validElements = 0
    private val data: Array<T?>

    init {
        @Suppress("UNCHECKED_CAST")
        this.data = arrayOfNulls<Any?>(blockFactor) as Array<T?>
    }

    override fun getSize(): Int {
        TODO("Not yet implemented")
    }

    override fun getData(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun formData(bytes: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun createInstance(): IBlock = Block<T>(data.size)
}