package com.example.server;

import org.springframework.stereotype.Service;

import javax.lang.model.element.NestingKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
@Service
public class DocumentService {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, String> viewerToEditorMap = new ConcurrentHashMap<>();
    private final Map<String, List<CRDTOperation>> operationHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY = 1000;
    private final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<String>> Userlist= new ConcurrentHashMap<>();
    public synchronized Map<String,String>joinSession(String code,String Username)
    {
        //System.out.println("code is: "+code);
        String editorcode="";
        String viewercode="";
        boolean valid=isValidCode(code); // test if code is avaliable
        if(!valid) return null;
        System.out.println("1- "+code);
        String role= getRole(code);
        if(role.equals("editor"))
        {
            editorcode=code;
            viewercode=viewerToEditorMap.get(editorcode);

        }
        else
        {
            viewercode=code;
            //editorcode="hi";
            for(Map.Entry<String,String> entry: viewerToEditorMap.entrySet())
            {
                if(entry.getValue().equals(viewercode)){
                    editorcode=entry.getKey();
                }
            }

        }
        System.out.println(code);
        List<String> users=Userlist.getOrDefault(editorcode,new ArrayList<>());
        users.add(Username);

        Userlist.put(code,users);
        return Map.of(
                "editorCode", editorcode,
                "viewerCode", viewercode
        );

    }

    public synchronized Map<String, String> createSession(String username) {
        String editorCode = generateReadableCode() + "-e";
        String viewerCode = editorCode.replace("-e", "-v");
        List<String> users=new ArrayList<String>();
        users.add(username);
        viewerToEditorMap.put(editorCode,viewerCode);
        Userlist.put(editorCode,users);
        documents.put(editorCode, new Document());
        viewerToEditorMap.put(viewerCode, editorCode);

        return Map.of(
                "editorCode", editorCode,
                "viewerCode", viewerCode
        );
    }

    public Document getDocumentFromCode(String code) {
        String editorCode = code.endsWith("-v") ? viewerToEditorMap.get(code) : code;
        return documents.get(editorCode);
    }

    public String getRole(String code) {
        if (code.endsWith("-e")) return "editor";
        if (code.endsWith("-v")) return "viewer";
        return "unknown";
    }

    public boolean isValidCode(String code) {
        return documents.containsKey(code) || viewerToEditorMap.containsKey(code);
    }

    private String generateReadableCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
    public List<String> getusers(String roomcode)
    {
        if(getRole(roomcode)=="editor")
        {
            return Userlist.get(roomcode);
        }
        else
        {
            String viewer=roomcode;
            String editorcode="";
            for(Map.Entry<String,String> entry: viewerToEditorMap.entrySet())
            {
                if(entry.getValue().equals(viewer)){
                    editorcode=entry.getKey();
                }
            }

            return Userlist.get(editorcode);
        }
    }
    public List<String> removeuser(String roomcode,String username)
    {
        Userlist.get(roomcode).remove(username);
        if(getRole(roomcode)=="editor")
        {
            return Userlist.get(roomcode);
        }
        else
        {
            String viewer=roomcode;
            String editorcode="";
            for(Map.Entry<String,String> entry: viewerToEditorMap.entrySet())
            {
                if(entry.getValue().equals(viewer)){
                    editorcode=entry.getKey();
                }
            }
            return Userlist.get(editorcode);
        }
    }
    public String geteditorcode(String roomcode)
    {
        String editorcode="";
        if(getRole(roomcode)=="editor")
        {
            return roomcode;
        }
        else
        {
            String viewer=roomcode;
             editorcode="";
            for(Map.Entry<String,String> entry: viewerToEditorMap.entrySet())
            {
                if(entry.getValue().equals(viewer)){
                    editorcode=entry.getKey();
                }
            }
        }
        return editorcode;
    }
    public void registerClientActivity(String roomCode, String userId) {
        ClientSession session = activeSessions.computeIfAbsent(userId + "|" + roomCode, 
            key -> new ClientSession(userId, roomCode));
        session.setLastActive(System.currentTimeMillis());
    }
    
    public boolean isWithinReconnectionWindow(String userId, String roomCode) {
        ClientSession session = activeSessions.get(userId + "|" + roomCode);
        if (session == null) return false;
        
        return System.currentTimeMillis() - session.getLastActive() <= 300000; // 5 minutes
    }
    private static class ClientSession {
        private String userId;
        private String roomCode;
        private long lastActive;
        
        public ClientSession(String userId, String roomCode) {
            this.userId = userId;
            this.roomCode = roomCode;
            this.lastActive = System.currentTimeMillis();
        }
        public String getUserId() {
            return userId;
        }  
        public String getRoomCode() {
            return roomCode;
        }
        public long getLastActive() {
            return lastActive;
        }   
        public void setLastActive(long lastActive) {
            this.lastActive = lastActive;
        }
        public void setUserId(String userId) {
            this.userId = userId;
        }
        public void setRoomCode(String roomCode) {
            this.roomCode = roomCode;
        }
    }
    public void trackOperation(String roomId, CRDTOperation op) {
        operationHistory.compute(roomId, (key, history) -> {
            if (history == null) history = new ArrayList<>();
            op.setTimestamp(System.currentTimeMillis());
            op.setSequenceNumber(history.size() + 1);
            history.add(op);
            if (history.size() > MAX_HISTORY) {
                history = history.subList(history.size() - MAX_HISTORY, history.size());
            }
            return history;
        });
    }
     public List<CRDTOperation> getMissedOperations(String roomId, long since) {
        List<CRDTOperation> history = operationHistory.getOrDefault(roomId, new ArrayList<>());
        return history.stream()
                .filter(op -> op.getTimestamp() > since)
                .collect(Collectors.toList());
    }

}
