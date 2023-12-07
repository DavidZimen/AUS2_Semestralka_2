package sk.zimen.semestralka.structures.quadtree

import kotlinx.coroutines.*
import sk.zimen.semestralka.structures.quadtree.metrics.*
import sk.zimen.semestralka.structures.quadtree.node.Node
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.structures.quadtree.types.Position
import sk.zimen.semestralka.structures.quadtree.types.QuadTreeData
import kotlin.math.abs

/**
 * Base fot data structure, which represents Quad Tree.
 * For more information visit [Quad Tree](https://en.wikipedia.org/wiki/Quadtree).
 * - [T] Data type to be used in Quad Tree.
 *      Must implement [QuadTreeData] interface to work correctly.
 */
class QuadTree<T : QuadTreeData> (
    maxDepth: Int,
    topLeftX: Double,
    topLeftY: Double,
    bottomRightX: Double,
    bottomRightY: Double,
) {
    var root: Node<T>
        protected set
    var maxAllowedDepth: Int
        protected set
    var size = 0
        protected set
    var health = 0.0
        get() = field * 100
        protected set

    init {
        this.maxAllowedDepth = maxDepth
        this.root = createRoot(topLeftX, topLeftY, bottomRightX, bottomRightY)
    }

    constructor(maxDepth: Int) : this(maxDepth, -180.0, 90.0, 180.0, -90.0)

    /**
     * Creates a root node specific for implementation of [QuadTree].
     * @param topLeftX Top x coordinate of tree boundary.
     * @param topLeftY Top y coordinate of tree boundary.
     * @param bottomRightX Bottom x coordinate of tree boundary.
     * @param bottomRightY Bottom y coordinate of tree boundary.
     */
    private fun createRoot(
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double
    ): Node<T> = Node(0, Boundary(doubleArrayOf(topLeftX, topLeftY), doubleArrayOf(bottomRightX, bottomRightY)))

    // Functions and functional attributes
    /**
     * Functional attribute to get current depth of quadtree.
     */
    val currentDepth: Int
        get() = runBlocking {
            val depthList = with(root) {
                listOf(
                    async { topLeft?.depth() ?: 0 },
                    async { topRight?.depth() ?: 0 },
                    async { bottomLeft?.depth() ?: 0 },
                    async { bottomRight?.depth() ?: 0 }
                )
            }
            depthList.maxOfOrNull { it.await() } ?: 0
        }

    /**
     * Functional attribute to find out if quadtree is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Inserts provided data to the correct position in the [QuadTree].
     * @param data Data to be inserted.
     */
    fun insert(data: T) {
        // check if it is in boundary of QuadTree
        val p: Position = root.getPosition(data)
        if (size == 0) {
            root.simpleInsert(data, p)
        } else {
            root.findMostEligibleNode(data).insert(data, maxAllowedDepth)
        }
        size++
    }

    /**
     * Find all data that where keys are intersecting with provided key
     * @param boundary Provided boundary for searching.
     */
    fun find(boundary: Boundary): List<T> = root.find(boundary)

    /**
     * @return List of all items in quadtree.
     */
    fun all(): List<T> {
        val nodeIterator = root.iterator()
        val allItems = ArrayList<T>(size)

        while (nodeIterator.hasNext()) {
            nodeIterator.next().dataIterator().forEach {
                allItems.add(it)
            }
        }

        return allItems
    }

    /**
     * Removes all nodes and items from quadtree.
     */
    fun clear() {
        val nodeIterator = root.iterator()

        while (nodeIterator.hasNext()) {
            nodeIterator.next().removeNode()
        }
        size = 0
    }

    /**
     * Removes item from [QuadTree].
     * @param item Data to be removed.
     */
    fun delete(item: T) {
        root.findMostEligibleNode(item).delete(item)
        size--
    }

    /**
     * Finds old item in quadtree.
     * - If old item has same key as new, just replace them.
     * - If keys are different, then delete old and insert new.
     * @return Reference to new version of item.
     */
    fun edit(old: T, new: T): T {
        val node = root.findMostEligibleNode(old)
        if (old.getBoundary() == new.getBoundary()) {
            node.edit(old, new)
        } else {
            node.delete(old)
            root.findMostEligibleNode(new).insert(new, maxAllowedDepth)
        }

        return new
    }

    /**
    * Check if [QuadTree] contains provided item.
    * @param item Provided item.
    */
    operator fun contains(item: T): Boolean {
        var dataIterator: Iterator<T>
        val nodeIterator: Iterator<Node<T>> = root.iterator()
        var node: Node<T>
        while (nodeIterator.hasNext()) {
            node = nodeIterator.next()
            dataIterator = node.dataIterator()
            while (dataIterator.hasNext()) {
                if (dataIterator.next() == item) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Optimizes QuadTree based on currently calculated metrics.
     */
    fun optimise() {
        val metrics = metrics()
        updateHealth(metrics)
        //if (health >= 0.85 * 100) return

        // initialize to current tree boundary
        var topLeftX: Double
        var topLeftY: Double
        var bottomRightX: Double
        var bottomRightY: Double
        var maxDepth = maxAllowedDepth
        with(root.boundary) {
            topLeftX = topLeft[0]
            topLeftY = topLeft[1]
            bottomRightX = bottomRight[0]
            bottomRightY = bottomRight[1]
        }

        with(metrics) {
            // x axis
            if (abs(dataBalanceFactorX) > MetricsConsts.BALANCE_FACTOR_MIN) {
                if (dataBalanceFactorX < 0) {
                    bottomRightX += abs(bottomRightX) * enlargingPercentage(bottomRightX, leftX)
                } else if (dataBalanceFactorX > 0) {
                    topLeftX -= abs(topLeftX) * enlargingPercentage(topLeftX, rightX)
                }
            }
            // y axis
            if (abs(dataBalanceFactorY) > MetricsConsts.BALANCE_FACTOR_MIN) {
                if (dataBalanceFactorY < 0) {
                    bottomRightY -= abs(bottomRightY) * enlargingPercentage(bottomRightY, topY)
                } else if (dataBalanceFactorY > 0) {
                    topLeftY += abs(topLeftY) * enlargingPercentage(topLeftY, bottomY)
                }
            }
            if (potentialDepth > depth) maxDepth = potentialDepth
        }
        changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)
        if (currentDepth < maxAllowedDepth) maxAllowedDepth = currentDepth
        updateHealth(metrics())
    }

    /**
     * Function, that constructs [QuadTreeMetrics] from
     * its children [SubTreeMetrics].
     */
    fun metrics(): QuadTreeMetrics {
        val keys = Position.entries
            .filter { it != Position.CURRENT }

        val metricsMap = runBlocking {
            keys.associateWith {
                async { root.getNodeOnPositionOrNull(it)?.metrics() ?: SubTreeMetrics()}
            }.mapValues { (_, result) ->
                result.await()
            }
        }

        return QuadTreeMetrics().apply {
            // general metrics
            with(metricsMap.values) {
                dataRoot = root.size
                divisibleDataSize = sumOf { it.divisibleDataSize }
                depth = maxOf { it.depth }
                potentialDepth = maxOf { it.potentialDepth }
                leftX = minOf { it.leftX }
                rightX = maxOf { it.rightX }
                topY = maxOf { it.topY }
                bottomY = minOf { it.bottomY }
            }

            // get metrics for each top, bottom, left and right
            with(metricsMap.filter { (key, _) -> key == Position.TOP_LEFT || key == Position.TOP_RIGHT } ) {
                nodesTop = values.sumOf { it.nodesCount }
                dataTop = values.sumOf { it.dataCount }
            }
            with(metricsMap.filter { (key, _) -> key == Position.BOTTOM_LEFT || key == Position.BOTTOM_RIGHT } ) {
                nodesBottom = values.sumOf { it.nodesCount }
                dataBottom = values.sumOf { it.dataCount }
            }
            with(metricsMap.filter { (key, _) -> key == Position.TOP_LEFT || key == Position.BOTTOM_LEFT } ) {
                nodesLeft = values.sumOf { it.nodesCount }
                dataLeft = values.sumOf { it.dataCount }
            }
            with(metricsMap.filter { (key, _) -> key == Position.TOP_RIGHT || key == Position.BOTTOM_RIGHT } ) {
                nodesRight = values.sumOf { it.nodesCount }
                dataRight = values.sumOf { it.dataCount }
            }
            balanceFactorX = balanceFactor(nodesLeft - nodesRight, nodesLeft + nodesRight)
            balanceFactorY = balanceFactor(nodesTop - nodesBottom, nodesTop + nodesBottom)
            dataBalanceFactorX = balanceFactor(dataLeft - dataRight, size)
            dataBalanceFactorY = balanceFactor(dataTop - dataBottom, size)
        }
    }

    /**
     * Calculates new value for QuadTree health based on
     * weight given to attributes of [metrics].
     * Result is value in interval <0, 1>
     */
    fun updateHealth(metrics: QuadTreeMetrics) {
        with(metrics) {
            health = MetricsConsts.WEIGHT_BALANCE_X * (1.0 - abs(dataBalanceFactorX)) +
                    MetricsConsts.WEIGHT_BALANCE_Y * (1.0 - abs(dataBalanceFactorY)) +
                    MetricsConsts.WEIGHT_DIVISIBLE_FACTOR * divisibleDataFactor(divisibleDataSize, size) +
                    MetricsConsts.WEIGHT_TREE_HEIGHT * (depth.toDouble() / maxAllowedDepth)
        }
    }

    /**
     * Function to change the current height of the [QuadTree].
     * Takes all data of the current tree and inserts it in new with provided height.
     * Deletes old Quadtree.
     */
    @Throws(IllegalArgumentException::class)
    fun changeHeight(newMaxDepth: Int) {
        if (newMaxDepth < 1)
            throw IllegalArgumentException("Height of quadtree can not be less than 1.")

        val iterator = root.iterator()
        var dataIterator: MutableIterator<T>
        var node: Node<T>

        val newRoot = with(root.boundary) {
            maxAllowedDepth = newMaxDepth
            createRoot(topLeft[0], topLeft[1], bottomRight[0], bottomRight[1])
        }

        while (iterator.hasNext()) {
            node = iterator.next()
            dataIterator = node.dataIterator()
            while (dataIterator.hasNext()) {
                newRoot.insert(dataIterator.next(), maxAllowedDepth)
            }
            node.removeNode()
        }
        root = newRoot
    }

    /**
     * Creates new root for quadtree.
     * USE ONLY when there is only one node.
     * Children nodes are not being deleted.
     */
    fun changeParameters(
        maxDepth: Int,
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double,
    ) {
        changeTreeBoundary(topLeftX, topLeftY, bottomRightX, bottomRightY)
        changeHeight(maxDepth)
    }

    /**
     * Changes the boundary on root node, which means the boundary of the whole quadtree.
     */
    private fun changeTreeBoundary(
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double,
    ) {
        this.root.boundary = Boundary(doubleArrayOf(topLeftX, topLeftY), doubleArrayOf(bottomRightX, bottomRightY))
    }
}
