package sk.zimen.semestralka.ui.generator_controllers

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import sk.zimen.semestralka.api.service.ParcelService
import sk.zimen.semestralka.api.service.PropertyService
import sk.zimen.semestralka.ui.util.allowOnlyDouble
import sk.zimen.semestralka.ui.util.allowOnlyInt
import sk.zimen.semestralka.ui.util.disable
import sk.zimen.semestralka.ui.util.showSpinner
import java.net.URL
import java.util.*

class GeneratorController {

    private val operationType: String = "generation"
    
    private val parcelService = ParcelService.getInstance()

    private val propertyService = PropertyService.getInstance()

    // UI properties
    @FXML
    private lateinit var itemsCount: TextField
    @FXML
    private lateinit var quadWidth: TextField
    @FXML
    private lateinit var quadHeight: TextField
    @FXML
    private lateinit var treeDepth: TextField
    @FXML
    private lateinit var spinnerLabel: Label
    @FXML
    private lateinit var spinner: ProgressIndicator
    @FXML
    private lateinit var button: Button
    @FXML
    private lateinit var trieDepth: TextField
    @FXML
    private lateinit var overloadBlockFactor: TextField
    @FXML
    private lateinit var mainBlockFactor: TextField

    fun initialize(p0: URL?, p1: ResourceBundle?) {
        spinner.showSpinner(false, spinnerLabel)
        quadWidth.allowOnlyDouble()
        quadHeight.allowOnlyDouble()
        treeDepth.allowOnlyInt()
        trieDepth.allowOnlyInt()
        overloadBlockFactor.allowOnlyInt()
        mainBlockFactor.allowOnlyInt()
        itemsCount.allowOnlyInt()
    }

    fun showSuccessAlert() {
        val alert = Alert(Alert.AlertType.NONE, "" , ButtonType.OK)
        alert.isResizable = false
        alert.headerText = "Parcels and properties $operationType was successful."
        alert.showAndWait()
    }

    fun showErrorAlert() {
        val alert = Alert(Alert.AlertType.ERROR, "" , ButtonType.OK)
        alert.isResizable = false
        alert.headerText = "Parcels and properties $operationType failed, please try again later."
        alert.showAndWait()
    }

    fun disableAll(disable: Boolean = true) {
        spinner.showSpinner(disable, spinnerLabel)
        button.disable(disable)
        itemsCount.disable(disable)
        disableForm(disable)
    }

    private fun disableForm(disable: Boolean = true) {
        quadWidth.disable(disable)
        quadHeight.disable(disable)
        treeDepth.disable(disable)
        trieDepth.disable(disable)
        overloadBlockFactor.disable(disable)
        mainBlockFactor.disable(disable)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun onGenerate() {
        val width = quadWidth.text.toDouble()
        val height = quadHeight.text.toDouble()
        val depth = treeDepth.text.toInt()
        val count = itemsCount.text.toInt()
        val mainBf = mainBlockFactor.text.toInt()
        val overloadBf = overloadBlockFactor.text.toInt()
        val trieMaxDepth = trieDepth.text.toInt()

        println(overloadBf)

        // Show loader on screen
        disableAll(true)

        //show loader on screen
        GlobalScope.launch {
            try {
                parcelService.generateData(count, depth, -width, height, width, -height, mainBf, overloadBf, trieMaxDepth)
                propertyService.generateData(count, depth, -width, height, width, -height, mainBf, overloadBf, trieMaxDepth)

                // Once the background tasks are done, update the UI on the JavaFX application thread
                Platform.runLater {
                    disableAll(false)
                    showSuccessAlert()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Update the UI to indicate an error
                Platform.runLater {
                    disableAll(false)
                    showErrorAlert()
                }
            }
        }
    }
}
