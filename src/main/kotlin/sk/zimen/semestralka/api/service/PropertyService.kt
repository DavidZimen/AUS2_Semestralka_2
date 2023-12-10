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

class PropertyService private constructor() {

    private val directory = "$ROOT_DIRECTORY/$NAME/quad_tree"

    /**
     * QuadTree structure for holding all [Property]ies of the application.
     */
    private val propertiesQuadTree: QuadTree<QuadTreePlace> = QuadTree(10)

    /**
     * DynamicHashStructure for holding all [Property]ies of the application.
     */
    private val propertiesHash = DynamicHashStructure(NAME, 5, 5, Property::class, moduloHashFunction(1000L), 5)

    private val keys= mutableSetOf<Long>()

    private val generator = Generator()

    init {
        initializeDirectory(directory)
    }

    @Throws(IllegalStateException::class)
    fun add(property: Property) {
        property.key = generator.generateLongKey(keys)
        //associateParcels(property)
        propertiesQuadTree.insert(property as QuadTreePlace)
        propertiesHash.insert(property)
    }

    @Throws(IllegalStateException::class)
    fun edit(propertyBefore: Property, propertyAfter: Property) {
        if (propertyBefore.topLeft != propertyAfter.topLeft || propertyBefore.bottomRight != propertyAfter.bottomRight) {
            associateParcels(propertyAfter)
        }
        propertiesQuadTree.edit(propertyBefore as QuadTreePlace, propertyAfter as QuadTreePlace)
        propertiesHash.edit(propertyBefore, propertyAfter)
    }

    fun find(key: Long): Property = propertiesHash.find(key)

    fun find(position: GpsPosition): MutableList<QuadTreePlace> {
        return try {
            propertiesQuadTree.find(Mapper.toBoundary(position)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun find(positions: GpsPositions): MutableList<QuadTreePlace> {
        return try {
            propertiesQuadTree.find(Mapper.toBoundary(positions)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun all(): MutableList<QuadTreePlace> = propertiesQuadTree.all() as MutableList

    fun delete(property: QuadTreePlace) {
        ParcelService.getInstance().removeParcelFromAssociatedParcels(propertiesHash.delete(property.key))
        propertiesQuadTree.delete(property)
    }

    fun hashStructurePrint(): HashPrint {
        return propertiesHash.sequentialPrint()
    }

    fun changeParameters(maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double)
        = propertiesQuadTree.changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)

    fun generateData(
        count: Int,
        maxDepth: Int,
        topLeftX: Double,
        topLeftY: Double,
        bottomRightX: Double,
        bottomRightY: Double,
        mainBlockFactor: Int,
        overloadBlockFactor: Int,
        maxTrieDepth: Int
    ) {
        propertiesHash.reset(DynamicHashMetadata(mainBlockFactor, -1, -1, -1, maxTrieDepth, overloadBlockFactor))
        propertiesQuadTree.reset()
        changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)
        val items = generator.generateItems(Property::class, count, propertiesQuadTree.root.boundary,)
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
        writeToCsv(directory,"properties.csv", QuadTreePlace::class, items)
        writeToCsv(directory, "properties-positions.csv", GpsPosition::class, gpsPositions)

        propertiesHash.save()
    }

    fun loadFromFile() {
        if (!existsFileInDirectory(directory, "properties.csv"))
            return

        val items = loadFromCsv(directory, "properties.csv", QuadTreePlace::class)
        val gpsPositions = loadFromCsv(directory, "properties-positions.csv", GpsPosition::class)
        gpsPositions.reverse()
        items.forEach {
            keys.add(it.key)
            val topLeft = gpsPositions.removeAt(gpsPositions.size - 1)
            val bottomRightX = gpsPositions.removeAt(gpsPositions.size - 1)
            it.topLeft = topLeft
            it.bottomRight = bottomRightX
            propertiesQuadTree.insert(it)
        }
    }

    fun removeParcelFromAssociatedProperties(parcel: Parcel) {
        parcel.propertiesForParcel.forEach {
            val propertyBefore = propertiesHash.find(it.key)
            val propertyAfter = propertyBefore.clone()
            propertyAfter.parcelsForProperty.removeIf { it.key == parcel.key }
            propertyAfter.validAssociated--
            propertiesHash.edit(propertyBefore, propertyAfter)
        }
    }

    @Throws(IllegalStateException::class)
    fun associateProperties(properties: List<AssociatedPlace>, parcel: Long) {
        // list of already written changes
        val updated: MutableList<Pair<Property, Property>> = mutableListOf()

        try {
            properties.forEach {
                val propertyBefore = propertiesHash.find(it.key)
                val propertyAfter = propertyBefore.clone()
                propertyAfter.parcelsForProperty.add(AssociatedPlace(parcel))
                propertyAfter.validAssociated++

                if (propertyAfter.validAssociated > Property.MAX_ASSOCIATED_PARCELS)
                    throw IllegalStateException("Property cannot have more than ${Property.MAX_ASSOCIATED_PARCELS} parcels associated.")

                propertiesHash.edit(propertyBefore, propertyAfter)
                updated.add(Pair(propertyBefore, propertyAfter))
            }
        } catch (e: IllegalStateException) {
            updated.forEach {
                propertiesHash.edit(it.second, it.first)
            }
            throw IllegalStateException("Property cannot have more than ${Property.MAX_ASSOCIATED_PARCELS} parcels associated.")
        }
    }

    @Throws(IllegalStateException::class)
    private fun associateParcels(property: Property) {
        val parcelService = ParcelService.getInstance()
        property.parcelsForProperty = parcelService
            .find(GpsPositions(property.topLeft, property.bottomRight))
            .map { AssociatedPlace(it.key) }
            .toMutableList()
        property.validAssociated = property.parcelsForProperty.size.toShort()

        if (property.validAssociated > Property.MAX_ASSOCIATED_PARCELS)
            throw IllegalStateException("Property cannot have more than ${Property.MAX_ASSOCIATED_PARCELS} parcels associated.")

        parcelService.associateParcels(property.parcelsForProperty, property.key)
    }

    companion object {
        private var instance: PropertyService? = null

        private const val NAME = "properties"

        fun getInstance(): PropertyService {
            if (instance == null) {
                synchronized(this) {
                    instance = PropertyService()
                }
            }
            return instance!!
        }
    }
}