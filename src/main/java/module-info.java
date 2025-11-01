module accounting.improved {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    // ملاحظة: قد تحتاج لحذف السطر التالي إذا لم تكن تستخدم مكتبة slf4j
    requires org.slf4j;
    requires org.slf4j.simple;
    requires java.logging;
    requires org.xerial.sqlitejdbc;

    // *** الأسطر الجديدة المطلوبة ***
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome;
    requires com.google.gson;
    requires com.zaxxer.hikari;
    // -----------------------------

    opens accounting.controller to javafx.fxml;
    opens accounting.gui to javafx.fxml, javafx.graphics;
    opens accounting.util to javafx.base;
    // *** سطر مهم جداً لربط النماذج بالواجهة ***
    opens accounting.model to javafx.base, javafx.fxml;

    exports accounting.gui;
    exports accounting.model;
}