package sk.zimen.semestralka.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import sk.zimen.semestralka.Aus2Semestralka2

class MainPageController {

    @FXML
    private lateinit var contentDetail: AnchorPane

    @FXML
    private lateinit var mainTitle: Label

    fun openParcels() {
        loadPath("parcels.fxml")
        mainTitle.text = "Parcels"
    }

    fun openProperties() {
        loadPath("properties.fxml")
        mainTitle.text = "Properties"
    }

    fun openGenerator() {
        loadPath("data-manipulation.fxml")
        mainTitle.text = "Storage / Generator"
    }

    private fun loadPath(path: String, controller: Any? = null) {
        val loader = FXMLLoader(Aus2Semestralka2::class.java.getResource(path))
        if (controller != null) loader.setController(controller)
        contentDetail.children.setAll(Pane(loader.load()))
    }
}