
        package com.example.server;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CRDTController {
    public DocumentService service;

    public CRDTController(DocumentService Service) {
        this.service = Service;
    }

    @MessageMapping("/room/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public CRDTOperation handleRoomMessage(@DestinationVariable String roomId, CRDTOperation op) {
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
}


