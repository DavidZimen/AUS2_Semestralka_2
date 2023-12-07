package sk.zimen.semestralka.structures.dynamic_hashing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.structures.dynamic_hashing.interfaces.DynamicHashData
import sk.zimen.semestralka.utils.deleteDirectory
import sk.zimen.semestralka.utils.generator.GeneratedOperation
import sk.zimen.semestralka.utils.generator.Generator
import sk.zimen.semestralka.utils.moduloHashFunction

internal class DynamicHashStructureRandomizedTest {

    private val generator = Generator()

    @Test
    fun randomizedTest() {
        println("Seed for generator is: ${generator.seed}")
        // initialization
        val itemsCount = 10_000
        val operationsCount = 2_000
        val strName = "randomizedTest"
        val blockFactor = 6
        val overloadBlockFactor = 15
        val modulo = 80L
        val operationRatio = intArrayOf(1, 1, 1, 1)
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
                    val foundItem = dynamicHash.find(findItem.key)
                    assertEquals(findItem, foundItem)
                }
                GeneratedOperation.INSERT -> {
                    val item = generator.generateTestItems(1)[0]
                    try {
                        dynamicHash.insert(item)
                        items.add(item)
                        assertTrue(dynamicHash.contains(item))
                        assertTrue(dynamicHash.isLastBlockOccupied())
                    } catch (e: IllegalArgumentException) {
                        println(e.message)
                    }
                }
                GeneratedOperation.DELETE -> {
                    val item = items.removeAt(generator.random.nextInt(0, items.size))
                    dynamicHash.delete(item.key)
                    assertFalse(dynamicHash.contains(item))
                    assertTrue(dynamicHash.isLastBlockOccupied())
                }
                GeneratedOperation.REPLACE -> {
                    val index = generator.random.nextInt(0, items.size)
                    val oldItem = items[index]
                    val newItem = TestItem(oldItem.key, generator.nextString(TestItem.MAX_STRING_LENGTH))
                    items[index] = newItem

                    dynamicHash.replace(oldItem, newItem)
                    assertTrue(dynamicHash.contains(newItem))
                    assertFalse(dynamicHash.contains(oldItem))
                    assertEquals(newItem, dynamicHash.find(newItem.key))
                }
            }
        }

        dynamicHash.printStructure()
        dynamicHash.save()
    }

    private fun <K, T: DynamicHashData<K>> DynamicHashStructure<K, T>.initialize(items: MutableList<T>) {
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
