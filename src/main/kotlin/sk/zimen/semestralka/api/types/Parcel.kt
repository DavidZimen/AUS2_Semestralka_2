package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.utils.StringData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.toByteArray
import sk.zimen.semestralka.utils.toNumber
import kotlin.reflect.full.createInstance

/**
 * Class with data representing Parcel from assignment.
 * @author David Zimen
 */
class Parcel() : QuadTreePlace(), HashData<Long> {

    var description: StringData = StringData()
    var validAssociated: Short = 0
    var propertiesForParcel: MutableList<AssociatedPlace> = mutableListOf()

    constructor(topLeft: GpsPosition, bottomRight: GpsPosition) : this() {
        this.topLeft = topLeft
        this.bottomRight = bottomRight
    }

    constructor(description: String?, topLeft: GpsPosition, bottomRight: GpsPosition) : this(topLeft, bottomRight) {
        if (description != null)
            this.description.value = description
    }

    fun clone(): Parcel {
        val parcel = Parcel()
        parcel.key = key
        parcel.description = StringData(description.value)
        parcel.validAssociated = validAssociated
        parcel.topLeft = topLeft.clone()
        parcel.bottomRight = bottomRight.clone()
        for (i in 0 until parcel.validAssociated) {
            parcel.propertiesForParcel.add(propertiesForParcel[i].clone())
        }
        return parcel
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
                && other is Parcel
                && description == other.description
                && validAssociated == other.validAssociated
    }

    override fun toString(): String {
        var result = "${super.toString()}, Desc: ${description.value}"
        for (i in 0 until validAssociated) {
            if (i == 0)
                result += ", Associated places: "
            result += "${propertiesForParcel[i]}, "
        }
        return result
    }

    override fun getSize(): Int {
        return Short.SIZE_BYTES +
                Long.SIZE_BYTES +
                2 * GpsPosition::class.createInstance().getSize() +
                MAX_ASSOCIATED_PROPERTIES * AssociatedPlace::class.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(validAssociated.toByteArray(), index)
        index = bytes.append(description.getData(MAX_STRING_LENGTH), index)
        index = bytes.append(key.toByteArray(), index)
        index = bytes.append(topLeft.getData(), index)
        index = bytes.append(bottomRight.getData(), index)

        val elementSize = AssociatedPlace::class.createInstance().getSize()
        for (i in 0 until validAssociated) {
            val element = propertiesForParcel[i]
            element.getData().copyInto(bytes, index)
            index += elementSize
        }

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0

        bytes.copyOfRange(index, Short.SIZE_BYTES).toNumber(index, Short::class).also {
            validAssociated = it.number as Short
            index = it.newIndex
        }

        val stringSize = StringData.getSize(MAX_STRING_LENGTH)
        description.formData(bytes.copyOfRange(index, index + stringSize), MAX_STRING_LENGTH)
        index += stringSize

        bytes.copyOfRange(index, index + Long.SIZE_BYTES).toNumber(index, Long::class).also {
            key = it.number as Long
            index = it.newIndex
        }

        topLeft = GpsPosition::class.createInstance()
        val posSize = topLeft.getSize()
        topLeft.formData(bytes.copyOfRange(index, index + posSize))
        index += posSize
        bottomRight = GpsPosition::class.createInstance()
        bottomRight.formData(bytes.copyOfRange(index, index + posSize))
        index += posSize

        for (i in 0 until validAssociated) {
            val element = AssociatedPlace::class.createInstance()
            val startIndex = index
            index += element.getSize()
            element.formData(bytes.copyOfRange(startIndex, index))
            propertiesForParcel.add(i, element)
        }
    }

    companion object {
        const val MAX_STRING_LENGTH = 11
        const val MAX_ASSOCIATED_PROPERTIES = 5
    }
}
