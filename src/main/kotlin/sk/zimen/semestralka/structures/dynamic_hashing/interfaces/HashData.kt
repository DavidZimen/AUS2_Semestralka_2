package sk.zimen.semestralka.structures.dynamic_hashing.interfaces

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
interface HashData<K>: HashBlock {
    /**
     * Unique key of data to be stored in hashing structure.
     */
    var key: K

    /**
     * Own logic for equals method in data is required.
     */
    override fun equals(other: Any?): Boolean
}