module com.giapha {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.giapha to javafx.graphics, javafx.fxml;
    opens com.giapha.model to javafx.base;
    opens com.giapha.view to javafx.graphics, javafx.fxml;

    exports com.giapha;
    exports com.giapha.model;
    exports com.giapha.view;
    exports com.giapha.repository;
    exports com.giapha.database;
    exports com.giapha.service;
    exports com.giapha.controller;
    exports com.giapha.exception;
}
