package com.example.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.io.IOException;

public class EditorController {
    private String roomCode;
    private String username;
    private final String userId = "User" + System.currentTimeMillis();
    private final DocumentWebsockethandler webSocketHandler;
    private final Document document;
    private Stage stage;

    // FXML fields
    @FXML private Label titleLabel;
    @FXML private TextField editorCodeField;
    @FXML private TextField viewerCodeField;
    @FXML private Button copyEditorButton;
    @FXML private Button copyViewerButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private Button endButton;
    @FXML private ListView<String> userList;
    @FXML private VBox lineNumbers;
    @FXML private ScrollPane lineScrollPane;
    @FXML private ScrollPane textScrollPane;
    @FXML private TextArea textArea;

    public EditorController() {
        this.roomCode = "ROOM" + System.currentTimeMillis(); // Default room code
        this.webSocketHandler = new DocumentWebsockethandler();
        this.document = new Document();
    }

    public EditorController(String roomCode) {
        this.roomCode = roomCode;
        this.webSocketHandler = new DocumentWebsockethandler();
        this.document = new Document();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void initializeWithUsername(String username, String editorCode, String viewerCode,String SERVER_IP) {
        this.username = username;
        if (editorCodeField != null && viewerCodeField != null) {
            editorCodeField.setText(editorCode);
            viewerCodeField.setText(viewerCode);
        }
        if (userList != null) {
            userList.getItems().add(username);
        }
        webSocketHandler.IP=SERVER_IP;
        initialize();
    }

    @FXML
    public void initialize() {
        if (titleLabel != null) {
            titleLabel.setText("Room: " + (roomCode != null ? roomCode : "Unknown"));
        }

        if (editorCodeField != null && (editorCodeField.getText() == null || editorCodeField.getText().isEmpty())) {
            editorCodeField.setText(roomCode + "-editor");
        }

        if (viewerCodeField != null && (viewerCodeField.getText() == null || viewerCodeField.getText().isEmpty())) {
            viewerCodeField.setText(roomCode + "-viewer");
        }

        if (textArea != null) {
            textArea.textProperty().addListener((obs, oldText, newText) -> {
                handleTextChange(oldText, newText);
                updateLineNumbers();
            });
        }

        if (textScrollPane != null && lineScrollPane != null) {
            textScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
                lineScrollPane.setVvalue(newVal.doubleValue());
            });
        }

        connect();
        updateTextArea();
        updateLineNumbers();
    }

    public boolean connect() {
        boolean connected = webSocketHandler.connectToWebSocket();
        if (connected) {
            webSocketHandler.subscribeToRoom(roomCode);
            webSocketHandler.setMessageHandler(this::handleWebSocketMessage);
        }
        return connected;
    }

    private void handleWebSocketMessage(CRDTOperation op) {
        Platform.runLater(() -> {
            if ("insert".equals(op.getType())) {
                document.remoteInsert(op.getId(), op.getValue(), op.getParentId());
            } else if ("delete".equals(op.getType())) {
                document.remoteDelete(op.getId());
            }
            updateTextArea();
            updateLineNumbers();
        });
    }

    private void handleTextChange(String oldText, String newText) {
        if (newText.length() > oldText.length()) {
            char insertedChar = newText.charAt(newText.length() - 1);
            String parentId = newText.length() == 1 ? null : document.getRootId();
            localInsert(insertedChar, parentId);
        } else if (newText.length() < oldText.length()) {
            localDelete();
        }
    }

    public void localInsert(char value, String parentId) {
        String nodeId = userId + ":" + System.currentTimeMillis();
        document.insert(value, parentId, userId);
        webSocketHandler.sendInsert(roomCode, nodeId, value, parentId);
    }

    public void localDelete() {
        document.undo(userId);
        updateTextArea();
    }

    @FXML
    public void undo() {
        document.undo(userId);
        updateTextArea();
        updateLineNumbers();
    }

    @FXML
    public void redo() {
        document.redo(userId);
        updateTextArea();
        updateLineNumbers();
    }

    @FXML
    public void copyEditorCode() {
        if (editorCodeField != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(editorCodeField.getText());
            clipboard.setContent(content);
        }
    }

    @FXML
    public void copyViewerCode() {
        if (viewerCodeField != null) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(viewerCodeField.getText());
            clipboard.setContent(content);
        }
    }

    @FXML
    public void handleImport() {
        System.out.println("Import functionality not implemented.");
    }

    @FXML
    public void handleExport() {
        System.out.println("Export functionality not implemented.");
    }

    @FXML
    public void endSession() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/client/Welcome.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) textArea.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Collaborative Text Editor");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTextArea() {
        if (textArea != null) {
            String text = document.getText();
            textArea.setText(text);
        }
    }

    private void updateLineNumbers() {
        if (lineNumbers != null && textArea != null) {
            lineNumbers.getChildren().clear();
            int lineCount = textArea.getText().split("\n", -1).length;
            for (int i = 1; i <= lineCount; i++) {
                Label lineLabel = new Label(String.valueOf(i));
                lineLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-text-fill: #666;");
                lineNumbers.getChildren().add(lineLabel);
            }
        }
    }

    public Document getDocument() {
        return document;
    }

    public String getRoomCode() {
        return roomCode;
    }
}
