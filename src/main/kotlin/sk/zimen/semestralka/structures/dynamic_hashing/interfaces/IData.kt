package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
abstract class IData<T>: IBlock {

    abstract var key: T
    /**
     * Own logic for equals method in data is required.
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * Method for printing object to console.
     */
    abstract fun printData()
}