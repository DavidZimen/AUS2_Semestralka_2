package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import sk.zimen.semestralka.structures.dynamic_hashing.DynamicHash
import sk.zimen.semestralka.structures.dynamic_hashing.TestItem
import sk.zimen.semestralka.structures.dynamic_hashing.types.Block

class Aus2Semestralka2 : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(Aus2Semestralka2::class.java.getResource("main-page.fxml"))
        val scene = Scene(fxmlLoader.load(), 320.0, 240.0)
        stage.title = "Bla!"
        stage.scene = scene

        stage.setOnCloseRequest { println("Application is closing.") }
        stage.show()
    }
}

fun main() {
//    Application.launch(Aus2Semestralka2::class.java)
    val hash = DynamicHash("file", 1, TestItem::class)
    val testItem1 = TestItem().apply {
        id = 1
        desc = "Description 1"
    }
    val testItem2 = TestItem().apply {
        id = 2
        desc = "Description 2"
    }
    val testItem3 = TestItem().apply {
        id = 3
        desc = "Description 3"
    }
    val testItem4 = TestItem().apply {
        id = 4
        desc = "Description 4"
    }
    val testItem5 = TestItem().apply {
        id = 5
        desc = "Description 5"
    }
    val testItem6 = TestItem().apply {
        id = 6
        desc = "Description 6"
    }

    hash.insert(testItem1)
    hash.insert(testItem2)
    hash.insert(testItem3)
    hash.insert(testItem4)
    hash.insert(testItem5)
    hash.insert(testItem6)
    val blocks = ArrayList<Block<TestItem>>()
    for (i in 0 until hash.file.length() / hash.blockSize) {
        blocks.add(hash.getBlock(i.toInt()))
    }
    hash.save()
}