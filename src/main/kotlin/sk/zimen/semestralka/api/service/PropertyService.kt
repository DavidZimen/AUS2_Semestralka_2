package sk.zimen.semestralka.api.service

import sk.zimen.semestralka.api.types.GpsPosition
import sk.zimen.semestralka.api.types.GpsPositions
import sk.zimen.semestralka.api.types.Property
import sk.zimen.semestralka.exceptions.NoResultFoundException
import sk.zimen.semestralka.structures.quadtree.QuadTree
import sk.zimen.semestralka.utils.Mapper
import sk.zimen.semestralka.utils.file.loadFromCsv
import sk.zimen.semestralka.utils.file.writeToCsv
import sk.zimen.semestralka.utils.generator.Generator

class PropertyService private constructor() {
    /**
     * Main structure for holding all [Property]ies of the application.
     */
    private val properties: QuadTree<Property> = QuadTree(10)

    fun add(property: Property) {
        associateParcels(property)
        properties.insert(property)
    }

    fun edit(propertyBefore: Property, propertyAfter: Property) {
        if (propertyBefore.positions != propertyAfter.positions) {
            associateParcels(propertyAfter)
        }
        properties.edit(propertyBefore, propertyAfter)
    }

    fun find(position: GpsPosition): MutableList<Property> {
        return try {
            properties.find(Mapper.toBoundary(position)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun find(positions: GpsPositions): MutableList<Property> {
        return try {
            properties.find(Mapper.toBoundary(positions)) as MutableList
        } catch (e: NoResultFoundException) {
            mutableListOf()
        }
    }

    fun all(): MutableList<Property> = properties.all() as MutableList

    fun delete(property: Property) {
        properties.delete(property)
    }

    fun changeParameters(maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double)
        = properties.changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)

    fun generateData(count: Int, maxDepth: Int, topLeftX: Double, topLeftY: Double, bottomRightX: Double, bottomRightY: Double) {
        changeParameters(maxDepth, topLeftX, topLeftY, bottomRightX, bottomRightY)
        val items = Generator().generateItems(Property::class, count, properties.root.boundary,)
        items.forEach {
            add(it)
        }
    }

    fun saveToFile() {
        val items = all()
        val gpsPositions = ArrayList<GpsPosition>(items.size * 2)
        items.forEach {
            gpsPositions.add(it.positions.topLeft)
            gpsPositions.add(it.positions.bottomRight)
        }
        writeToCsv("data","properties.csv", Property::class, items)
        writeToCsv("data", "properties-positions.csv", GpsPosition::class, gpsPositions)
    }

    fun loadFromFile() {
        val items = loadFromCsv("data", "properties.csv", Property::class)
        val gpsPositions = loadFromCsv("data", "properties-positions.csv", GpsPosition::class)
        gpsPositions.reverse()
        items.forEach {
            val topLeft = gpsPositions.removeAt(gpsPositions.size - 1)
            val bottomRightX = gpsPositions.removeAt(gpsPositions.size - 1)
            it.positions = GpsPositions(topLeft, bottomRightX)
            add(it)
        }
    }

    private fun associateParcels(property: Property) {
        property.parcelsForProperty = ParcelService.getInstance().find(property.positions)
        property.parcelsForProperty.forEach {
            it.propertiesForParcel.add(property)
        }
    }

    companion object {
        private var instance: PropertyService? = null

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