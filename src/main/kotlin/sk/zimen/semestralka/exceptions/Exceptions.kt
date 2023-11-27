package sk.zimen.semestralka.exceptions

class MultipleResultsFoundException(message: String) : RuntimeException(message)

class NoResultFoundException(message: String) : RuntimeException(message)

class PositionException(message: String) : RuntimeException(message)

class BlockIsFullException(message: String) : RuntimeException(message)

class BoundaryException(message: String) : RuntimeException(message) {
    constructor(topLeft: DoubleArray, bottomRight: DoubleArray) : this(
        "Provided coordinates can not make a boundary. " +
                "Top: [" + topLeft[0] + "," + topLeft[1] + "] " +
                "Bottom [" + bottomRight[0] + "," + bottomRight[1] + "]"
    )
}
