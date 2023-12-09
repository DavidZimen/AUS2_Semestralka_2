package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.utils.*
import java.util.*

/**
 * Class to hold data about GPS position on map.
 * @author David Zimen
 */
class GpsPosition() : HashData<Byte?> {

    var width: Double = 0.0
    var widthPosition: WidthPos = WidthPos.Z
    var height: Double = 0.0
    var heightPosition: HeightPos = HeightPos.S

    constructor(width: Double, widthPosition: WidthPos, height: Double, heightPosition: HeightPos): this() {
        this.width = width
        this.widthPosition = widthPosition
        this.height = height
        this.heightPosition = heightPosition
    }

    /**
     * Not necessary to implement because it will not be directly inserted into structure.
     */
    override var key: Byte? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other is GpsPosition) {
            DoubleUtils.isAEqualsToB(
                width,
                other.width
            ) && widthPosition == other.widthPosition && DoubleUtils.isAEqualsToB(
                height, other.height
            ) && heightPosition == other.heightPosition
        } else false
    }

    override fun getSize(): Int {
        return 2 * Double.SIZE_BYTES + 2 * Char.SIZE_BYTES
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize())

        index = bytes.append(width.toByteArray(), index)
        index = bytes.append(widthPosition.value.toByteArray(), index)
        index = bytes.append(height.toByteArray(), index)
        bytes.append(heightPosition.value.toByteArray(), index)

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        bytes.copyOfRange(index, Double.SIZE_BYTES).toNumber(index, Double::class).also {
            width = it.number as Double
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Char.SIZE_BYTES).toChar(index).also {
            widthPosition = WidthPos.getByVal(it.char)
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Double.SIZE_BYTES).toNumber(index, Double::class).also {
            height = it.number as Double
            index = it.newIndex
        }
        bytes.copyOfRange(index, index + Char.SIZE_BYTES).toChar(index).also {
            heightPosition = HeightPos.getByVal(it.char)
            index = it.newIndex
        }
    }

    override fun hashCode(): Int {
        var result = width.hashCode()
        result = 31 * result + widthPosition.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + heightPosition.hashCode()
        return result
    }
}

/**
 * Class that represent key of [Place], that will be stored in QuadTree.
 * @author David Zimen
 */
data class GpsPositions(val topLeft: GpsPosition, val bottomRight: GpsPosition) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other is GpsPositions) {
            topLeft == other.topLeft && bottomRight == other.bottomRight
        } else false
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + bottomRight.hashCode()
        return result
    }
}

/**
 * Enum with possibilities for width in [GpsPosition].
 */
enum class WidthPos(val value: Char) {
    Z('Z'),
    V('V');

    companion object {
        fun getByVal(value: Char): WidthPos {
            return if (value == 'Z') Z else V
        }
    }
}

/**
 * Enum with possibilities for height in [GpsPosition].
 */
enum class HeightPos(val value: Char) {
    S('S'),
    J('J');

    companion object {
        fun getByVal(value: Char): HeightPos {
            return if (value == 'S') S else J
        }
    }
}
