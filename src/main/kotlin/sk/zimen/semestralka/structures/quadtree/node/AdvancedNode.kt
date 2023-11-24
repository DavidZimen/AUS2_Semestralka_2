package sk.zimen.semestralka.structures.quadtree.node

import org.apache.commons.collections4.iterators.IteratorChain
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.Position
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData

/**
 * Represents an advanced node with data in AdvancedQuadTree.
 * Provides better performance for insert and delete operations
 * @author David Zimen
 */
class AdvancedNode<T : QuadTreeData> : Node<T> {
    /**
     * Holds data, where it is not possible further insert into deeper nodes.
     */
    private val nonDivisibleData: MutableList<T> = ArrayList()

    constructor(level: Int, boundary: Boundary) : super(level, boundary)

    constructor(level: Int, parent: Node<T>?, boundary: Boundary) : super(level, parent, boundary)

    // Functions and functional attributes overrides
    override val isEmpty: Boolean
        get() = dataList.isEmpty() && nonDivisibleData.isEmpty()

    override val size: Int
        get() = dataList.size + nonDivisibleData.size

    override fun edit(old: T, new: T): T {
        val iterator = if (getPosition(new) == Position.CURRENT) {
            nonDivisibleData.listIterator()
        } else {
            dataList.listIterator()
        }

        for (item in iterator) {
            if (item == old) {
                iterator.set(new)
            }
        }
        return new
    }

    override fun simpleInsert(item: T, p: Position): Boolean {
        return if (p === Position.CURRENT) {
            nonDivisibleData.add(item)
        } else {
            dataList.add(item)
        }
    }

    override fun dataIterator(): MutableIterator<T> = IteratorChain(dataList.listIterator(), nonDivisibleData.listIterator())

    override fun removeSingleItem(): T {
        return if (dataList.isEmpty()) {
            nonDivisibleData.removeAt(0)
        } else {
            dataList.removeAt(0)
        }
    }

    override fun canBeDivided(maxDepth: Int): Boolean {
        return if (nonDivisibleData.isEmpty()) {
            dataList.isNotEmpty() && (dataList.size > 1 || !isLeaf) && level < maxDepth
        } else {
            dataList.isNotEmpty() && level < maxDepth
        }
    }

    override fun createNewNode(p: Position): Node<T> = AdvancedNode(level + 1, this, Boundary.createBoundaryOnPosition(p, boundary))

    override fun divisibleItems(): List<T> {
        return if (size > 1) {
            dataList
        } else {
            emptyList()
        }
    }

    override fun findDataIterator(): Iterator<T> {
        return if (!isLeaf) {
            nonDivisibleData.listIterator()
        } else {
            dataIterator()
        }
    }
}
