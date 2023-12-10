package sk.zimen.semestralka.api.types

import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData
import sk.zimen.semestralka.utils.Mapper
import sk.zimen.semestralka.utils.file.CsvExclude

open class QuadTreePlace() : QuadTreeData {

    var key: Long = Long.MIN_VALUE
    @CsvExclude
    lateinit var topLeft: GpsPosition
    @CsvExclude
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
}