package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.*
import java.util.*

class TestItem() : IData<Int> {

    var id: Int = 0
    var desc: String = ""

    constructor(number: Int, desc: String) : this() {
        this.id = number
        this.desc = desc
    }

    override fun key(): Int = id

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is TestItem && id == other.id && desc == other.desc
    }

    override fun hash(): BitSet = numberToBitSet(id % 4)

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

    override fun createInstance(): IBlock = TestItem()

    override fun printData() {
        println("Id: ${id}, Description: ${desc}")
    }

    companion object {
        const val STRING_LENGTH = 20
    }
}