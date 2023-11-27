package sk.zimen.semestralka.structures.dynamic_hashing.types

import sk.zimen.semestralka.structures.dynamic_hashing.generics.IData

/**
 * Class, that holds key and its corresponding data in dynamic hash data structure.
 * @author David Zimen
 */
class DHData<K, T : IData<K>>() {

    var key: K? = null

    var data: T? = null

    constructor(key: K, data: T) : this() {
        this.key = key
        this.data = data
    }
}