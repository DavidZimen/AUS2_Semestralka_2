package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.utils.StringData
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.toByteArray
import sk.zimen.semestralka.utils.toNumber
import kotlin.reflect.full.createInstance

/**
 * Class with data representing Parcel from assignment.
 * @author David Zimen
 */
class Parcel() : QuadTreePlace() {

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

    override fun getSize(): Int {
        return super.getSize() +
                Short.SIZE_BYTES +
                MAX_ASSOCIATED_PROPERTIES * AssociatedPlace::class.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        println("Size of whole: ${getSize()}, size of super: ${super.getSize()}")
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(super.getData(), index)
        index = bytes.append(validAssociated.toByteArray(), index)
        index = bytes.append(description.getData(MAX_STRING_LENGTH), index)

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

        super.formData(bytes)
        index += super.getSize()

        bytes.copyOfRange(index, index + Short.SIZE_BYTES).toNumber(index, Short::class).also {
            validAssociated = it.number as Short
            index = it.newIndex
        }

        val stringSize = StringData.getSize(MAX_STRING_LENGTH)
        description.formData(bytes.copyOfRange(index, index + stringSize), MAX_STRING_LENGTH)
        index += stringSize

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
