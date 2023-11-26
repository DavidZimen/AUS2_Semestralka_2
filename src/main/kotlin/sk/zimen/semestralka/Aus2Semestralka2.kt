package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import sk.zimen.semestralka.api.types.TestItem
import sk.zimen.semestralka.structures.dynamic_hashing.DynamicHash
import sk.zimen.semestralka.utils.moduloHashFunction

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
    val hash = DynamicHash("mainTest", 1, 3, TestItem::class, moduloHashFunction(50))
    val testItem1 = TestItem(1, "Description 1")
    val testItem2 = TestItem(2, "Description 2")
    val testItem3 = TestItem(3, "Description 3")
    val testItem4 = TestItem(4, "Description 4")
    val testItem5 = TestItem(5, "Description 5")
    val testItem6 = TestItem(6, "Description 6")

    hash.insert(testItem1)
    hash.insert(testItem2)
    hash.insert(testItem3)
    hash.insert(testItem4)
    hash.insert(testItem5)
    hash.insert(testItem6)
    hash.printStructure()

    hash.save()
}