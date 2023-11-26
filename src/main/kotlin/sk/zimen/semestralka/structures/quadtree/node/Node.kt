package sk.zimen.semestralka.structures.quadtree.node

import sk.zimen.semestralka.structures.quadtree.exceptions.BoundaryException
import sk.zimen.semestralka.structures.quadtree.exceptions.MultipleResultsFoundException
import sk.zimen.semestralka.structures.quadtree.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.quadtree.exceptions.PositionException
import sk.zimen.semestralka.structures.quadtree.interfaces.NodeIterator
import sk.zimen.semestralka.structures.quadtree.metrics.SubTreeMetrics
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.Position
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData
import java.util.*

/**
 * Represents a base abstract class for node with data in QuadTree.
 * @author David Zimen
 */
abstract class Node<T : QuadTreeData> {

    val dataList: MutableList<T> = ArrayList()
    val level: Int
    var boundary: Boundary
    var topLeft: Node<T>? = null
    var bottomLeft: Node<T>? = null
    var topRight: Node<T>? = null
    var bottomRight: Node<T>? = null
    private var parent: Node<T>? = null

    constructor(level: Int, boundary: Boundary) {
        this.level = level
        this.boundary = boundary
    }

    constructor(level: Int, parent: Node<T>?, boundary: Boundary) {
        this.level = level
        this.boundary = boundary
        this.parent = parent
    }

    // ABSTRACT function and functional attributes
    /**
     * Returns boolean flag, if current node has no elements.
     */
    abstract val isEmpty: Boolean

    /**
     * @return Number of elements in node.
     */
    abstract val size: Int

    /**
     * Function to edit data in current node.
     * @param old Version of item before editing.
     * @param new Version to be saved.
     */
    abstract fun edit(old: T, new: T): T

    /**
     * Function to only add data the list in node.
     * @param item Provided data.
     */
    abstract fun simpleInsert(item: T, p: Position): Boolean

    /**
     * Iterator with every item in [Node].
     */
    abstract fun dataIterator(): MutableIterator<T>

    /**
     * Creates new [Node] with boundary corresponding to provided [Position].
     * @param p Position where to create [Boundary].
     */
    abstract fun createNewNode(p: Position): Node<T>

    /**
     * Used to remove data when there is only one item left in node.
     */
    protected abstract fun removeSingleItem(): T

    /**
     * @param maxDepth Maximum depth of QuadTree where node is located.
     * @return Information whether node can be further divided.
     */
    protected abstract fun canBeDivided(maxDepth: Int): Boolean

    /**
     * @return Number of items in node which can go into lower nodes if allowed.
     */
    protected abstract fun divisibleItems(): List<T>

    // Functions and functional attributes
    /**
     * Function to provide information, whether [Node] has is leaf -> has no children.
     */
    val isLeaf: Boolean
        get() = topLeft == null
                && bottomLeft == null
                && topRight == null
                && bottomRight == null

    /**
     * Returns boolean value, whether current node is root of the QuadTree
     */
    val isRoot: Boolean
        get() = parent == null

    /**
     * Method to insert data in [Node]
     * @param item Generic data to be inserted.
     */
    fun insert(item: T, maxDepth: Int): Boolean {
        val boundary: Boundary = item.getBoundary()

        // if no data and not divided yet or node level is maximum
        if ((isEmpty && isLeaf) || level == maxDepth) {
            return simpleInsert(item, this.getPosition(boundary))
        }

        // insert item on required position
        var position: Position = this.getPosition(boundary)
        var node: Node<T> = getOrCreateNodeOnPosition(position)
        val res = node.simpleInsert(item, node.getPosition(boundary))

        // check all data and rearrange
        val nodeIterator: NodeIterator<T> = this.iterator()
        if (this !== node) {
            nodeIterator.addToIteration(node)
        }

        // loop through children nodes
        while (nodeIterator.hasNext()) {
            node = nodeIterator.nextWithoutChildren()
            if (node.canBeDivided(maxDepth)) {
                val dataIterator: MutableIterator<T> = node.dataList.iterator()

                // for each data try to put it in correct position
                while (dataIterator.hasNext()) {
                    val listData: T = dataIterator.next()
                    position = node.getPosition(listData.getBoundary())

                    //if position is not current -> insert on position
                    if (position !== Position.CURRENT) {
                        val newPosNode: Node<T> = node.getOrCreateNodeOnPosition(position)
                        newPosNode.simpleInsert(listData, newPosNode.getPosition(listData))
                        dataIterator.remove()

                        // add to iteration, if new node of data can be divided
                        if (newPosNode.canBeDivided(maxDepth)) {
                            nodeIterator.addToIteration(newPosNode)
                        }
                    }
                }
            }
        }

        return res
    }

    /**
     * Method to find all items which intersects with given key.
     * @param boundary Provided are to do interval searching.
     */
    open fun find(boundary: Boundary): List<T> {
        val foundData: MutableList<T> = mutableListOf()

        // check child notes and their data
        for (node in findIterator(boundary)) {
            for (item in node.findDataIterator()) {
                if (item.getBoundary().intersects(boundary)) {
                    foundData.add(item)
                }
            }
        }
        return foundData
    }

    /**
     * Deletes provided item from [Node].
     * @param item Item to be deleted.
     * @throws NoResultFoundException When no items were deleted.
     * @throws MultipleResultsFoundException When there are more exactly same items.
     */
    @Throws(NoResultFoundException::class, MultipleResultsFoundException::class)
    fun delete(item: T) {
        var data: T
        val removedData: MutableList<T> = ArrayList(1)
        val dataIterator: MutableIterator<T> = dataIterator()

        // loop over all data in node
        while (dataIterator.hasNext()) {
            data = dataIterator.next()
            if (data == item) {
                removedData.add(data)
                dataIterator.remove()
            }
        }

        // check correctness
        if (removedData.isEmpty()) {
            throw NoResultFoundException("No items match provided item for deletion.")
        } else if (removedData.size > 1) {
            dataList.addAll(removedData)
            throw MultipleResultsFoundException("Two items are of the same specification")
        }

        // rearrange nodes if necessary
        val childCount: Int = childrenCount()
        val onlyChild: Node<T>? = oneNotNullChild()
        if (!isEmpty || isRoot || onlyChild == null) return
        if (childCount == 0 || childCount == 1 ) {
            var upstreamMergeNode = parent!!

            if (childCount == 0 || onlyChild.childrenCount() == 0) {
                if (onlyChild.isEmpty) {
                    onlyChild.removeNode()
                    removeNode()
                } else if (onlyChild.size == 1) {
                    val reinsertItem: T = onlyChild.removeSingleItem()
                    onlyChild.removeNode()
                    simpleInsert(reinsertItem, getPosition(reinsertItem))
                    upstreamMergeNode = this
                }
                upstreamMergeNode.mergeUpstream()
            }
        }
    }

    /**
     * Return a node on provided position.
     * Can't return a null value.
     * @param p Position where to find node.
     */
    fun getNodeOnPosition(p: Position): Node<T> = getNodeOnPositionOrNull(p)!!

    /**
     * Return a node on provided position.
     * Can return a null value.
     * @param p Position where to find node.
     */
    fun getNodeOnPositionOrNull(p: Position): Node<T>? {
        return when (p) {
            Position.CURRENT -> this
            Position.TOP_LEFT -> topLeft
            Position.BOTTOM_LEFT -> bottomLeft
            Position.TOP_RIGHT -> topRight
            Position.BOTTOM_RIGHT -> bottomRight
        }
    }

    /**
     * Finds existing node, that is the most suitable for provided data.
     * @param item Provided data
     * @return Reference to [Node]
     */
    fun findMostEligibleNode(item: T): Node<T> {
        val boundary: Boundary = item.getBoundary()
        return this.findMostEligibleNode(boundary)
    }

    /**
     * Finds existing node, that is the most suitable for provided boundary.
     * @param b Provided boundary.
     * @return Reference to [Node]
     */
    fun findMostEligibleNode(b: Boundary): Node<T> {
        var position: Position = this.getPosition(b)
        var existsNode: Boolean = existsOnPosition(position)
        var node: Node<T> = if (existsNode) getNodeOnPosition(position) else this

        while (existsNode) {
            position = node.getPosition(b)
            existsNode = node.existsOnPosition(position)
            node = if (existsNode) node.getNodeOnPosition(position) else node
        }
        return node
    }

    /**
     * Removes node from the tree.
     */
    fun removeNode() {
        parent?.let { parentNode ->
            when (this) {
                parentNode.topLeft -> parentNode.topLeft = null
                parentNode.bottomLeft -> parentNode.bottomLeft = null
                parentNode.topRight -> parentNode.topRight = null
                parentNode.bottomRight -> parentNode.bottomRight = null
            }
        }

        parent = null
        topLeft = null
        bottomLeft = null
        topRight = null
        bottomRight = null
    }

    /**
     * @return Number of not-null children for [Node].
     */
    fun childrenCount(): Int = listOf(topLeft, bottomLeft, topRight, bottomRight).count { it != null }

    /**
     * @param data Provided data.
     * @throws IllegalArgumentException When no data was provided.
     * @return Position where the data belongs in the context of current [Node].
     */
    @Throws(IllegalArgumentException::class)
    fun getPosition(data: T): Position = this.getPosition(data.getBoundary())

    /**
     * @param b Provided boundary.
     * @throws IllegalArgumentException When no boundary was provided.
     * @return Position where the boundary belongs in the context of current [Node].
     */
    private fun getPosition(b: Boundary): Position {
        return if (getBoundaryOnPosition(Position.TOP_LEFT).contains(b)) {
            Position.TOP_LEFT
        } else if (getBoundaryOnPosition(Position.BOTTOM_LEFT).contains(b)) {
            Position.BOTTOM_LEFT
        } else if (getBoundaryOnPosition(Position.TOP_RIGHT).contains(b)) {
            Position.TOP_RIGHT
        } else if (getBoundaryOnPosition(Position.BOTTOM_RIGHT).contains(b)) {
            Position.BOTTOM_RIGHT
        } else if (boundary.contains(b)) {
            Position.CURRENT
        } else {
            throw BoundaryException("Boundary can't fit into node.")
        }
    }

    private fun getBoundaryOnPosition(p: Position): Boundary {
        return when(p) {
            Position.TOP_LEFT -> topLeft?.boundary ?: Boundary.createBoundaryOnPosition(Position.TOP_LEFT, boundary)
            Position.BOTTOM_LEFT -> bottomLeft?.boundary ?: Boundary.createBoundaryOnPosition(Position.BOTTOM_LEFT, boundary)
            Position.TOP_RIGHT -> topRight?.boundary ?: Boundary.createBoundaryOnPosition(Position.TOP_RIGHT, boundary)
            Position.BOTTOM_RIGHT -> bottomRight?.boundary ?: Boundary.createBoundaryOnPosition(Position.BOTTOM_RIGHT, boundary)
            Position.CURRENT -> boundary
        }
    }

    /**
     * @param p Provided position where to get or create a new [Node].
     * @return Node on a position.
     * @throws PositionException When no position was provided.
     */
    @Throws(PositionException::class)
    protected fun getOrCreateNodeOnPosition(p: Position?): Node<T> {
        return when (p) {
            Position.CURRENT -> this
            Position.TOP_LEFT -> topLeft ?: createNewNode(p).also { topLeft = it }
            Position.BOTTOM_LEFT -> bottomLeft ?: createNewNode(p).also { bottomLeft = it }
            Position.TOP_RIGHT -> topRight ?: createNewNode(p).also { topRight = it }
            Position.BOTTOM_RIGHT -> bottomRight ?: createNewNode(p).also { bottomRight = it }
            else -> throw PositionException("No position provided.")
        }
    }

    /**
     * Checks whether [Node] exists on provided position.
     * When position is CURRENT, then return false to not enter into while loop.
     * Use only in findMostEligibleNode method !!!
     */
    private fun existsOnPosition(p: Position): Boolean {
        return when (p) {
            Position.TOP_LEFT -> topLeft != null
            Position.BOTTOM_LEFT -> bottomLeft != null
            Position.TOP_RIGHT -> topRight != null
            Position.BOTTOM_RIGHT -> bottomRight != null
            Position.CURRENT -> false
        }
    }

    /**
     * Returns child [Node], if there is only one node.
     * If there are more than 1 or 0, then returns null.
     */
    private fun oneNotNullChild(): Node<T>? {
        val count: Int = childrenCount()
        if (count == 0 || count > 1) return null

        return if (topLeft != null) {
            topLeft
        } else if (bottomLeft != null) {
            bottomLeft
        } else if (topRight != null) {
            topRight
        } else if (bottomRight != null) {
            bottomRight
        } else {
            null
        }
    }

    /**
     * Function to merge nodes as close to quadtree root as possible.
     */
    private fun mergeUpstream() {
        var node = this
        while (node.canBeMerged()) {
            val reinsertItem = node.removeSingleItem()
            val parent = node.parent!!
            node.removeNode()
            node = parent
            node.simpleInsert(reinsertItem, node.getPosition(reinsertItem))
        }
    }

    /**
     * @return True if current node can be merged with parent.
     */
    private fun canBeMerged(): Boolean {
        return !isRoot && childrenCount() == 0 && size == 1
                && parent?.childrenCount() == 1 && parent?.size == 0
    }

    //FUNCTIONS FOR METRICS

    /**
     * Calculates [SubTreeMetrics] for current node.
     */
    fun metrics(): SubTreeMetrics {
        var depth = 0
        var nodesCount = 0
        var dataCount = 0
        var divisibleItemCount = 0
        var potentialDepth = level
        var leftX = 0.0
        var rightX = 0.0
        var topY = 0.0
        var bottomY = 0.0

        val iterator = iterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            dataCount += node.size
            nodesCount++
            if (node.level > depth) depth = node.level
            if (depth > potentialDepth) potentialDepth = depth
            if (node.isLeaf) {
                val divisibleItems = node.divisibleItems()
                divisibleItemCount += divisibleItems.size
                divisibleItems.forEach {
                    val potential = node.getPotentialDepth(it)
                    if (potential > potentialDepth) potentialDepth = potential
                }
            }
            for (item in node.dataIterator()) {
                with(item.getBoundary()) {
                    if (topLeft[0] < leftX) leftX = topLeft[0]
                    if (topLeft[1] > topY) topY = topLeft[1]
                    if (bottomRight[0] > rightX) rightX = bottomRight[0]
                    if (bottomRight[1] < bottomY) bottomY = bottomRight[1]
                }
            }
        }

        return SubTreeMetrics(dataCount, divisibleItemCount, nodesCount, depth, leftX, rightX, topY, bottomY, potentialDepth)
    }

    /**
     * Calculates the depth that can be reached from current node.
     * Returns maximal level from search.
     * If needed for number of levels between current node and deepest
     * just calculate result - node.level
     */
    fun depth(): Int {
        var currentDepth = 0
        val it = iterator()
        while (it.hasNext()) {
            val node = it.next()
            if (node.level > currentDepth) currentDepth = node.level
        }
        return currentDepth
    }

    /**
     * For provided item it returns depth to which it can go.
     */
    private fun getPotentialDepth(item: T): Int {
        var depth = level
        val position = getPosition(item)
        val maxDepth = 200

        if (position == Position.CURRENT) return depth

        var node = createNewNode(position)
        while (depth < maxDepth && node.getPosition(item) != Position.CURRENT) {
            node = node.createNewNode(node.getPosition(item))
            depth = node.level
        }
        return depth
    }

    // ITERATOR CLASS AND FUNCTIONS
    /**
     * @return [QuadTreeNodeIterator] instance for current [Node].
     */
    fun iterator(): NodeIterator<T> {
        return QuadTreeNodeIterator()
    }

    /**
     * @return [FindIterator] instance for current [Node].
     */
    fun findIterator(b: Boundary): Iterator<Node<T>> {
        return FindIterator(b)
    }

    /**
     * Operator to traverse data for finding operation.
     */
    open fun findDataIterator(): Iterator<T> = dataIterator()

    /**
     * Iterator implementation for [Node].
     * Iterates in order TOP_LEFT -> BOTTOM_LEFT -> TOP_RIGHT -> BOTTOM_RIGHT.
     * @author David Zimen
     */
    private inner class QuadTreeNodeIterator : NodeIterator<T> {
        /**
         * Stack of the nodes for traversal.
         */
        private val stack: Stack<Node<T>> = Stack()

        init {
            stack.push(this@Node)
        }

        override operator fun hasNext(): Boolean = !stack.isEmpty()


        override operator fun next(): Node<T> {
            if (!hasNext())
                throw NoSuchElementException("No more items in NodeIterator.")
            val current: Node<T> = stack.pop()

            // Push all not null children into stack
            if (current.topLeft != null) {
                addToIteration(current.topLeft!!)
            }
            if (current.bottomLeft != null) {
                addToIteration(current.bottomLeft!!)
            }
            if (current.topRight != null) {
                addToIteration(current.topRight!!)
            }
            if (current.bottomRight != null) {
                addToIteration(current.bottomRight!!)
            }
            return current
        }

        override fun nextWithoutChildren(): Node<T> {
            if (!hasNext())
                throw NoSuchElementException("No more items in NodeIterator.")
            return stack.pop()
        }

        override fun addToIteration(quadtreeNode: Node<T>) {
            stack.push(quadtreeNode)
        }

        /**
         * @throws UnsupportedOperationException Operation not implemented !!!
         */
        @Deprecated("DON'T USE THIS METHOD")
        override fun remove() { }
    }

    /**
     * Special iterator for find operation, when only those children,
     * that intersects with initial provided [boundary] will be added for traversing.
     */
    private inner class FindIterator(b: Boundary) : Iterator<Node<T>> {

        private val stack = Stack<Node<T>>()
        private val boundary: Boundary

        init {
            stack.push(this@Node)
            boundary = b
        }

        override fun hasNext(): Boolean = !stack.isEmpty()

        override fun next(): Node<T> {
            if (!hasNext())
                throw NoSuchElementException("No more items in NodeIterator.")
            val current: Node<T> = stack.pop()

            // Push children that intersects with required boundary to stack
            if (current.topLeft?.boundary?.intersects(boundary) == true) {
                addToIteration(current.topLeft!!)
            }
            if (current.bottomLeft?.boundary?.intersects(boundary) == true) {
                addToIteration(current.bottomLeft!!)
            }
            if (current.topRight?.boundary?.intersects(boundary) == true) {
                addToIteration(current.topRight!!)
            }
            if (current.bottomRight?.boundary?.intersects(boundary) == true) {
                addToIteration(current.bottomRight!!)
            }
            return current
        }

        fun addToIteration(quadtreeNode: Node<T>) {
            stack.push(quadtreeNode)
        }
    }
}
