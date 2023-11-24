package sk.zimen.semestralka.structures.quadtree

import sk.zimen.semestralka.structures.quadtree.node.AdvancedNode
import sk.zimen.semestralka.structures.quadtree.node.Node
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData

/**
 * Class that specifically implements [QuadTree] with [AdvancedNode]
 * for underlying logic.
 * [T] Data type to be used in Quad Tree.
 *      Must implement [QuadTreeData] interface to work correctly.
*/
class AdvancedQuadTree<T : QuadTreeData> : QuadTree<T> {

    constructor()
            : super(5)

    constructor(maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double)
            : super(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)

    constructor(maxAllowedDepth: Int)
            : super(maxAllowedDepth)


    override fun createRoot(
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double
    ): Node<T> {
        return AdvancedNode(0, Boundary(doubleArrayOf(topLeftX, topLeftY), doubleArrayOf(bottomRightX, bottomRightY)))
    }
}
