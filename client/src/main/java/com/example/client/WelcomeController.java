package com.example.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;


public class WelcomeController {
    @FXML private TextField usernameField;
    @FXML private TextField sessionField;
    @FXML private HBox joinSessionBox;
    HttpHelper helper;
/*
    @FXML
*/
//    private void handleStartEditing() throws IOException {
//        String username = usernameField.getText().trim();
//        if (!username.isEmpty()) {
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/welcome.fxml"));
//            Parent root = loader.load();
//
//            EditorController controller = loader.getController();
//            //controller.initialize(username);
//
//            Stage stage = (Stage) usernameField.getScene().getWindow();
//            stage.setScene(new Scene(root, 800, 600));
//            stage.setTitle("Collaborative Editor - " + username);
//        }
//    }
@FXML
private void handleStartEditing() throws IOException {
    String username = usernameField.getText().trim();
    if (!username.isEmpty()) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/editor.fxml"));
        Parent root = loader.load();
        Map<String,String> sessionCodes= helper.createSession(usernameField.getText().trim());
        String editor=sessionCodes.get("editorCode");
        String viewer=sessionCodes.get("viewerCode");
        System.out.println(editor+ " "+viewer);
        EditorController controller = loader.getController();
        controller.initializeWithUsername(username, editor,viewer);
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.setScene(new Scene(root, 800, 600));
        stage.setTitle("Collaborative Editor - " + username);
    }
}

    @FXML
    private void handleJoinSession() {
        joinSessionBox.setVisible(true);
    }

    @FXML
    private void handleJoinSessionSubmit() throws IOException {
        String username = usernameField.getText().trim();
        String sessionCode = sessionField.getText().trim();
        if (!username.isEmpty() && !sessionCode.isEmpty()) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/editor.fxml"));
            Parent root = loader.load();
            Map<String,String> sessionCodes= helper.joinSession(sessionCode,username);
            String editor=sessionCodes.get("editorCode");
            String viewer=sessionCodes.get("viewerCode");
            EditorController controller = loader.getController();
            System.out.println(editor+ " "+viewer);
            controller.initializeWithUsername(username,editor,viewer); // Pass the username
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Collaborative Editor - " + username);
        }
    }
}
