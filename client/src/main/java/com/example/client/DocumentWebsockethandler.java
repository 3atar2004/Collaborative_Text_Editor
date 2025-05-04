        package com.example.client;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DocumentWebsockethandler {
    private volatile StompSession stompSession;
    private Consumer<CRDTOperation> messageHandler;
    //hetta zeyada le reconnection
    private ScheduledExecutorService reconnectExecutor;
    private String roomCode;
    private Queue<CRDTOperation> pendingOperations = new ConcurrentLinkedQueue<>();
    private long lastConnectedTime;
    private boolean manualDisconnect = false;
    private final long RECONNECTION_WINDOW_MS = 300000; // 5 minutes

    public DocumentWebsockethandler() {
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    public boolean connectToWebSocket() {
        if (stompSession != null && stompSession.isConnected()) {
            System.out.println("Already connected.");
            return true;
        }
        //reconnect
        manualDisconnect = false;

        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        String url = "ws://localhost:8080/ws";
        MyStompSessionHandler sessionHandler = new MyStompSessionHandler();

        CompletableFuture<Boolean> connectionResult = new CompletableFuture<>();
        try {
            ListenableFuture<StompSession> future = stompClient.connect(url, sessionHandler);
            future.addCallback(
                session -> {
                    stompSession = session;
                    //reconnect 
                    lastConnectedTime = System.currentTimeMillis();
                    System.out.println("Successfully connected to WebSocket server.");
                    //reconnect
                    if (roomCode != null) {
                        subscribeToRoom(roomCode);
                    }
                    flushPendingOperations();
                    connectionResult.complete(true);
                },
                ex -> {
                    System.err.println("WebSocket connection failed: " + ex.getMessage());
                      //reconnect
                    scheduleReconnection();
                    connectionResult.complete(false);
                    //connectionResult.completeExceptionally(ex);
                }
        );
        return connectionResult.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("Connection attempt failed: " + e.getMessage());
            scheduleReconnection();
            return false;
        }
    }

        // try {
        //     return connectionResult.get(10, TimeUnit.SECONDS);
        // } catch (Exception e) {
        //     System.err.println("Connection attempt failed: " + e.getMessage());
        //     return false;
        // }
    
        private void scheduleReconnection() {
            if (manualDisconnect || (System.currentTimeMillis() - lastConnectedTime) > RECONNECTION_WINDOW_MS) {
                System.out.println("Reconnection window expired or manual disconnect");
                return;
            }
    
            System.out.println("Scheduling reconnection attempt...");
            reconnectExecutor.schedule(() -> {
                if (!manualDisconnect && (System.currentTimeMillis() - lastConnectedTime) <= RECONNECTION_WINDOW_MS) {
                    connectToWebSocket();
                }
            }, 5, TimeUnit.SECONDS);
        }

        private void flushPendingOperations() {
            while (!pendingOperations.isEmpty()) {
                CRDTOperation op = pendingOperations.poll();
                sendOperation(op);
            }
        }

    public void subscribeToRoom(String roomCode) {
        this.roomCode = roomCode;
        if (stompSession == null || !stompSession.isConnected()) {
            System.err.println("Not connected. Please ensure connectToWebSocket() succeeds.");
            return;
        }

        String topic = "/topic/room/" + roomCode;
        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return CRDTOperation.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                CRDTOperation op = (CRDTOperation) payload;
                if (messageHandler != null) {
                    messageHandler.accept(op);
                }
            }
        });
        System.out.println("Subscribed to topic: " + topic);
    }
    public void disconnect() {
        //reconnect
        manualDisconnect = true;

        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
            stompSession = null;
            System.out.println("Disconnected from WebSocket server.");
        }
    }
    private void sendOperation(CRDTOperation op) {
        if (stompSession == null || !stompSession.isConnected()) {
            pendingOperations.add(op);
            System.out.println("Operation queued for later delivery: " + op.getType());
            return;
        }

        String destination = "/app/room/" + roomCode;
        stompSession.send(destination, op);
        System.out.println("Sent operation: " + op.getType());
    }
    
    public void sendInsert(String roomCode, String id, char value, String parentId) {
        if (stompSession == null || !stompSession.isConnected()) {
            System.err.println("Not connected. Please ensure connectToWebSocket() succeeds.");
            return;
        }

        CRDTOperation op = new CRDTOperation();
        op.setType("insert");
        op.setId(id);
        op.setValue(value);
        op.setParentId(parentId);
        op.setroomId(roomCode);

        String destination = "/app/room/" + roomCode;
        stompSession.send(destination, op);
        System.out.println("Sent insert op: " + value);
    }
    public void sendDelete(String roomCode,String id)
    {
        if(stompSession==null||!stompSession.isConnected())
        {
            System.err.println("Not connected to websocket");
            return;
        }
        CRDTOperation op = new CRDTOperation();
        op.setType("delete");
        op.setId(id);
        op.setroomId(roomCode);
        String destination = "/app/room/"+roomCode;
        stompSession.send(destination,op);
        System.out.println("sent delete op for id: "+id);
    }

    public void setMessageHandler(Consumer<CRDTOperation> handler) {
        this.messageHandler = handler;
    }

    public boolean isConnected() {
        return stompSession != null && stompSession.isConnected();
    }
}



