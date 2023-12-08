package sk.zimen.semestralka.utils

import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.structures.dynamic_hashing.DynamicHashStructure
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.file.deleteDirectory
import sk.zimen.semestralka.utils.generator.Generator

fun initHashStructure(generator: Generator = Generator()): TestInit<Long, TestItem> {
    return initHashStructure(
        itemsCount = 10_000,
        structureName = "randomizedTest",
        blockFactor = 6,
        overloadBlockFactor = 10,
        hashTrieDepth = 10,
        modulo = 100L,
        generator = generator
    )
}

fun initHashStructure(
    itemsCount: Int,
    structureName: String,
    blockFactor: Int,
    overloadBlockFactor: Int,
    hashTrieDepth: Int,
    modulo: Long,
    generator: Generator = Generator()
): TestInit<Long, TestItem> {
    deleteDirectory("data/$structureName")
    val dynamicHash = DynamicHashStructure(structureName, blockFactor, overloadBlockFactor, TestItem::class, moduloHashFunction(modulo), hashTrieDepth)

    // generate items
    val items = generator.generateTestItems(itemsCount)
    dynamicHash.initialize(items)

    return TestInit(dynamicHash, items)
}

fun <K, T: IData<K>> DynamicHashStructure<K, T>.initialize(items: MutableList<T>) {
    val iterator = items.iterator()

    while (iterator.hasNext()) {
        val item = iterator.next()
        insert(item)
    }
}

data class TestInit <K, T : IData<K>> (
    val structure: DynamicHashStructure<K, T>,
    val insertedItems: MutableList<T>
)