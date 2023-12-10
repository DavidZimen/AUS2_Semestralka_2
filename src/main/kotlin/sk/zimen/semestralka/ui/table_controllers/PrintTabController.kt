package sk.zimen.semestralka.ui.table_controllers

import javafx.fxml.FXML
import javafx.scene.control.TextArea
import sk.zimen.semestralka.api.service.ParcelService
import sk.zimen.semestralka.api.service.PropertyService
import sk.zimen.semestralka.ui.state.WindowState

class PrintTabController {

    @FXML
    private lateinit var mainFileArea: TextArea
    @FXML
    private lateinit var overloadFileArea: TextArea

    fun printStructure() {
        val state = WindowState.getInstance()

        val printData = if (state.isParcel) {
            ParcelService.getInstance().hashStructurePrint()
        } else {
            PropertyService.getInstance().hashStructurePrint()
        }

        mainFileArea.text = printData.mainStructure
        overloadFileArea.text = printData.overloadStructure
    }

    fun clearAreas() {
        mainFileArea.text = ""
        overloadFileArea.text = ""
    }
}