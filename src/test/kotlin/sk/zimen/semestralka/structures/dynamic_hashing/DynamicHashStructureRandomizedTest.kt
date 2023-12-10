package sk.zimen.semestralka.structures.dynamic_hashing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.utils.generator.GeneratedOperation
import sk.zimen.semestralka.utils.generator.Generator
import sk.zimen.semestralka.utils.initHashStructure

internal class DynamicHashStructureRandomizedTest {

    private val generator = Generator()

    @Test
    fun randomizedTest() {
        println("Seed for generator is: ${generator.seed}")
        // initialization
        val operationRatio = intArrayOf(1, 1, 1, 1)
        val operations = generator.generateOperations(2_000, operationRatio)
            ?: throw IllegalArgumentException("Wrong number of operations or wrong ratio provided.")

        val testInit = initHashStructure(
            itemsCount = 5_000,
            structureName = "randomizedTest",
            blockFactor = 8,
            overloadBlockFactor = 15,
            hashTrieDepth = 10,
            modulo = 500L,
            generator = generator
        )

        val dynamicHash = testInit.structure
        val items = testInit.insertedItems

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

                    dynamicHash.edit(oldItem, newItem)
                    assertTrue(dynamicHash.contains(newItem))
                    assertFalse(dynamicHash.contains(oldItem))
                    assertEquals(newItem, dynamicHash.find(newItem.key))
                }
            }
        }

        println(items[0].key)

        dynamicHash.save()
    }
}
