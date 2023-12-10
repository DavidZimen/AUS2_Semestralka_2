package sk.zimen.semestralka.api.service

import sk.zimen.semestralka.api.types.*
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.dynamic_hashing.DynamicHashStructure
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

    init {
        initializeDirectory(directory)
    }

    fun add(property: Property) {
        associateParcels(property)
        propertiesQuadTree.insert(property as QuadTreePlace)
        propertiesHash.insert(property)
    }

    fun edit(propertyBefore: Property, propertyAfter: Property) {
        if (propertyBefore.topLeft != propertyAfter.topLeft || propertyBefore.bottomRight != propertyAfter.bottomRight) {
            associateParcels(propertyAfter)
        }
        propertiesQuadTree.edit(propertyBefore as QuadTreePlace, propertyAfter as QuadTreePlace)
        propertiesHash.replace(propertyBefore, propertyAfter)
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
        propertiesQuadTree.delete(property)
        propertiesHash.delete(property.key)
    }

    fun changeParameters(maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double)
        = propertiesQuadTree.changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)

    fun generateData(count: Int, maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double) {
        changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)
        val items = Generator().generateItems(Property::class, count, propertiesQuadTree.root.boundary,)
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
            val topLeft = gpsPositions.removeAt(gpsPositions.size - 1)
            val bottomRightX = gpsPositions.removeAt(gpsPositions.size - 1)
            it.topLeft = topLeft
            it.bottomRight = bottomRightX
            propertiesQuadTree.insert(it)
        }
    }

    private fun associateParcels(property: Property) {
        property.parcelsForProperty = ParcelService.getInstance()
            .find(GpsPositions(property.topLeft, property.bottomRight))
            .map { AssociatedPlace(it.key) }
            .toMutableList()

        if (property.parcelsForProperty.size > Property.MAX_ASSOCIATED_PARCELS)
            throw IllegalStateException("Property cannot have more than ${Property.MAX_ASSOCIATED_PARCELS} parcels associated.")

        // TODO associate also other way arround
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