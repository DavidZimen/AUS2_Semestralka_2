package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

import java.util.*

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
interface IData : IBlock {
    /**
     * Own logic for equals method in data is required.
     */
    override fun equals(other: Any?): Boolean

    /**
     * Creates bitset from object.
     * Own implementation for better control over hashing.
     */
    fun hash(): BitSet
}