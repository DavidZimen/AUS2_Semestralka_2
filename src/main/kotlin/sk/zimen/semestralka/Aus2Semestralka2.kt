package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import sk.zimen.semestralka.api.types.Parcel
import sk.zimen.semestralka.utils.generator.Generator

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
//    Application.launch(Aus2Semestralka2::class.java)

    val item = Generator().generateItems(Parcel::class, 1)[0]
    val data = item.getData()
    val newItem = Parcel()
    newItem.formData(data)
    println(item.key)
}