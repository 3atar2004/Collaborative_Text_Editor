package com.example.server;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
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
        return documentService.joinSession(code,username);
    }
}
