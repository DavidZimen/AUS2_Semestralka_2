package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.HashData
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData
import sk.zimen.semestralka.utils.Mapper
import sk.zimen.semestralka.utils.append
import sk.zimen.semestralka.utils.toByteArray
import sk.zimen.semestralka.utils.toNumber
import kotlin.reflect.full.createInstance

open class QuadTreePlace() : QuadTreeData, HashData<Long> {

    override var key: Long = Long.MIN_VALUE
    lateinit var topLeft: GpsPosition
    lateinit var bottomRight: GpsPosition

    constructor(topLeft: GpsPosition, bottomRight: GpsPosition) : this() {
        this.topLeft = topLeft
        this.bottomRight = bottomRight
    }

    override fun getBoundary(): Boundary = Mapper.toBoundary(topLeft, bottomRight)

    override fun setBoundary(boundary: Boundary) {
        val positions = Mapper.toPositions(boundary)
        topLeft = positions.topLeft
        bottomRight = positions.bottomRight
    }

    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        return if (other is QuadTreePlace) {
            topLeft == other.topLeft
                    && bottomRight == other.bottomRight
                    && key == other.key
        } else false
    }

    override fun getSize(): Int {
        return Long.SIZE_BYTES +
                2 * GpsPosition::class.createInstance().getSize()
    }

    override fun getData(): ByteArray {
        var index = 0
        val bytes = ByteArray(this@QuadTreePlace.getSize())

        println("QuadTreePlace size: ${this@QuadTreePlace.getSize()}")
        index = bytes.append(key.toByteArray(), index)
        index = bytes.append(topLeft.getData(), index)
        bytes.append(bottomRight.getData(), index)

        return bytes
    }

    override fun formData(bytes: ByteArray) {
        var index = 0
        bytes.copyOfRange(index, Long.SIZE_BYTES).toNumber(index, Long::class).also {
            key = it.number as Long
            index = it.newIndex
        }

        topLeft = GpsPosition::class.createInstance()
        val posSize = topLeft.getSize()
        topLeft.formData(bytes.copyOfRange(index, index + posSize))
        index += posSize
        bottomRight = GpsPosition::class.createInstance()
        bottomRight.formData(bytes.copyOfRange(index, index + posSize))
    }
}