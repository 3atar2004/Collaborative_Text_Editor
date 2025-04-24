package com.example.server;
public class Node { //represents a single character crdt element
    private String id;          // Unique ID format: "userID:timestamp" (e.g., "User1:123456789")
    private char value;         // The actual character (e.g., 'A')
    private String parentId;    // ID of the preceding character (null for the first character)
    private boolean isDeleted;  // Tombstone flag (true if deleted)


    public Node(String id, char value, String parentId) {
        this.id = id;
        this.value = value;
        this.parentId = parentId;
        this.isDeleted = false;
    }
    public String getId() { return id; }
    public char getValue() { return value; }
    public String getParentId() { return parentId; }
    public boolean isDeleted() { return isDeleted; }


    public void delete() {
        this.isDeleted = true;
    }

    public void setParentId(String rootId) {
        this.parentId=rootId;
    }
}