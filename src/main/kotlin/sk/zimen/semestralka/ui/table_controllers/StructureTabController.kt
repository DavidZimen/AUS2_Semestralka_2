package sk.zimen.semestralka.ui.table_controllers

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Tab
import sk.zimen.semestralka.ui.state.WindowState
import sk.zimen.semestralka.ui.util.setContent
import java.net.URL
import java.util.*

class StructureTabController : Initializable {

    @FXML
    private lateinit var tableTab: Tab
    @FXML
    private lateinit var printTab: Tab

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        val state = WindowState.getInstance()
        tableTab.setContent(if (state.isParcel) "parcels.fxml" else "properties.fxml")
        printTab.setContent("print-tab.fxml")
    }
}