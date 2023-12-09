package sk.zimen.semestralka.ui.add_edit_controllers

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.TextField
import sk.zimen.semestralka.api.service.PropertyService
import sk.zimen.semestralka.api.types.Property
import sk.zimen.semestralka.ui.state.PropertyState

class AddEditPropertyController : AbstractAddEditController<Property>() {

    private val propertyService = PropertyService.getInstance()

    @FXML
    private lateinit var number: TextField

    override fun onSave() {
        if (editBefore == null) {
            try {
                propertyService.add(
                    Property(number.text.toInt(), desc.text, getGpsPosition(true), getGpsPosition(false))
                )
                showSuccessAlert(true)
            } catch (e: Exception) {
                showErrorAlert(true)
            }
        } else {
            try {
                propertyService.edit(
                    editBefore!!,
                    Property(number.text.toInt(), desc.text, getGpsPosition(true), getGpsPosition(false)).apply {
                        key = this@AddEditPropertyController.key.text.toLong()
                    }
                )
                showSuccessAlert(false)
            } catch (e: Exception) {
                showErrorAlert(false)
            }
        }
    }

    override fun onCancel() {
        navigate("properties.fxml")
    }

    override fun initState() {
        state = PropertyState.getInstance()
    }

    override fun init() {
        val key = state.editItem?.key ?: return

        editBefore = propertyService.find(key)
        editBefore?.let {
            associatedItems = FXCollections.observableArrayList(editBefore!!.parcelsForProperty)
            number.text = editBefore!!.number.toString()
        }
    }
}