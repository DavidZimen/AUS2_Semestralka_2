package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import sk.zimen.semestralka.api.service.ParcelService
import sk.zimen.semestralka.api.service.PropertyService

class Aus2Semestralka2 : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(Aus2Semestralka2::class.java.getResource("main-page.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Semestrálna práca AUS2"
        stage.icons.add(Image(Aus2Semestralka2::class.java.getResourceAsStream("icons/SYMBOL_T_biela.png")))
        stage.scene = scene

        val propertyService = PropertyService.getInstance()
        val parcelService = ParcelService.getInstance()

        stage.setOnCloseRequest {
            propertyService.saveToFile()
            parcelService.saveToFile()
            println("Application is closing.")
        }
        stage.show()

        propertyService.loadFromFile()
        parcelService.loadFromFile()
    }
}

fun main() {
    Application.launch(Aus2Semestralka2::class.java)

//    val item = Generator().generateItems(Parcel::class, 1)[0]
//    val data = item.getData()
//    val newItem = Parcel()
//    newItem.formData(data)
//    println(item.key)
}