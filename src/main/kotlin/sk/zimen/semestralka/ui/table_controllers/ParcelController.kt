package sk.zimen.semestralka.ui.table_controllers

import javafx.collections.FXCollections
import javafx.fxml.Initializable
import sk.zimen.semestralka.api.service.ParcelService
import sk.zimen.semestralka.api.types.GpsPosition
import sk.zimen.semestralka.api.types.QuadTreePlace
import sk.zimen.semestralka.ui.state.ParcelState

class ParcelController : Initializable, AbstractTableController() {

    private val parcelService = ParcelService.getInstance()

    override fun search() {
        newSearchState()
        tableItems = state.searchBar?.let { searchState ->
            with(searchState) {
                FXCollections.observableArrayList(
                    parcelService.find(GpsPosition(width, wPos, height, hPos))
                )
            }
        }

        table.items = tableItems
    }

    override fun onEdit() {
        state.newEdit(table.selectionModel.selectedItem)
        navigate("add-edit-parcel.fxml")
    }

    override fun onAdd() {
        state.newEdit(null)
        navigate("add-edit-parcel.fxml")
    }

    override fun loadAll() {
        tableItems = FXCollections.observableArrayList(parcelService.all())
        table.items = tableItems
    }

    override fun initState() {
        state = ParcelState.getInstance()
        super.initState()
    }

    override fun deleteFromService(item: QuadTreePlace) {
        parcelService.delete(item)
    }
}