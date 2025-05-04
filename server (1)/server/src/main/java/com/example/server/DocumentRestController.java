package com.example.server;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class DocumentRestController {

    public DocumentService documentService;
    public DocumentRestController(DocumentService documentService) {this.documentService = documentService;}

    @PostMapping("/create")
    public Map<String,String > create(@RequestBody String username) {
        return documentService.createSession(username);
    }
    @PostMapping("/join")
    public Map<String,String > join(@RequestBody String params) {
        String code = params.split(",")[0];
        String username = params.split(",")[1];
        List<String>users=documentService.getusers(code);
        Map<String,String>codes= documentService.joinSession(code,username);
        System.out.println("After joining: ");
        for (String user : users) {
            System.out.println(user);
        }
        System.out.println("/////");
        return codes;
    }
//    @PostMapping ("/getdoc")
//    public Document get(@RequestBody String code) {
//        return documentService.getDocumentFromCode(code);
//}
    @GetMapping("/document/{code}")
    public Map<String, Object> getDocument(@PathVariable String code) {
        Document doc = documentService.getDocumentFromCode(code);
        if (doc == null) {
            throw new IllegalArgumentException("No document found for room code: " + code);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("content", doc.getText()); // Assuming Document has getText()
        return response;
    }
    @PostMapping("/getdoc")
    public ResponseEntity<Document> getDocumentByCode(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        Document document = documentService.getDocumentFromCode(code);
        return ResponseEntity.ok(document);
    }
}
