package sk.zimen.semestralka.ui.state

import sk.zimen.semestralka.api.types.HeightPos
import sk.zimen.semestralka.api.types.QuadTreePlace
import sk.zimen.semestralka.api.types.WidthPos


class ParcelState private constructor() : AbstractState<QuadTreePlace>() {
    companion object {
        @Volatile
        private var instance: ParcelState? = null

        fun getInstance(): AbstractState<QuadTreePlace> {
            return instance
                ?: synchronized(this) {
                    instance ?: ParcelState().also { instance = it }
                }
        }
    }
}

class PropertyState private constructor() : AbstractState<QuadTreePlace>() {
    companion object {
        @Volatile
        private var instance: PropertyState? = null

        fun getInstance(): AbstractState<QuadTreePlace> {
            return instance
                ?: synchronized(this) {
                    instance ?: PropertyState().also { instance = it }
                }
        }
    }
}

abstract class AbstractState<T : QuadTreePlace> {
    var searchBar: SearchState? = null
        private set
    var editItem: T? = null
        private set

    fun newSearch(width: Double, widthPos: WidthPos, height: Double, heightPos: HeightPos) {
        searchBar = SearchState(width, widthPos, height, heightPos)
    }

    fun newEdit(item: T?) {
        editItem = item
    }
}

data class SearchState(
    var width: Double,
    var wPos: WidthPos,
    var height: Double,
    var hPos: HeightPos
)