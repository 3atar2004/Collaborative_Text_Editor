package com.example.server;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
@Service
public class DocumentService {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, String> viewerToEditorMap = new ConcurrentHashMap<>();

    public synchronized Map<String, String> createSession() {
        String editorCode = generateReadableCode() + "-e";
        String viewerCode = editorCode.replace("-e", "-v");

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
