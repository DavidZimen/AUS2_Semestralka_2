package sk.zimen.semestralka.structures.dynamic_hashing.generics

/**
 * Interface containing methods for data to be inserted in dynamic hashing structure.
 * @author David Zimen
 */
abstract class IData<K>: IBlock<K> {
    /**
     * Own logic for equals method in data is required.
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * Returns size of a key for data.
     */
    abstract fun getKeySize(): Int
}