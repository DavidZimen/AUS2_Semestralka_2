package sk.zimen.semestralka.structures.quadtree.types


/**
 * Interface to make inserted data into QuadTree convert to boundaries.
 */
interface QuadTreeData {
    fun getBoundary(): Boundary

    fun setBoundary(boundary: Boundary)
}
