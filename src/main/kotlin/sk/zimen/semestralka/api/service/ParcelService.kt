package sk.zimen.semestralka.api.service

import sk.zimen.semestralka.api.types.*
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.DynamicHashStructure
import sk.zimen.semestralka.structures.dynamic_hashing.util.DynamicHashMetadata
import sk.zimen.semestralka.structures.dynamic_hashing.util.HashPrint
import sk.zimen.semestralka.structures.dynamic_hashing.util.ROOT_DIRECTORY
import sk.zimen.semestralka.structures.quadtree.QuadTree
import sk.zimen.semestralka.utils.Mapper
import sk.zimen.semestralka.utils.file.existsFileInDirectory
import sk.zimen.semestralka.utils.file.initializeDirectory
import sk.zimen.semestralka.utils.file.loadFromCsv
import sk.zimen.semestralka.utils.file.writeToCsv
import sk.zimen.semestralka.utils.generator.Generator
import sk.zimen.semestralka.utils.moduloHashFunction


class ParcelService private constructor() {

    private val directory = "$ROOT_DIRECTORY/$NAME/quad_tree"
    /**
     * QuadTree structure for holding all [Parcel]ies of the application.
     */
    private val parcelsQuadTree: QuadTree<QuadTreePlace> = QuadTree(10)

    /**
     * DynamicHashStructure for holding all [Parcel]ies of the application.
     */
    private val parcelsHash = DynamicHashStructure(NAME, 5, 5, Parcel::class, moduloHashFunction(1000L), 5)

    init {
        initializeDirectory(directory)
    }

    fun add(parcel: Parcel) {
        associateProperties(parcel)
        parcelsQuadTree.insert(parcel as QuadTreePlace)
        parcelsHash.insert(parcel)
    }

    fun edit(parcelBefore: Parcel, parcelAfter: Parcel) {
        if (parcelBefore.topLeft != parcelAfter.topLeft || parcelBefore.bottomRight != parcelAfter.bottomRight) {
            associateProperties(parcelAfter)
        }
        parcelsQuadTree.edit(parcelBefore as QuadTreePlace, parcelAfter as QuadTreePlace)
        parcelsHash.replace(parcelAfter, parcelAfter)
    }

    fun find(position: GpsPosition): MutableList<QuadTreePlace> {
        return try {
            parcelsQuadTree.find(Mapper.toBoundary(position)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun find(positions: GpsPositions): MutableList<QuadTreePlace> {
        return try {
            parcelsQuadTree.find(Mapper.toBoundary(positions)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun find(key: Long): Parcel = parcelsHash.find(key)

    fun all(): MutableList<QuadTreePlace> = parcelsQuadTree.all() as MutableList

    fun delete(parcel: QuadTreePlace) {
        parcelsQuadTree.delete(parcel)
    }

    fun hashStructurePrint(): HashPrint {
        return HashPrint("This is main", "This is overload")
//        return parcelsHash.sequentialPrint()
    }

    fun changeParameters(maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double)
        = parcelsQuadTree.changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)

    fun generateData(
        count: Int,
        quadTreeMaxDepth: Int,
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double,
        mainBlockFactor: Int,
        overloadBlockFactor: Int,
        maxTrieDepth: Int
    ) {
        parcelsHash.reset(DynamicHashMetadata(mainBlockFactor, -1, -1, -1, maxTrieDepth, overloadBlockFactor))
        parcelsQuadTree.reset()
        changeParameters(quadTreeMaxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)
        val items = Generator().generateItems(Parcel::class, count, parcelsQuadTree.root.boundary)
        items.forEach {
            add(it)
        }
    }

    fun saveToFile() {
        val items = all()
        val gpsPositions = ArrayList<GpsPosition>(items.size * 2)
        items.forEach {
            gpsPositions.add(it.topLeft)
            gpsPositions.add(it.bottomRight)
        }
        writeToCsv(directory, "parcels.csv", QuadTreePlace::class, items)
        writeToCsv(directory, "parcels-positions.csv", GpsPosition::class, gpsPositions)

        parcelsHash.save()
    }

    fun loadFromFile() {
        if (!existsFileInDirectory(directory, "parcels.csv"))
            return

        val items = loadFromCsv(directory, "parcels.csv", QuadTreePlace::class)
        val gpsPositions = loadFromCsv(directory, "parcels-positions.csv", GpsPosition::class)
        gpsPositions.reverse()
        items.forEach {
            val topLeft = gpsPositions.removeAt(gpsPositions.size - 1)
            val bottomRightX = gpsPositions.removeAt(gpsPositions.size - 1)
            it.topLeft = topLeft
            it.bottomRight = bottomRightX
            parcelsQuadTree.insert(it)
        }
    }

    private fun associateProperties(parcel: Parcel) {
        parcel.propertiesForParcel = PropertyService.getInstance()
            .find(GpsPositions(parcel.topLeft, parcel.bottomRight))
            .map { AssociatedPlace(it.key) }
            .toMutableList()

        if (parcel.propertiesForParcel.size > Parcel.MAX_ASSOCIATED_PROPERTIES)
            throw IllegalStateException("Parcel cannot have more than ${Parcel.MAX_ASSOCIATED_PROPERTIES} parcels associated.")

        // TODO associate also other way around
        // dont forget to rollback when some error is thrown
    }

    companion object {
        private var instance: ParcelService? = null

        private const val NAME = "parcels"

        fun getInstance(): ParcelService {
            if (instance == null) {
                synchronized(this) {
                    instance = ParcelService()
                }
            }
            return instance!!
        }
    }
}