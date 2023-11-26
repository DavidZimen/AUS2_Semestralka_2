package sk.zimen.semestralka.utils

import java.util.*


/**
 * Function which takes provided long key and transforms it with provided [modulo] to [BitSet].
 */
fun moduloHashFunction(modulo: Long): (Long) -> BitSet = { key ->
    (key % modulo).toBitSet()
}