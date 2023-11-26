package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

import java.util.*

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
interface IData<T> : IBlock {

    /**
     * Return key for the class.
     */
    fun key(): T

    /**
     * Own logic for equals method in data is required.
     */
    override fun equals(other: Any?): Boolean

    /**
     * Creates bitset from object.
     * Own implementation for better control over hashing.
     */
    fun hash(): BitSet

    /**
     * Method for printing object to console.
     */
    fun printData()

    /**
     * Creates a new empty instance of the implementing class.
     */
    fun createInstance(): IBlock
}