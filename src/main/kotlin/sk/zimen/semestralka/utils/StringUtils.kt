package sk.zimen.semestralka.utils


/**
 * Empty character to fill in the string.
 */
private const val EMPTY_CHAR = '/'

/**
 * Transforms [input] to fill remaining spaces to [length] with [EMPTY_CHAR].
 */
fun fillRemainingString(input: String, length: Int) = input.padEnd(length, EMPTY_CHAR)

/**
 * Returns only string containing first [validCount] characters.
 */
fun getValidString(input: String, validCount: Int) = input.take(validCount)