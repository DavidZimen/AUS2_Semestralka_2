package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

import java.util.*

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
abstract class IData<K>: IBlock {
    /**
     * Unique key of data to be stored in dynamic hashing structure.
     */
    abstract var key: K

    /**
     * Own logic for equals method in data is required.
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * Method for printing object to console.
     */
    abstract fun printData(hashFunc: (K) -> BitSet)
}