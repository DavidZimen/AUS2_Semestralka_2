module sk.zimen.semestralka {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;

    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires commons.math3;
    requires org.apache.commons.collections4;
    requires java.desktop;
    requires kotlin.reflect;
    requires kotlinx.coroutines.test;
    requires kotlinx.coroutines.core;
    requires kotlinx.coroutines.core.jvm;
    requires org.apache.commons.lang3;

    opens sk.zimen.semestralka to javafx.fxml;
    opens sk.zimen.semestralka.ui to javafx.fxml;
    opens sk.zimen.semestralka.ui.table_controllers to javafx.fxml;
    opens sk.zimen.semestralka.ui.add_edit_controllers to javafx.fxml;
    opens sk.zimen.semestralka.ui.generator_controllers to javafx.fxml;
    opens sk.zimen.semestralka.structures.trie.nodes to kotlin.reflect;
    opens sk.zimen.semestralka.structures.dynamic_hashing to kotlin.reflect;
    opens sk.zimen.semestralka.structures.dynamic_hashing.util to kotlin.reflect;
    opens sk.zimen.semestralka.structures.quadtree.interfaces to kotlin.reflect;
    opens sk.zimen.semestralka.structures.quadtree.node to kotlin.reflect;
    opens sk.zimen.semestralka.structures.quadtree to kotlin.reflect;
    opens sk.zimen.semestralka.api.types to kotlin.reflect, javafx.base;
    opens sk.zimen.semestralka.utils to kotlin.reflect;
    opens sk.zimen.semestralka.utils.generator to kotlin.reflect;
    opens sk.zimen.semestralka.utils.file to kotlin.reflect;
    exports sk.zimen.semestralka;
}