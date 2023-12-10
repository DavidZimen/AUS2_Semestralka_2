package sk.zimen.semestralka.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import sk.zimen.semestralka.Aus2Semestralka2
import sk.zimen.semestralka.ui.state.WindowState

class MainPageController {

    @FXML
    private lateinit var contentDetail: AnchorPane

    @FXML
    private lateinit var mainTitle: Label

    fun openParcels() {
        WindowState.getInstance().isParcel = true
        loadPath("structures-tab.fxml")
        mainTitle.text = "Parcels"
    }

    fun openProperties() {
        WindowState.getInstance().isParcel = false
        loadPath("structures-tab.fxml")
        mainTitle.text = "Properties"
    }

    fun openGenerator() {
        loadPath("generator.fxml")
        mainTitle.text = "Generator"
    }

    private fun loadPath(path: String, controller: Any? = null) {
        val loader = FXMLLoader(Aus2Semestralka2::class.java.getResource(path))
        if (controller != null) loader.setController(controller)
        contentDetail.children.setAll(Pane(loader.load()))
    }
}