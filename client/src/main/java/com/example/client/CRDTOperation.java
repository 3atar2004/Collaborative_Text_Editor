package com.example.client;

public class CRDTOperation {
    private String type;      // "insert" or "delete"
    private String id;        //node id
    private char value;       // only used for insert
    private String parentId;  // only used for insert
    private String roomId;

    public CRDTOperation() {} // Needed for deserialization

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public char getValue() { return value; }
    public void setValue(char value) { this.value = value; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setroomId(String roomID){this.roomId=roomID;}
    public String getRoomId(){return this.roomId;}
}
