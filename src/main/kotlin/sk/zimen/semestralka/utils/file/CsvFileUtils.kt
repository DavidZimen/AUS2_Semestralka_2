package sk.zimen.semestralka.utils.file

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.*

/**
 * Annotation class, to annotate properties in classes,
 * which should not be written into CSV file.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class CsvExclude


private const val DELIMITER = ";"

/**
 * Uses kotlin-reflection library to get all
 * properties, that are not annotated with [CsvExclude].
 */
fun <T : Any> writeDataToCSV(directory: String, fileName: String, clazz: KClass<T>, data: List<T>) {
    if (data.isEmpty()) return

    val classProperties = getValidProperties(clazz)
    val header = classProperties.joinToString(DELIMITER) { it.name }
    val lines = data.joinToString("\n") { item ->
            classProperties.joinToString(DELIMITER) { it.get(item).toString() }
    }

    val csvData = "$header\n$lines"
    File(directory, fileName).writeText(csvData)
}

/**
 * With kotlin reflection library creates instances of [clazz].
 * Then sets its attributes to loaded data, based on header.
 * Header must match property name.
 * Supports reading of [String], [Int], [Double] and simple string [Enum] values.
 */
fun <T : Any> readDataFromCSV(directory: String, fileName: String, clazz: KClass<T>): MutableList<T> {
    val lines = File(directory, fileName).readText().lines()
    val objects = mutableListOf<T>()

    if (lines.isNotEmpty()) {
        val header = lines[0].split(DELIMITER)
        val propertyMap = getValidProperties(clazz).associateBy { it.name }

        for (line in lines.drop(1)) {
            val values = line.split(DELIMITER)
            if (values.size == header.size) {
                val objectInstance = clazz.createInstance()

                for ((index, value) in values.withIndex()) {
                    val propertyName = header[index]
                    val property = propertyMap[propertyName]
                    if (property != null) {
                        val propValue = when {
                            property.returnType.isSubtypeOf(Enum::class.starProjectedType) -> {
                                val enumClass = property.returnType.classifier as KClass<Enum<*>>
                                enumClass.java.enumConstants.first { it.name == value }
                            }
                            property.returnType == Int::class.starProjectedType -> value.toInt()
                            property.returnType == Double::class.starProjectedType -> value.toDouble()
                            property.returnType == String::class.starProjectedType -> value
                            property.returnType == String::class.starProjectedType.withNullability(true) -> value
                            else -> throw IllegalArgumentException("Unsupported type: ${property.returnType}")
                        }
                        property.setter.call(objectInstance, propValue)
                    }
                }
                objects.add(objectInstance)
            }
        }
    }

    return objects
}

/**
 * Returns properties of [clazz], that are mot annotated with [CsvExclude].
 * All item must be mutable for this function to work.
 */
private fun <T : Any> getValidProperties(clazz: KClass<T>): List<KMutableProperty1<T, *>> {
    return clazz.memberProperties
        .filter { it.findAnnotation<CsvExclude>() == null }
        .map { it as KMutableProperty1<T, *>}
}