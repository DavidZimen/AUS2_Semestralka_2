package sk.zimen.semestralka.utils

/**
 * Function which takes provided integer key and transforms it with modulo.
 */
fun moduloHashFunction(key: Long) =  numberToBitSet(key % 50)