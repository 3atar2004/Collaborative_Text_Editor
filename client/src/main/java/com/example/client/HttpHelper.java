package com.example.client;

import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpHelper {
    private static final RestTemplate restTemplate= new RestTemplate();
    public static String baseUrl;

    public static Map<String,String> createSession(String username) {
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
    public static Map<String,String> joinSession(String code,String username) {
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

}
