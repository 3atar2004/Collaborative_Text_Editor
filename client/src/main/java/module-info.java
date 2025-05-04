module com.example.client {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires spring.messaging;
    requires spring.websocket;
    requires spring.core;
    requires spring.web;
    requires java.desktop;
    requires com.fasterxml.jackson.annotation;

    opens com.example.client to javafx.fxml;
    exports com.example.client;
}