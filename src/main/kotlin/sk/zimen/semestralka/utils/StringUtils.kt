package sk.zimen.semestralka.utils


/**
 * Class that represents string value and number of valid characters in that string
 */
class StringData() {

    var validChars: Int = 0
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

        index = bytes.append(numberToByteArray(validChars), index)
        bytes.append(value.fillRemainingString(maxStringLength).toByteArray(), index)

        return bytes
    }

    fun formData(bytes: ByteArray, maxStringLength: Int) {
        var index = 0
        with(byteArrayToNumber(bytes.copyOfRange(index, index + Int.SIZE_BYTES), index, Int::class)) {
            validChars = number as Int
            index = newIndex
        }
        value = String(bytes.copyOfRange(index, index + maxStringLength)).getValidString(validChars)
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
 * Transforms [input] to fill remaining spaces to [length] with [EMPTY_CHAR].
 */
fun String.fillRemainingString(maxLength: Int): String {
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