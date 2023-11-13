package sk.zimen.semestralka

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Aus2Semestralka2 : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(Aus2Semestralka2::class.java.getResource("main-page.fxml"))
        val scene = Scene(fxmlLoader.load(), 320.0, 240.0)
        stage.title = "Bla!"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(Aus2Semestralka2::class.java)
}