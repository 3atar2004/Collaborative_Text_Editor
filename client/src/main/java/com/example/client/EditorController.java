//package com.example.client;
//
//import javafx.application.Platform;
//import javafx.event.ActionEvent;
//import javafx.fxml.FXML;
//import javafx.scene.Node;
//import javafx.scene.control.*;
//import javafx.scene.input.Clipboard;
//import javafx.scene.input.ClipboardContent;
//import javafx.scene.layout.VBox;
//import javafx.scene.paint.Color;
//import javafx.stage.FileChooser;
//import javafx.stage.Stage;
//import javafx.stage.Window;
//import org.springframework.core.io.FileSystemResource;
//import javafx.geometry.Pos;
//import javafx.scene.input.KeyCode;
//import javafx.scene.input.KeyCodeCombination;
//import javafx.scene.input.KeyCombination;
//import org.w3c.dom.events.EventTarget;
//
//import javax.print.Doc;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class EditorController {
//    @FXML
//    public Label edit_label;
//    public Label view_label;
//    @FXML
//    private Label titleLabel;
//
//    @FXML
//    private TextField editorCodeField;
//
//    @FXML
//    private TextField viewerCodeField;
//
//    @FXML
//    private Button copyEditorButton;
//
//    @FXML
//    private Button copyViewerButton;
//
//    @FXML
//    private Button undoButton;
//
//    @FXML
//    private Button redoButton;
//
//    @FXML
//    private Button endButton;
//
//    @FXML
//    private ListView<String> userList;
//
//    @FXML
//    private TextArea textArea;
//
//    @FXML
//    private VBox lineNumbers;
//
//    @FXML
//    private ScrollPane lineScrollPane;
//
//    @FXML
//    private ScrollPane textScrollPane;
//
//    private List<String> user_list = new ArrayList<>();
//    private DocumentWebsockethandler websockethandler;
//    private Userlistwebsockethandler userlistwebsockethandler;
//    private CursorWebSocketHandler cursorWebSocketHandler;
//    private Document document;
//    private String UserID;
//    private String roomCode; // Mock room code
//    private Stack<String> undoStack = new Stack<>();
//    private Stack<String> redoStack = new Stack<>();
//    private boolean isProcessingChange = false; // Debounce flag
//    private Set<String> sentOperationIds = new HashSet<>(); // Track sent operations
//    private Set<String> deletedids = new HashSet<>();
//    private boolean Remoteupdate = false;
//    private boolean newdoc=false;
//
//    HttpHelper helper;
//
//
//    @FXML
//    public void initialize() {
//        titleLabel.setText("Text Editor");
//        document = new Document();
//        websockethandler = new DocumentWebsockethandler();
//        websockethandler.setMessageHandler(this::handleServerOperation);
//        userlistwebsockethandler= new Userlistwebsockethandler();
//        userlistwebsockethandler.setUsershandler(this::handleuserlist);
//        cursorWebSocketHandler= new CursorWebSocketHandler();
//        cursorWebSocketHandler.setCursorHandler(this::handlecursors);
//        helper = new HttpHelper();
//
//        // Debounce text change listener
//        textArea.textProperty().addListener((obs, oldValue, newValue) -> {
//            if (Remoteupdate) {
//                return; // it just skips.
//            }
//            if (!isProcessingChange) {
//                isProcessingChange = true;
//                handleLocalTextChange(oldValue, newValue);
//                isProcessingChange = false;
//            }
//        });
//
//
//        // Sync scrolling of line numbers with text area
//        textScrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
//            lineScrollPane.setVvalue(newValue.doubleValue());
//        });
//        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
//            cursorWebSocketHandler.sendCursorPosition(roomCode,UserID, (Integer) newPos,"RED");
//        });
//
//        // Initialize line numbers
//        updateLineNumbers();
//
//        // Initialize buttons and fields
//        editorCodeField.setText(generateEditorCode());
//        viewerCodeField.setText(generateViewerCode());
//        updateUndoRedoButtons();
//    }
//
//    private void handlecursors(String username, Integer position, String color) {
//        System.out.println("Recieved cursor from "+username+"at position "+position+"with color: "+color);
//    }
//
//
//    public void initializeWithUsername(String username, String editor, String viewer, boolean edit) throws IOException, InterruptedException {
//        if (username != null && !username.trim().isEmpty()) {
//            userList.getItems().add(username);
//        }
//        this.viewerCodeField.setText(viewer);
//        this.editorCodeField.setText(editor);
//        user_list.add(username);
//
//        if (edit == false) {
//            editorCodeField.setVisible(false);
//            viewerCodeField.setVisible(false);
//            textArea.setEditable(false);
//            copyEditorButton.setVisible(false);
//            copyViewerButton.setVisible(false);
//            edit_label.setVisible(false);
//            view_label.setVisible(false);
//        }
//        this.UserID = username;
//        roomCode = editor;
//        if (roomCode == null || roomCode.trim().isEmpty()) {
//            showAlert("Invalid Room Code", "Room code is not set.");
//            return;
//        }
//
//        // Connect to WebSocket first
//        if (websockethandler.connectToWebSocket()) {
//            websockethandler.subscribeToRoom(roomCode);
//        } else {
//            showAlert("Connection Failed", "Couldn't connect to WebSocket server");
//        }
//
//        if (userlistwebsockethandler.connectToWebSocket()) {
//            userlistwebsockethandler.subscribeToRoom(roomCode);
//        } else {
//            showAlert("Connection Failed", "Couldn't connect to WebSocket server");
//        }
//        if(cursorWebSocketHandler.connectToWebSocket())
//        {
//            cursorWebSocketHandler.subscribeToCursorUpdates(roomCode);
//        }
//        else
//        {
//            showAlert("connection failed","can't connect to websocket server");
//        }
//
//        // Fetch the document from server
//        Document doc2 = helper.getDocumentFromCode(roomCode);
//        System.out.println("doc received: " + (doc2 != null ? doc2.getText() : "null"));
//
//        if (doc2 != null) {
//            // Replace the local document with the server document
//            this.document = doc2; // This maintains all node IDs and structure
//
//            // Update the UI
//
//            Platform.runLater(() -> {
//                String text = document.getText();
//                newdoc=true;
//                textArea.setText(text);
//                newdoc=false;
//                updateLineNumbers();
//                System.out.println("Document loaded with text: " + text);
//
//                // Print all nodes for verification
//                System.out.println("Document nodes:");
//                document.getNodes().forEach((id, node) -> {
//                    System.out.println("ID: " + id +
//                            ", Value: " + node.getValue() +
//                            ", Parent: " + node.getParentId() +
//                            ", Deleted: " + node.isDeleted());
//                });
//            });
//        } else {
//            System.out.println("Failed to fetch document from server");
//        }
//
//
//        userlistwebsockethandler.join(roomCode,username);
//    }
package com.example.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.core.io.FileSystemResource;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.w3c.dom.events.EventTarget;

import javax.print.Doc;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EditorController {
    @FXML
    public Label edit_label;
    public Label view_label;
    @FXML
    private Label titleLabel;

    @FXML
    private TextField editorCodeField;

    @FXML
    private TextField viewerCodeField;

    @FXML
    private Button copyEditorButton;

    @FXML
    private Button copyViewerButton;

    @FXML
    private Button undoButton;

    @FXML
    private Button redoButton;

    @FXML
    private Button endButton;

    @FXML
    private ListView<String> userList;

    @FXML
    private TextArea textArea;

    @FXML
    private VBox lineNumbers;

    @FXML
    private ScrollPane lineScrollPane;

    @FXML
    private ScrollPane textScrollPane;

    @FXML
    private Pane cursorLayer; // Added for remote cursors

    private List<String> user_list = new ArrayList<>();
    private DocumentWebsockethandler websockethandler;
    private Userlistwebsockethandler userlistwebsockethandler;
    private CursorWebSocketHandler cursorWebSocketHandler;
    private Document document;
    private String UserID;
    private String roomCode; // Mock room code
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isProcessingChange = false; // Debounce flag
    private Set<String> sentOperationIds = new HashSet<>(); // Track sent operations
    private Set<String> deletedids = new HashSet<>();
    private boolean Remoteupdate = false;
    private boolean newdoc = false;
    private int lastSentPosition = -1;
    private long lastCursorUpdateTime = 0;
    private static final long CURSOR_UPDATE_DELAY_MS =100;

    // For remote cursors
    private final Map<String, RemoteCursor> remoteCursors = new HashMap<>();
    private static final String[] USER_COLORS = {
            "#FF0000", "#00FF00", "#0000FF",
            "#FF00FF", "#FFFF00", "#00FFFF"
    };

    HttpHelper helper;

    @FXML
    public void initialize() {
        titleLabel.setText("Text Editor");
        editorCodeField.setText(generateEditorCode());
        viewerCodeField.setText(generateViewerCode());
        updateUndoRedoButtons();
        document = new Document();
        websockethandler = new DocumentWebsockethandler();
        websockethandler.setMessageHandler(this::handleServerOperation);
        userlistwebsockethandler = new Userlistwebsockethandler();
        userlistwebsockethandler.setUsershandler(this::handleuserlist);
        cursorWebSocketHandler = new CursorWebSocketHandler();
        cursorWebSocketHandler.setCursorHandler(this::handlecursors);
        helper = new HttpHelper();

        // Debounce text change listener
        textArea.textProperty().addListener((obs, oldValue, newValue) -> {
            if (Remoteupdate) {
                return; // it just skips.
            }
            if (!isProcessingChange) {
                isProcessingChange = true;
                handleLocalTextChange(oldValue, newValue);
                isProcessingChange = false;
            }
        });

        // Sync scrolling of line numbers with text area
        textScrollPane.vvalueProperty().addListener((obs, oldValue, newValue) -> {
            lineScrollPane.setVvalue(newValue.doubleValue());
        });

        // Track local cursor position
        textArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {

            cursorWebSocketHandler.sendCursorPosition(roomCode, UserID, (Integer) newPos, getColorForUser(UserID));
        });

        // Initialize line numbers
        updateLineNumbers();

        // Initialize buttons and fields

    }

    private void handlecursors(String userId, Integer position, String color) {
        System.out.println("Recieved cursor from: "+userId+" with position "+position+ "and color: "+color);
        Platform.runLater(() -> {
            if (userId.equals(UserID)) return; // Skip own cursor

            String username = userList.getItems().stream()
                    .filter(u -> u.equals(userId))
                    .findFirst()
                    .orElse(userId);

            updateRemoteCursor(userId, username, position, color);
        });
    }

    private void updateRemoteCursor(String userId, String username, int position, String color) {
        if (cursorLayer == null) {
            System.err.println("Cursor layer not initialized!");
            return;
        }

        try {
            RemoteCursor cursor = remoteCursors.computeIfAbsent(userId, id ->
                    new RemoteCursor(username, color != null ? color : getColorForUser(id)));

            Point2D pos = calculateCursorPosition(position);
            cursor.updatePosition(pos.getX(), pos.getY());
        } catch (Exception e) {
            System.err.println("Error updating cursor: " + e.getMessage());
}
    }

    private Point2D calculateCursorPosition(int caretPosition) {
        String text = textArea.getText().substring(0, caretPosition);
        String[] lines = text.split("\n", -1);
        int lineNumber = lines.length - 1;
        String lastLine = lines[lineNumber];

        // Create a temporary Text object for measurement
        Text helper = new Text(lastLine);
        helper.setFont(textArea.getFont());

        // Calculate position
        double lineHeight = textArea.getFont().getSize() * 1.2;
        double yPos = lineHeight * (lineNumber + 0.5) + 5; // Add small offset
        double xPos = helper.getLayoutBounds().getWidth() + 10; // Add small offset

        return new Point2D(xPos, yPos);
    }

    private String getColorForUser(String userId) {
        int hash = userId.hashCode();
        return USER_COLORS[Math.abs(hash) % USER_COLORS.length];
    }

    private class RemoteCursor {
        private final Line line;
        private final Text label;
        private final String color;

        public RemoteCursor(String username, String color) {
            this.color = color;
            this.line = new Line(0, 0, 0, 20);
            this.line.setStroke(Color.web(color));
            this.line.setStrokeWidth(2);

            this.label = new Text(username);
            this.label.setStyle("-fx-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
            this.label.setTranslateY(-15);

            Group cursorGroup = new Group(line, label);
            cursorLayer.getChildren().add(cursorGroup);
        }

        public void updatePosition(double x, double y) {
            line.setStartX(x);
            line.setStartY(y);
            line.setEndX(x);
            line.setEndY(y + 20);

            label.setTranslateX(x - (label.getLayoutBounds().getWidth() / 2));
            label.setTranslateY(y - 15);
        }
    }

    public void initializeWithUsername(String username, String editor, String viewer, boolean edit) throws IOException, InterruptedException {
        if (username != null && !username.trim().isEmpty()) {
            userList.getItems().add(username);
        }
        this.viewerCodeField.setText(viewer);
        this.editorCodeField.setText(editor);
        user_list.add(username);

        if (edit == false) {
            editorCodeField.setVisible(false);
            viewerCodeField.setVisible(false);
            textArea.setEditable(false);
            copyEditorButton.setVisible(false);
            copyViewerButton.setVisible(false);
            edit_label.setVisible(false);
            view_label.setVisible(false);
        }
        this.UserID = username;
        roomCode = editor;
        if (roomCode == null || roomCode.trim().isEmpty()) {
            showAlert("Invalid Room Code", "Room code is not set.");
            return;
        }

        // Connect to WebSocket first
        if (websockethandler.connectToWebSocket()) {
            websockethandler.subscribeToRoom(roomCode);
        } else {
            showAlert("Connection Failed", "Couldn't connect to WebSocket server");
        }

        if (userlistwebsockethandler.connectToWebSocket()) {
            userlistwebsockethandler.subscribeToRoom(roomCode);
        } else {
            showAlert("Connection Failed", "Couldn't connect to WebSocket server");
        }
        if (cursorWebSocketHandler.connectToWebSocket()) {
            cursorWebSocketHandler.subscribeToCursorUpdates(roomCode);
        } else {
            showAlert("connection failed", "can't connect to websocket server");
        }

        // Fetch the document from server
        Document doc2 = helper.getDocumentFromCode(roomCode);
        System.out.println("doc received: " + (doc2 != null ? doc2.getText() : "null"));

        if (doc2 != null) {
            // Replace the local document with the server document
            this.document = doc2; // This maintains all node IDs and structure

            // Update the UI
            Platform.runLater(() -> {
                String text = document.getText();
                newdoc = true;
                textArea.setText(text);
                newdoc = false;
                updateLineNumbers();
                System.out.println("Document loaded with text: " + text);

                // Print all nodes for verification
                System.out.println("Document nodes:");
                document.getNodes().forEach((id, node) -> {
                    System.out.println("ID: " + id +
                            ", Value: " + node.getValue() +
                            ", Parent: " + node.getParentId() +
                            ", Deleted: " + node.isDeleted());
                });
            });
        } else {
            System.out.println("Failed to fetch document from server");
        }

        userlistwebsockethandler.join(roomCode, username);
}

    @FXML
    private void handleImport(ActionEvent event) throws IOException {
//        textArea.setText("Imported text content...");
//        undoStack.push("");
//        redoStack.clear();
//        updateUndoRedoButtons();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to import");
        Window window = ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File selectedfile = fileChooser.showOpenDialog(window);
        if (selectedfile != null) {
            String filepath = selectedfile.getAbsolutePath();
            String text = Files.readString(Paths.get(filepath));
            textArea.setText(text);
        } else {
            System.out.println("no file is selected! ");
        }
    }

    @FXML
    private void handleExport(ActionEvent event) throws IOException {
        String content = textArea.getText();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as ....");
        Window window = ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        File fileselected = fileChooser.showSaveDialog(window);
        if (fileselected != null) {
            String filepath = fileselected.getAbsolutePath();
            ExportFile(content, filepath);
            showSuccess("File Saved","File Exported Successfully");

        }
    }

    public void ExportFile(String content, String filePath) throws IOException {
        // Convert to Path to handle extensions and directories cleanly
        Path path = Paths.get(filePath);

        // Add .txt extension if missing
        if (!filePath.toLowerCase().endsWith(".txt")) {
            path = path.resolveSibling(path.getFileName() + ".txt");
        }

        File file = path.toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

    }

    private void  handleuserlist(List<String>usernames)
    {
        System.out.println("recieved userlist");
        for(String user: usernames)
        {
            System.out.println(user);
        }
        //userList.getItems().removeLast();
        Platform.runLater(() -> {
//            for (String username : usernames ) {
////                if(username.equals(username)&& user_list.contains(username))
////                {
////                    continue;
////                }
//
//                userList.getItems().add(username);
//            }
            userList.getItems().setAll(usernames);
        });
    }

    @FXML
    private void copyEditorCode(ActionEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(editorCodeField.getText());
        clipboard.setContent(content);
    }

    @FXML
    private void copyViewerCode(ActionEvent event) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(viewerCodeField.getText());
        clipboard.setContent(content);
    }

    @FXML
    private void undo(ActionEvent event) {
        document.undo(UserID);
        textArea.setText(document.getText());
        updateUndoRedoButtons();
    }

    @FXML
    private void redo(ActionEvent event) {
        document.redo(UserID);
        textArea.setText(document.getText());
        updateUndoRedoButtons();
    }

    @FXML
    private void endSession(ActionEvent event) {
        textArea.clear();
        userList.getItems().clear();
        userlistwebsockethandler.leave(roomCode,UserID);
        //userlistwebsockethandler.disconnect();
        websockethandler.disconnect();
        updateUndoRedoButtons();
    }

    private void handleLocalTextChange(String oldValue, String newValue) {
        if(!newdoc) {
            if (oldValue == null || newValue == null || oldValue.equals(newValue)) return;


            int cursorPosition = textArea.getCaretPosition();
            System.out.println("oldValue: '" + oldValue + "', newValue: '" + newValue + "', cursorPosition: " + cursorPosition);

            if (newValue.length() > oldValue.length()) { // Character inserted
                if (cursorPosition >= 0 && cursorPosition <= newValue.length()) {
                    char insertedChar = 0;
                    int insertIndex = cursorPosition > 0 ? cursorPosition - 1 : 0; // get position of the inserted character
                    if (newValue.length() == oldValue.length() + 1) {
                        // Single character insertion
                        if (insertIndex < oldValue.length() && oldValue.charAt(insertIndex) == newValue.charAt(insertIndex)) {
                            insertedChar = cursorPosition < newValue.length() ? newValue.charAt(cursorPosition) : newValue.charAt(newValue.length() - 1);
                        } else {
                            insertedChar = newValue.charAt(insertIndex);
                        }
                    } else {
                        // here we will handle lama text yeb2a pasted. // multiple character insertion at once.
                        // Multiple character insertion = paste
                        int insertedLength = newValue.length() - oldValue.length();
                        int startIndex = insertIndex;

// Extract inserted substring
                        String insertedText = newValue.substring(startIndex, startIndex + insertedLength);
                        System.out.println("Pasted text: '" + insertedText + "'");

// Get initial parentId (before the paste position)
                        String parentId = null;
                        if (startIndex > 0) {
                            parentId = document.getNodeIdAtPosition(startIndex - 1);
                        }
                        if (parentId == null) {
                            parentId = document.getLastNodeId();
                        }

                        for (int i = 0; i < insertedText.length(); i++) {
                            char c = insertedText.charAt(i);
                            String nodeId = document.insert(c, parentId, UserID);
                            System.out.println("Inserted char from paste: " + c + ", nodeId: " + nodeId + ", parentId: " + parentId);

                            if (nodeId != null && sentOperationIds.add(nodeId)) {
                                websockethandler.sendInsert(roomCode, nodeId, c, parentId);
                            }

                            // Set parentId for the next character to be this node
                            parentId = nodeId;
                        }
                    }

                    // Determine parentId
                    String parentId = null;
                    if (cursorPosition > 0) {
                        parentId = document.getNodeIdAtPosition(cursorPosition - 1);
                    }
                    if (parentId == null) {
                        parentId = document.getLastNodeId();
                    }

                    System.out.println("Inserting char: " + insertedChar + ", parentId: " + parentId);
                    String nodeId = document.insert(insertedChar, parentId, UserID); // Insert in the local document
                    // String nodeId = document.getNodes().keySet().stream().max(String::compareTo).orElse(null);
                    System.out.println("Document text after insert: " + document.getText());
                    //textArea.positionCaret(insertIndex)
                    if (nodeId != null && sentOperationIds.add(nodeId)) {
                        System.out.println("Sent insert op: " + insertedChar + ", nodeId: " + nodeId);
                        websockethandler.sendInsert(roomCode, nodeId, insertedChar, parentId);
                    } else {
                        System.out.println("Failed to get nodeId or operation already sent for insertion: " + nodeId);
                    }
                } else {
                    System.out.println("Invalid cursor position for insertion: " + cursorPosition);
                }
            } else if (newValue.length() < oldValue.length()) { // Character deleted
                // Adjust cursor position and check if there's text to delete
                int deletedPosition = cursorPosition - 1;

                if (deletedPosition < 0) {
                    System.out.println("Nothing to delete (cursor at start)");
                    return;
                }

                System.out.println("Attempting to delete at position: " + deletedPosition);

                // Find the correct nodeId corresponding to the deleted character
                String nodeId = document.getNodeIdAtPosition(deletedPosition);

                if (nodeId != null) {
                    //sentOperationIds.remove(nodeId);
                    // Check if operation for this nodeId has already been sent

                    if (deletedids.add(nodeId)) {
                        System.out.println("Sent delete op for nodeId: " + nodeId);
                        document.delete(nodeId);  // Actually delete the node in the document
                        websockethandler.sendDelete(roomCode, nodeId);

                        // After deletion, update the document text
                        String updatedText = document.getText();
                        System.out.println("Updated document text after delete: '" + updatedText + "'");

                        // Update the TextArea with the updated text after deletion
                        textArea.setText(updatedText);
                    } else {
                        System.out.println("Operation already sent for nodeId: " + nodeId);
                    }
                } else {
                    System.out.println("No nodeId found at position: " + deletedPosition);
                }
            }
        }


    }


    private void handleServerOperation(CRDTOperation op) {
        Platform.runLater(() -> {
            // Skip if operation ID was sent by this client
            if (sentOperationIds.contains(op)) {
                System.out.println("Ignoring sent operation: " + op.getType() + ", id: " + op.getId());
                return;
            }
            int old = textArea.getCaretPosition();
            System.out.println("Processing remote op: " + op.getType() + ", id: " + op.getId() + ", value: " + op.getValue());
            if ("insert".equals(op.getType())) {
                document.remoteInsert(op.getId(), op.getValue(), op.getParentId());
            } else if ("delete".equals(op.getType())) {
                System.out.println("applying remote delete for id: " + op.getId());
                document.remoteDelete(op.getId());
                if (document.getNodes().get(op.getId()) == null) {
                    System.out.println("node not found for remote delete: " + op.getId());
                }
            }
            String documentText = document.getText();
            System.out.println("Remote op applied, document text: " + documentText);
            Remoteupdate = true;
            textArea.setText(documentText);
            textArea.positionCaret(Math.min(old, documentText.length()));
            Remoteupdate = false;
            updateLineNumbers();
        });
    }

    private void updateLineNumbers() {
        lineNumbers.getChildren().clear();
        int lineCount = textArea.getText().split("\n").length;
        for (int i = 1; i <= lineCount; i++) {
            Label lineLabel = new Label(String.valueOf(i));
            lineLabel.getStyleClass().add("line-number-label");
            lineNumbers.getChildren().add(lineLabel);
        }
    }

    private void updateUndoRedoButtons() {
        undoButton.setDisable(false);
        redoButton.setDisable(false);
    }

    private String generateEditorCode() {
        return "EDT-" + System.currentTimeMillis();
    }

    private String generateViewerCode() {
        return "VWR-" + System.currentTimeMillis();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);  // Green "info" style
        alert.setTitle(title);
        alert.setHeaderText(null);  // No header
        alert.setContentText(message);

        // Optional: Add a checkmark icon (requires custom CSS)
        alert.getDialogPane().getStylesheets().add("path/to/your/styles.css");
        alert.showAndWait();

}
}