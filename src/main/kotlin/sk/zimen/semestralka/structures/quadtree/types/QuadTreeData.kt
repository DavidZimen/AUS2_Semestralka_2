package sk.zimen.semestralka.structures.quadtree.types


/**
 * Interface to make inserted data into QuadTree convert to boundaries.
 */
abstract class QuadTreeData {
    abstract fun getBoundary(): Boundary

    abstract fun setBoundary(boundary: Boundary)
}
