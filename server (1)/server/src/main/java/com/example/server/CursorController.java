package com.example.server;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class CursorController {
    @MessageMapping("/cursors/{roomId}")
    @SendTo("/topic/cursors/{roomId}")
    public CursorPositionMessage handleCursorUpdate(
            @DestinationVariable String roomId,
            CursorPositionMessage message) {
        System.out.println("recieved cursor position from "+ message.getUserId()+"with position"+message.getPosition());
        return message; // Broadcast to all room subscribers
    }
}
