package com.example.client;

import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpHelper {
    private static final RestTemplate restTemplate= new RestTemplate();
    HttpHelper()
    {
        //restTemplate.getMessageConverters().add(new MappingJackson2CborHttpMessageConverter());
    }
    //public static String baseUrl;
    static String baseUrl="http://localhost:8080";
    public  Map<String,String> createSession(String username) {
        Map<String,String> sessionCodes = new ConcurrentHashMap<>();
        String url = baseUrl + "/create";
        try{
            sessionCodes=restTemplate.postForObject(url,username,Map.class);
            System.out.println("Session created successfully\n");
        }catch (Exception e){
            System.out.println("Failed to create session with error " + e.getMessage());
            System.out.println("Exiting..");
            System.exit(0);
        }
        return sessionCodes;
    }
    public  Map<String,String> joinSession(String code,String username) {
        Map<String,String> sessionCodes = new ConcurrentHashMap<>();
        String url = baseUrl + "/join";
        try{
            String params = code+","+username;
            //String params=code;
            sessionCodes=restTemplate.postForObject(url,params,Map.class);
            System.out.println("Session Joined successfully\n");
        }catch (Exception e){
            System.out.println("Failed to Join session with error " + e.getMessage());
            System.out.println("Exiting..");
            System.exit(0);
        }
        return sessionCodes;
    }
    public  Document getDocumentFromCode(String code) {
        String url = baseUrl + "/getdoc";
        Document document = null;
        try{
            document=restTemplate.postForObject(url,code,Document.class);
            System.out.println("Document fetched successfully\n");
        }catch (Exception e){
            System.out.println("Failed to fetch document with error " + e.getMessage());
            System.out.println("Exiting..");
        }
        return document;
}
    public Document getdocuments(String code) {
        String url = baseUrl + "/document/" + code;
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                System.out.println("No document found for room code: " + code);
                System.out.println("Exiting..");
                System.exit(0);
            }

            List<Map<String, Object>> nodes = (List<Map<String, Object>>) response.get("nodes");
            if (nodes == null) {
               System.out.println("Invalid document data for room code: " + code);
//                System.out.println("Exiting..");
//                System.exit(0);
                return null;
            }

            Document document = new Document();
            for (Map<String, Object> node : nodes) {
                String id = (String) node.get("id");
                String value = (String) node.get("value");
                String parentId = (String) node.get("parentId");
                if (value != null && value.length() == 1) {
                    document.remoteInsert(id, value.charAt(0), parentId); // Assuming remoteInsert
                }
            }

            System.out.println("Document retrieved successfully for room code: " + code);
            return document;
        } catch (Exception e) {
            System.out.println("Failed to retrieve document for room code " + code + " with error: " + e.getMessage());
            System.out.println("Exiting..");
            System.exit(0);
            return null; // Unreachable
        }
    }

}