package sk.zimen.semestralka.utils.generator

import sk.zimen.semestralka.api.types.Parcel
import sk.zimen.semestralka.api.types.Property
import sk.zimen.semestralka.api.types.QuadTreePlace
import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.structures.quadtree.types.Boundary
import sk.zimen.semestralka.utils.DoubleUtils
import sk.zimen.semestralka.utils.Mapper
import sk.zimen.semestralka.utils.StringData
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class Generator() {
    /**
     * Instance of [Random].
     */
    val random = Random()
    val seed: Long = random.nextLong()
    var leftX: Double = -180.0
    var topY: Double = 90.0
    var rightX: Double = 180.0
    var bottomY: Double = -90.0

    constructor(quadrantWidth: Double, quadrantHeight: Double) : this() {
        setCoordinates(-quadrantWidth, quadrantHeight, quadrantWidth, -quadrantHeight)
    }

    init {
        random.setSeed(seed)
    }

    fun generateTestItems(count: Int): MutableList<TestItem> {
        val items = ArrayList<TestItem>(count)
        val keys = HashSet<Long>(count)
        while (items.size < count) {
            val key = random.nextLong()
            if (keys.contains(key)) {
                continue
            }
            keys.add(key)
            items.add(TestItem(key, nextString(20)))
        }
        return items
    }

    fun <T : QuadTreePlace> generateItems(
            itemClass: KClass<T>,
            count: Int,
            boundary: Boundary? = null
    ): MutableList<QuadTreePlace> {
        setCoordinates(boundary)
        val items = ArrayList<QuadTreePlace>(count)
        val keys = HashSet<Long>(count)

        while (items.size < count) {
            val key = random.nextLong()
            if (keys.contains(key)) {
                continue
            }
            keys.add(key)
            val item = if (itemClass.isSubclassOf(Property::class)) {
                generateParcel()
            }

        }
        return items
    }

    fun generateBoundaries(count: Int, boundary: Boundary? = null): MutableList<Boundary> {
        setCoordinates(boundary)
        val boundaries = ArrayList<Boundary>(count)
        while(boundaries.size < count) {
            try {
                boundaries.add(nextBoundary(generateSize()))
            } catch (_: Exception) { }
        }
        return boundaries
    }

    /**
     * Generates operations from [GeneratedOperation] enum with a given ratio.
     * @param count Number of operations to be generated.
     * @param ratio Double array of size 4, where
     *              0 -> INSERT
     *              1 -> DELETE
     *              2 -> EDIT
     *              3 -> FIND
     */
    fun generateOperations(count: Int, ratio: IntArray): Stack<GeneratedOperation>? {
        if (count < 1 || ratio.size != 4) {
            return null
        }
        val probs = DoubleArray(4)
        val sum = ratio.sum()
        ratio.forEachIndexed { index, i ->
            probs[index] = i.toDouble() / sum
        }

        val operations = Stack<GeneratedOperation>()
        for (i in 0 until count) {
            val probability = random.nextDouble()
            if (probability < probs[0]) {
                operations.push(GeneratedOperation.INSERT)
            } else if (probability < probs[0] + probs[1]) {
                operations.push(GeneratedOperation.DELETE)
            } else if (probability < probs[0] + probs[1] + probs[2]) {
                operations.push(GeneratedOperation.REPLACE)
            } else {
                operations.push(GeneratedOperation.FIND)
            }
        }
        return operations
    }

    fun nextString(maxLength: Int): String {
        val length = random.nextInt(1, maxLength)
        return (1..length)
            .map { CHARSET[random.nextInt(CHARSET.length)] }
            .joinToString("")
    }

    private fun generateParcel(): Parcel {
        val instance = Parcel()
        val positions = Mapper.toPositions(nextBoundary(generateSize()))
        instance.topLeft = positions.topLeft
        instance.bottomRight = positions.bottomRight
        instance.description = StringData(nextString(Parcel.MAX_STRING_LENGTH))
        return instance
    }

    private fun generateProperty(): Property {
        val instance = Property()
        val positions = Mapper.toPositions(nextBoundary(generateSize()))
        instance.topLeft = positions.topLeft
        instance.bottomRight = positions.bottomRight
        instance.description = StringData(nextString(Property.MAX_STRING_LENGTH))
        instance.number = random.nextInt()
        return instance
    }

    private fun generateSize(): GeneratedSize {
        val probability = random.nextDouble()
        return if (probability < 0.2) {
            GeneratedSize.XXS
        } else if (probability < 0.5) {
            GeneratedSize.XS
        } else if (probability < 0.75) {
            GeneratedSize.S
        } else if (probability < 0.9) {
            GeneratedSize.M
        } else if (probability < 0.95) {
            GeneratedSize.L
        } else if (probability < 0.98) {
            GeneratedSize.XL
        } else {
            GeneratedSize.XXL
        }
    }

    private fun nextBoundary(size: GeneratedSize): Boundary {
        val topX = this.widthCoordinate()
        val topY = this.heightCoordinate()
        val bottomX = this.widthCoordinate(topX, size)
        val bottomY = this.heightCoordinate(topY, size)
        return Boundary(doubleArrayOf(topX, topY), doubleArrayOf(bottomX, bottomY))
    }

    private fun widthCoordinate(): Double {
        return random.nextDouble(leftX, rightX)
    }

    private fun widthCoordinate(lower: Double, size: GeneratedSize): Double {
        var higher: Double = lower + size.maxSize
        higher = if (DoubleUtils.isALessOrEqualsToB(higher, rightX)) higher else rightX
        return if (higher == lower) {
            higher
        } else random.nextDouble(lower, higher)
    }

    private fun heightCoordinate(): Double {
        return random.nextDouble(bottomY, topY)
    }

    private fun heightCoordinate(higher: Double, size: GeneratedSize): Double {
        var lower: Double = higher - size.maxSize
        lower = if (DoubleUtils.isAGreaterOrEqualsToB(lower, bottomY)) lower else bottomY
        return if (higher == lower) {
            higher
        } else random.nextDouble(lower, higher)
    }

    private fun setCoordinates(boundary: Boundary?) {
        if (boundary != null) {
            with(boundary) {
                setCoordinates(topLeft[0], topLeft[1], bottomRight[0], bottomRight[1])
            }
        }
    }

    private fun setCoordinates(leftX: Double, topY: Double, rightX: Double, bottomY: Double) {
        this.leftX = leftX
        this.topY = topY
        this.rightX = rightX
        this.bottomY = bottomY
    }

    companion object {
        const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }
}