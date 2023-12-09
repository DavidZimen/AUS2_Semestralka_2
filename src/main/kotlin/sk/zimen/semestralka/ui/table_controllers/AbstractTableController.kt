package sk.zimen.semestralka.ui.table_controllers

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import sk.zimen.semestralka.Aus2Semestralka2
import sk.zimen.semestralka.api.types.HeightPos
import sk.zimen.semestralka.api.types.QuadTreePlace
import sk.zimen.semestralka.api.types.WidthPos
import sk.zimen.semestralka.ui.state.AbstractState
import sk.zimen.semestralka.ui.util.allowOnlyDouble
import java.net.URL
import java.util.*

abstract class AbstractTableController : Initializable {

    protected var tableItems = FXCollections.observableArrayList<QuadTreePlace>()
    protected lateinit var state: AbstractState<QuadTreePlace>
    @FXML
    protected lateinit var borderPane: BorderPane
    @FXML
    protected var deleteButton: Button? = null
    @FXML
    protected var editButton: Button? = null
    @FXML
    protected lateinit var width: TextField
    @FXML
    protected lateinit var height: TextField
    @FXML
    protected lateinit var zPos: RadioButton
    @FXML
    protected lateinit var sPos: RadioButton
    @FXML
    protected lateinit var vPos: RadioButton
    @FXML
    protected lateinit var jPos: RadioButton
    @FXML
    protected lateinit var table: TableView<QuadTreePlace>
    @FXML
    protected lateinit var bottomHeightPos: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var bottomHeightValue: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var key: TableColumn<QuadTreePlace, Double>
    @FXML
    protected lateinit var topWidthValue: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var topWidthPos: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var topHeightValue: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var topHeightPos: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var bottomWidthValue: TableColumn<QuadTreePlace, String>
    @FXML
    protected lateinit var bottomWidthPos: TableColumn<QuadTreePlace, String>

    abstract fun search()

    abstract fun onEdit()

    abstract fun onAdd()

    abstract fun loadAll()

    abstract fun deleteFromService(item: QuadTreePlace)

    open fun initState() {
        state.searchBar?.let {
            width.text = it.width.toString()
            zPos.isSelected = it.wPos == WidthPos.Z
            vPos.isSelected = it.wPos == WidthPos.V
            height.text = it.height.toString()
            sPos.isSelected = it.hPos == HeightPos.S
            jPos.isSelected = it.hPos == HeightPos.J
            search()
        }
    }

    fun onDelete() {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.isResizable = false
        alert.title = "Delete"
        alert.headerText = "Selected item will be permanently deleted. Are you sure you want to continue ?"
        alert.showAndWait()
        if (alert.result == ButtonType.OK) {
            val delProperty = table.selectionModel.selectedItem
            deleteFromService(delProperty)
            table.items.remove(delProperty)
        } else {
            alert.close()
        }
    }

    fun newSearchState() {
        state.newSearch(
            width.text.toDouble(),
            if (zPos.isSelected) WidthPos.Z else WidthPos.V,
            height.text.toDouble(),
            if (sPos.isSelected) HeightPos.S else HeightPos.J
        )
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        table.selectionModel.selectionMode = SelectionMode.SINGLE
        key.cellValueFactory = PropertyValueFactory("key")
        topWidthValue.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.topLeft.width.toString()) }
        topWidthPos.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.topLeft.widthPosition.toString()) }
        topHeightValue.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.topLeft.height.toString()) }
        topHeightPos.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.topLeft.heightPosition.toString()) }
        bottomWidthValue.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bottomRight.width.toString()) }
        bottomWidthPos.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bottomRight.widthPosition.toString()) }
        bottomHeightValue.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bottomRight.height.toString()) }
        bottomHeightPos.setCellValueFactory { cellData -> SimpleStringProperty(cellData.value.bottomRight.heightPosition.toString()) }
        deleteButton?.isVisible = false
        editButton?.isVisible = false
        width.allowOnlyDouble()
        height.allowOnlyDouble()

        //initialize visibility of delete and edit button
        table.selectionModel.selectedItemProperty().addListener { _, _, newSelection ->
            deleteButton?.isVisible = newSelection != null
            editButton?.isVisible = newSelection != null
        }

        initState()
    }

    protected fun navigate(path: String) {
        borderPane.children.setAll(BorderPane(FXMLLoader(Aus2Semestralka2::class.java.getResource(path)).load()))
    }
}