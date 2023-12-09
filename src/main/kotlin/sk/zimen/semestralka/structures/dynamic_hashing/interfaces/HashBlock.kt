package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

import java.util.*

/**
 * Interface that holds every method needed in block for dynamic hashing structure.
 * @author David Zimen
 */
interface HashBlock<K> {
    /**
     * Size of implementing object in bytes representation.
     */
    fun getSize(): Int

    /**
     * Converts implementing class to the [ByteArray],
     * so it can be stored in file.
     */
    fun getData(): ByteArray

    /**
     * Converts provided [ByteArray].
     */
    fun formData(bytes: ByteArray)

    /**
     * Method for printing object to console.
     */
    fun printData(hashFunc: (K) -> BitSet)
}