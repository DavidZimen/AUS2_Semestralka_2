package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.generics.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.generics.IData
import sk.zimen.semestralka.utils.*
import java.util.*

class TestItem() : IData<Long>() {

    var number: Long = Long.MIN_VALUE
    var desc: StringData = StringData()

    constructor(number: Long, desc: String): this() {
        this.number = number
        this.desc.value = desc
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is TestItem && number == other.number && desc == other.desc
    }

    override fun getKeySize(): Int = Long.SIZE_BYTES

    override fun getSize(): Int {
        return Long.SIZE_BYTES + StringData.getSize(MAX_STRING_LENGTH)
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(number.toByteArray(), index)
        bytes.append(desc.getData(MAX_STRING_LENGTH), index)

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        bytes.copyOfRange(index, Long.SIZE_BYTES).toNumber(index, Long::class).also {
            number = it.number as Long
            index = it.newIndex
        }
        desc.formData(bytes.copyOfRange(index, index + StringData.getSize(MAX_STRING_LENGTH)), MAX_STRING_LENGTH)
    }

    override fun createInstance(): IBlock<Long> = TestItem()

    override fun printData(hashFunc: (Long) -> BitSet) = println("Hash: ${hashFunc.invoke(number).toOwnString()}, Id: ${number}, Description: ${desc.value}")


    companion object {
        const val MAX_STRING_LENGTH = 20
    }
}