package sk.zimen.semestralka.structures.dynamic_hashing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.IData
import sk.zimen.semestralka.utils.deleteDirectory
import sk.zimen.semestralka.utils.generator.GeneratedOperation
import sk.zimen.semestralka.utils.generator.Generator
import sk.zimen.semestralka.utils.moduloHashFunction

internal class DynamicHashStructureTest {

    private val generator = Generator()

    @Test
    fun randomizedTest() {
        // initialization
        val itemsCount = 25_000
        val operationsCount = 1_000
        val strName = "randomizedTest"
        val blockFactor = 10
        val overloadBlockFactor = 25
        val modulo = 2_000L
        val operationRatio = intArrayOf(1, 0, 0, 1)
        deleteDirectory("data/$strName")
        val dynamicHash = DynamicHashStructure(strName, blockFactor, overloadBlockFactor, TestItem::class, moduloHashFunction(modulo), 10)

        // generate items
        val items = generator.generateTestItems(itemsCount)

        // generate operations
        val operations = generator.generateOperations(operationsCount, operationRatio)
                ?: throw IllegalArgumentException("Wrong number of operations or wrong ratio provided.")

        dynamicHash.initialize(items)

        while (!operations.isEmpty()) {
            val operation = operations.pop()!!
            when (operation) {
                GeneratedOperation.FIND -> {
                    val item = items[generator.random.nextInt(0, items.size)]
                    val findItem = TestItem(item.key, item.desc.value)
                    try {
                        val foundItem = dynamicHash.find(findItem.key)
                        assertEquals(findItem, foundItem)
                    } catch (_: Exception) { }
                }
                GeneratedOperation.INSERT -> {
                    val item = generator.generateTestItems(1)[0]
                    try {
                        dynamicHash.insert(item)
                        assertTrue(dynamicHash.contains(item))
                        assertTrue(dynamicHash.isLastBlockOccupied())
                    } catch (_: Exception) { }
                }
                GeneratedOperation.DELETE -> {
                    // TODO so far not implemented, implement after DELETE function is done.
                }
                GeneratedOperation.EDIT -> {
                    // TODO so far not implemented, implement after EDIT function is done.
                }
            }
        }

        dynamicHash.printStructure()
        dynamicHash.save()
    }

    private fun <K, T: IData<K>> DynamicHashStructure<K, T>.initialize(items: MutableList<T>) {
        val iterator = items.iterator()

        while (iterator.hasNext()) {
            val item = iterator.next()

            try {
                insert(item)
            } catch (e: IllegalStateException) {
                iterator.remove()
            }
        }
    }
}
