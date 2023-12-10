package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.utils.toByteArray
import sk.zimen.semestralka.utils.toNumber

class AssociatedPlace() : HashData<Long> {

    override var key: Long = Long.MIN_VALUE

    constructor(key: Long,) : this() {
        this.key = key
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        return if (other is AssociatedPlace)
            key == other.key
        else false
    }

    override fun toString() = key.toString()

    override fun getSize(): Int {
        return Long.SIZE_BYTES
    }

    override fun getData() = key.toByteArray()

    override fun formData(bytes: ByteArray) {
        bytes.copyOfRange(0, Long.SIZE_BYTES).toNumber(0, Long::class).also {
            key = it.number as Long
        }
    }

}