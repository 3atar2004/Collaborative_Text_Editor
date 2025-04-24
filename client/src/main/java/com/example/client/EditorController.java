
package com.example.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/*public class EditorController implements Initializable {
    // FXML injections
    @FXML private Label titleLabel;
    @FXML private TextArea textArea;
    @FXML private VBox lineNumbers;
    @FXML private ScrollPane lineScrollPane;
    @FXML private ScrollPane textScrollPane;
    @FXML private HBox textEditorContainer;
    @FXML private TextField editorCodeField;
    @FXML private TextField viewerCodeField;
    @FXML private Button copyEditorButton;
    @FXML private Button copyViewerButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private ListView<String> userList;
    @FXML private Button endButton;

    //private UndoManager undoManager;
    private String username;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //undoManager = new UndoManager();
        setupTextArea();
        setupScrollSync();
    }

    public void setUsername(String username) {
        this.username = username;
        titleLabel.setText("Collaborative Editor - " + username);
        editorCodeField.setText("ABC123");
        viewerCodeField.setText("XYZ789");
        userList.getItems().add(username);

        setupUndoRedo();
        setupCopyButtons();
        setupEndButton();
    }

    private void setupTextArea() {
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            updateLineNumbers();
            if (!newText.equals(oldText)) {
                //undoManager.addEdit(oldText, newText);
            }
        });

        textArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> {
            updateLineNumbers();
        });
    }

    private void setupScrollSync() {
        textScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            lineScrollPane.setVvalue(newVal.doubleValue());
        });
    }

    private void updateLineNumbers() {
        String[] lines = textArea.getText().split("\n", -1);
        lineNumbers.getChildren().clear();
        for (int i = 0; i < lines.length; i++) {
            Label lineLabel = new Label(String.format("%d", i + 1));
            lineLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b8ca5;");
            lineLabel.setMinWidth(30);
            lineLabel.setAlignment(Pos.TOP_RIGHT);
            lineNumbers.getChildren().add(lineLabel);
        }
    }

    private void setupUndoRedo() {
////        undoButton.setOnAction(e -> undoManager.undo(textArea));
////        redoButton.setOnAction(e -> undoManager.redo(textArea));
//
//        textArea.getScene().getAccelerators().put(
//                new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN),
//                //() -> undoManager.undo(textArea)
//        );
//        textArea.getScene().getAccelerators().put(
//                new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN),
//                //() -> undoManager.redo(textArea)
//        );
    }

    private void setupCopyButtons() {
        setupCopyButton(copyEditorButton, editorCodeField.getText());
        setupCopyButton(copyViewerButton, viewerCodeField.getText());
    }

    private void setupCopyButton(Button button, String text) {
        Tooltip copiedTooltip = new Tooltip("Copied!");
        copiedTooltip.setStyle("""
            -fx-background-color: #4682b6;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-padding: 4;
        """);

        button.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);

            Point2D p = button.localToScreen(button.getWidth()/2, button.getHeight());
            copiedTooltip.show(button, p.getX(), p.getY());

//            new PauseTransition(Duration.seconds(1))
//                    .setOnFinished(event -> copiedTooltip.hide())
//                    .play();
        });
    }

    private void setupEndButton() {
        endButton.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/com/collaborative/editor/view/Welcome.fxml"));
                Stage stage = (Stage) endButton.getScene().getWindow();
                stage.setScene(new Scene(loader.load(), 800, 600));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }}



package com.example.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;*/

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
        // Default constructor needed for FXML loading
        this.roomCode = "ROOM" + System.currentTimeMillis(); // Default room code for Start Editing
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

    public void initializeWithUsername(String username) {
        this.username = username;
        initialize();
    }

    @FXML
    public void initialize() {
        titleLabel.setText("Room: " + roomCode);
        editorCodeField.setText(roomCode + "-editor");
        viewerCodeField.setText(roomCode + "-viewer");

        // Initialize user list with the username
        userList.getItems().add(username != null ? username : userId);

        // Sync line numbers with text area
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            handleTextChange(oldText, newText);
            updateLineNumbers();
        });

        // Sync scroll positions
        textScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            lineScrollPane.setVvalue(newVal.doubleValue());
        });

        // Connect to WebSocket
        connect();

        // Initial update
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
        CRDTOperation op = new CRDTOperation();
        op.setType("insert");
        op.setId(nodeId);
        op.setValue(value);
        op.setParentId(parentId);
        op.setroomId(roomCode);
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
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(editorCodeField.getText());
        clipboard.setContent(content);
    }

    @FXML
    public void copyViewerCode() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(viewerCodeField.getText());
        clipboard.setContent(content);
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

            WelcomeController controller = loader.getController();
            // No need to set stage since we're reusing the existing one

            Stage stage = (Stage) textArea.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Collaborative Text Editor");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateTextArea() {
        String text = document.getText();
        textArea.setText(text);
    }

    private void updateLineNumbers() {
        lineNumbers.getChildren().clear();
        int lineCount = textArea.getText().split("\n", -1).length;
        for (int i = 1; i <= lineCount; i++) {
            Label lineLabel = new Label(String.valueOf(i));
            lineLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px; -fx-text-fill: #666;");
            lineNumbers.getChildren().add(lineLabel);
        }
    }

    public Document getDocument() {
        return document;
    }

    public String getRoomCode() {
        return roomCode;
    }
}