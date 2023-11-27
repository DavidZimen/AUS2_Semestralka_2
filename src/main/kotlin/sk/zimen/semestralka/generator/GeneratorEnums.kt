package sk.zimen.semestralka.generator

import sk.zimen.semestralka.api.types.GpsPosition

/**
 * Enum values to represent max distance between 2 generated [GpsPosition]s.
 * @author David Zimen
 */
enum class GeneratedSize(val maxSize: Double) {
    XXS(0.0001),
    XS(0.001),
    S(0.1),
    M(0.2),
    L(0.5),
    XL(1.0),
    XXL(2.0)
}

/**
 * Enum values to represent operation to invoke in testing.
 * @author David Zimen
 */
enum class GeneratedOperation() {
    DELETE,
    INSERT,
    FIND,
    EDIT
}