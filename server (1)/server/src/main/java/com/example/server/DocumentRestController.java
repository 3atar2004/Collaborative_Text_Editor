package com.example.server;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final CRDTController crdtController;
    public DocumentRestController(DocumentService documentService, CRDTController crdtController) {
        this.documentService = documentService; 
         this.crdtController = crdtController;
        }

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
//    @GetMapping("/document/{code}")
//    public String getDocument(@PathVariable String code) {
//        Document doc = documentService.getDocumentFromCode(code);
////        if (doc == null) {
////            throw new IllegalArgumentException("No document found for room code: " + code);
////        }
////        Map<String, Object> response = new HashMap<>();
//         // Assuming Document has getText()
//        return doc.getText();
//    }
//    @PostMapping("/getdoc/{code}")
//    public String getDocumentByCode(@PathVariable String code) {
//        Document document = documentService.getDocumentFromCode(code);
//        System.out.println("Document text=" );
//        if(document.getText()==null)
//        {
//            return "";
//        }
//        return document.getText();
//    }
//    @GetMapping("/document/{docId}")
//    public ResponseEntity<Document> getDocument_(@PathVariable String docId) {
//        Document doc = documentService.getDocumentFromCode(docId);
//        return ResponseEntity.ok(doc);
//}
@GetMapping("/getdoc")
public Document get(@RequestParam String code) {  // Change to @RequestParam
    return documentService.getDocumentFromCode(code);
}
    
@GetMapping("/missed-operations/{roomId}")
    public List<CRDTOperation> getMissedOperations(
            @PathVariable String roomId,
            @RequestParam long since) {
        return crdtController.getMissedOperations(roomId, since);
    }

@GetMapping("/document-state/{roomId}")
public Document getFullDocumentState(@PathVariable String roomId) {
    return documentService.getDocumentFromCode(roomId);
}

}
