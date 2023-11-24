package sk.zimen.semestralka.structures.dynamic_hashing

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block
import sk.zimen.semestralka.utils.readAtPosition
import java.io.RandomAccessFile
import java.util.*
import kotlin.reflect.KClass

class DynamicHash<T : IData>(private val fileName: String, val blockFactor: Int, clazz: KClass<T>) {

    private val file: RandomAccessFile = RandomAccessFile(fileName, "rw")
    private val blockSize: Int

    init {
        val block = Block(6, clazz)
        blockSize = block.getSize()
        file.setLength(blockSize.toLong())
        file.write(block.getData())
    }

    fun insert(item: T) {
        val block = file.readAtPosition(0, blockSize)
    }

    fun save() = file.close()
}

class TestItem() : IData {

    private var number: Int = 0
    private var desc: String = ""

    constructor(number: Int, desc: String) : this() {
        this.number = number
        this.desc = desc
    }

    override fun equals(other: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hash(): BitSet {
        TODO("Not yet implemented")
    }

    override fun getSize(): Int {
        return Int.SIZE_BYTES + STRING_LENGTH
    }

    override fun getData(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun formData(bytes: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun createInstance(): IBlock = TestItem()

    companion object {
        const val STRING_LENGTH = 20
    }
}
