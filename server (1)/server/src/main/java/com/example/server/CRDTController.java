
        package com.example.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CRDTController {
    public DocumentService service;
     private final Map<String, List<CRDTOperation>> operationHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY = 1000;
    
    public CRDTController(DocumentService Service) {
        this.service = Service;
    }

    @MessageMapping("/room/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public CRDTOperation handleRoomMessage(@DestinationVariable String roomId, CRDTOperation op) {
         op.setTimestamp(System.currentTimeMillis());
        op.setSequenceNumber(operationHistory.getOrDefault(roomId, new ArrayList<>()).size() + 1);
        service.trackOperation(roomId, op);
        operationHistory.compute(roomId, (key, history) -> {
            if (history == null) history = new ArrayList<>();
            history.add(op);
            if (history.size() > MAX_HISTORY) {
                history = history.subList(history.size() - MAX_HISTORY, history.size());
            }
            return history;
        });

        // Step 1: Validate room code
        if (!service.isValidCode(roomId)) {
            System.out.println("Invalid room ID: " + roomId);
            System.out.println("Invalid room ID: " + roomId);
            return null;
        }

        // Step 2: Get user's role (editor/viewer)
        String role = service.getRole(roomId);
        if ("viewer".equals(role) && !"cursor".equals(op.getType())) {
            System.out.println("Viewer tried to perform a write operation.");
            return null;
        }

        // Step 3: Get the corresponding document
        Document doc = service.getDocumentFromCode(roomId);

        // Step 4: Apply the operation
        switch (op.getType()) {
            case "insert":
                doc.remoteInsert(op.getId(), op.getValue(), op.getParentId());
                break;
            case "delete":
                doc.remoteDelete(op.getId());
                break;
            default:
                System.out.println("Unknown operation type: " + op.getType());
        }
        System.out.println(doc.getText());

        //Step 5: Return the op to broadcast to all users in the room
        return op;
    }
    public List<CRDTOperation> getMissedOperations(String roomId, long lastReceivedTimestamp) {
        List<CRDTOperation> history = operationHistory.getOrDefault(roomId, new ArrayList<>());
        return history.stream()
                .filter(op -> op.getTimestamp() > lastReceivedTimestamp)
                .collect(Collectors.toList());
    }
}


