package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Aus2Semestralka2 : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(Aus2Semestralka2::class.java.getResource("main-page.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Semestrálna práca AUS2"
        stage.icons.add(Image(Aus2Semestralka2::class.java.getResourceAsStream("icons/SYMBOL_T_biela.png")))
        stage.scene = scene

        stage.setOnCloseRequest {
            println("Application is closing.")
            //PropertyService.getInstance().saveToFile()
            //ParcelService.getInstance().saveToFile()
        }
        stage.show()
    }
}

fun main() {
    Application.launch(Aus2Semestralka2::class.java)
//    val hash = DynamicHashStructure("randomizedTest", 8, 15, TestItem::class, moduloHashFunction(500L), 10)
//    val item = hash.find(8019468532429308957L)
//    println("Found item - key: ${item.key}, desc: ${item.desc.value}")
}