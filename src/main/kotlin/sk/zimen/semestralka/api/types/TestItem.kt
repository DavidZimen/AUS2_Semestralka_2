package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IBlock
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.StringData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.byteArrayToNumber
import sk.zimen.semestralka.utils.numberToByteArray

class TestItem() : IData<Long>() {

    override var key: Long = Long.MIN_VALUE
    var desc: StringData = StringData()

    constructor(id: Long, desc: String): this() {
        this.key = id
        this.desc.value = desc
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is TestItem && key == other.key && desc == other.desc
    }

    override fun getSize(): Int {
        return Long.SIZE_BYTES + StringData.getSize(MAX_STRING_LENGTH)
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(numberToByteArray(key), index)
        bytes.append(desc.getData(MAX_STRING_LENGTH), index)

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        with(byteArrayToNumber(bytes.copyOfRange(index, index + Long.SIZE_BYTES), index, Long::class)) {
            key = number as Long
            index = newIndex
        }
        desc.formData(bytes.copyOfRange(index, index + StringData.getSize(MAX_STRING_LENGTH)), MAX_STRING_LENGTH)
    }

    override fun createInstance(): IBlock = TestItem()

    override fun printData() = println("Id: ${key}, Description: ${desc.value}")


    companion object {
        const val MAX_STRING_LENGTH = 20
    }
}