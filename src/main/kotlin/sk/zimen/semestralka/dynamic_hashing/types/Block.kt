package sk.zimen.semestralka.dynamic_hashing.types

import sk.zimen.semestralka.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.dynamic_hashing.interfaces.IData

/**
 * Class that represents one block inside file for dynamic hashing structure.
 * @author David Zimen
 */
class Block<T : IData>(blockFactor: Int) : IBlock {

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

    override fun <T> formData(bytes: ByteArray): T {
        TODO("Not yet implemented")
    }
}