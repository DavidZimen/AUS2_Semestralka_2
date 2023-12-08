package sk.zimen.semestralka.structures.dynamic_hashing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import sk.zimen.semestralka.utils.generator.Generator
import sk.zimen.semestralka.utils.initHashStructure

class DynamicHashStructureTest {

    @Test
    fun testDelete() {
        val generator = Generator()
        val testInit = initHashStructure(generator)
        val structure = testInit.structure
        val items = testInit.insertedItems

        println("Seed for generator is: ${generator.seed}")

        var deleteCount = 0
        while (items.isNotEmpty()) {
            println(++deleteCount)
            if (deleteCount == 10000)
                println("Problem item")

            val deleteItem = items.removeAt(generator.random.nextInt(0, items.size))
            structure.delete(deleteItem.key)

            assertFalse(structure.contains(deleteItem))
            assertTrue(structure.isLastBlockOccupied())
            assertEquals(structure.size, items.size)
            if (structure.size == 0) {
                assertTrue(structure.isStateInitial())
            }
        }
    }
}