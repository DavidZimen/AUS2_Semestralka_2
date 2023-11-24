module sk.zimen.semestralka {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires commons.math3;
    requires kotlin.reflect;
    requires kotlinx.coroutines.core.jvm;
    requires org.apache.commons.collections4;

    opens sk.zimen.semestralka to javafx.fxml;
    opens sk.zimen.semestralka.ui to javafx.fxml;
    exports sk.zimen.semestralka;
}