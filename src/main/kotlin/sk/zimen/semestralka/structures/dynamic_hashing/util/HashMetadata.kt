package sk.zimen.semestralka.structures.dynamic_hashing.util

/**
 * Class to store last state of [DynamicHashStructure] into file.
 */
open class HashMetadata() {
    var blockFactor: Int = -1
    var firstEmptyBlock: Long = -1
    var blockSize: Int = -1

    constructor(blockFactor: Int, firstEmptyBlock: Long, blockSize: Int) : this() {
        this.blockFactor = blockFactor
        this.firstEmptyBlock = firstEmptyBlock
        this.blockSize = blockSize
    }
}

class DynamicHashMetadata() : HashMetadata() {
    var size: Int = 0
    var trieDepth: Int = 0
    var overloadBlockFactor: Int = 0

    constructor(blockFactor: Int, firstEmptyBlock: Long, blockSize: Int, size: Int, trieDepth: Int) : this() {
        this.size = size
        this.trieDepth = trieDepth
        this.blockFactor = blockFactor
        this.blockSize = blockSize
        this.firstEmptyBlock = firstEmptyBlock
    }

    constructor(blockFactor: Int,
                firstEmptyBlock: Long,
                blockSize: Int,
                size: Int,
                trieDepth: Int,
                overloadBlockFactor: Int)
            : this(
                blockFactor,
                firstEmptyBlock,
                blockSize,
                size,
                trieDepth
            ) {
        this.overloadBlockFactor = overloadBlockFactor
    }
}