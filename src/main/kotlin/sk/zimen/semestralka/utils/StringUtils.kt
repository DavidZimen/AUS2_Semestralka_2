package sk.zimen.semestralka.utils


/**
 * Class that represents string value and number of valid characters in that string
 */
class StringData() {

    private var validChars: Int = 0

    var value: String = ""
        set(value) {
            field = value
            validChars = field.length
        }

    constructor(value: String) : this() {
        this.value = value
        validChars = value.length
    }

    fun getData(maxStringLength: Int): ByteArray {
        var index = 0
        val bytes = ByteArray(getSize(maxStringLength))

        index = bytes.append(validChars.toByteArray(), index)
        bytes.append(value.fillRemaining(maxStringLength).toByteArray(), index)

        return bytes
    }

    fun formData(bytes: ByteArray, maxStringLength: Int) {
        var index = 0
        bytes.copyOfRange(index, Int.SIZE_BYTES).toNumber(index, Int::class).also {
            validChars = it.number as Int
            index = it.newIndex
        }
        value = String(bytes.copyOfRange(index, index + maxStringLength)).getValidString(validChars)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        return other is StringData && value == other.value && validChars == other.validChars
    }

    override fun toString(): String {
        return "Value: $value, valid chars: $validChars."
    }

    companion object {
        fun getSize(maxStringLength: Int): Int {
            return Int.SIZE_BYTES + maxStringLength
        }
    }
}

/**
 * Empty character to fill in the string.
 */
private const val EMPTY_CHAR = '/'

/**
 * Transforms [String] to fill remaining spaces to [maxLength] with [EMPTY_CHAR].
 */
fun String.fillRemaining(maxLength: Int): String {
    return if (length > maxLength) {
        take(maxLength)
    } else {
        padEnd(maxLength, EMPTY_CHAR)
    }
}

/**
 * Returns only string containing first [validCount] characters.
 */
fun String.getValidString(validCount: Int) = take(validCount)

/**
 * Replaces last character of string with [newChar].
 */
fun String.replaceLast(newChar: Char): String {
    if (isEmpty())
        return this
    return substring(0, length - 1) + newChar
}