package com.example.server;

import org.springframework.stereotype.Service;

import javax.lang.model.element.NestingKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class DocumentService {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, String> viewerToEditorMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> Userlist= new ConcurrentHashMap<>();
    public synchronized Map<String,String>joinSession(String code,String Username)
    {
        String editorcode="";
        String viewercode="";
        boolean valid=isValidCode(code); // test if code is avaliable
        if(!valid) return null;
        String role= getRole(code);
        if(role.equals("editor"))
        {
             editorcode=code;
             viewercode=viewerToEditorMap.get(editorcode);

        }
        else
        {
             viewercode=code;
             editorcode="hi";
            for(Map.Entry<String,String> entry: viewerToEditorMap.entrySet())
            {
                if(entry.getValue().equals(viewercode)){
                    editorcode=entry.getKey();
                }
            }

        }
        List<String> users=Userlist.getOrDefault(code,new ArrayList<>());
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
}
