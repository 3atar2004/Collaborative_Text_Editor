package com.example.server;
import java.util.*;

public class Document {
    private final Map<String, Node> nodes;  // Maps node IDs to Node objects
    private String rootId;                     // ID of the first character (null if empty)

    public Document() {
        this.nodes = new HashMap<>();
        this.rootId = null;  // Document starts empty
    }

    private long lasttimestamp= System.currentTimeMillis();
    private String generateid(String userid)
    {
        synchronized (this)
        {
            lasttimestamp=Math.max(lasttimestamp+1,System.currentTimeMillis());
            return userid+":"+lasttimestamp;
        }
    }
    public void insert(char value, String parentId, String userId) {
        // Check if the parentId is provided and valid
        if (parentId != null && !nodes.containsKey(parentId)) {
            throw new IllegalArgumentException("Parent node not found: " + parentId);
        }

        // Generate a unique ID with timestamp to ensure proper ordering
        String nodeId = generateid(userId);
        Node newNode = new Node(nodeId, value, parentId);

        synchronized (this) {
            nodes.put(nodeId, newNode);

            // If root is null, this is the first node, so set it as the root
            if (rootId == null) {
                rootId = nodeId;
            }
            // If inserting at root level but root exists, make it a child of root
            else if (parentId == null) {
                newNode.setParentId(rootId);  // Modify to be child of root
            }
        }

        // Record the insert for undo functionality
        recordInsert(nodeId, userId);
    }



    public void delete(String nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }
        nodes.get(nodeId).delete(); // Mark the node as deleted
    }

    public void remoteInsert(String id, char value, String parentId) {
        if (nodes.containsKey(id)) return; // avoid re-inserting the same op
        Node newNode = new Node(id, value, parentId);
        nodes.put(id, newNode);
        if (rootId == null) {
            rootId = id;
        }
    }

    public void remoteDelete(String id) {
        Node node = nodes.get(id);
        if (node != null) {
            node.delete();
        }
    }


    public String getText() {
        if(rootId==null)return "";
        StringBuilder documenttext=new StringBuilder();
        traverse(rootId,documenttext);
        return documenttext.toString();
    }
    private List<Node>getchildren(String parentId)
    {
        List<Node> children= new ArrayList<>();
        for(Node n: nodes.values())
        {
            if(parentId.equals(n.getParentId()))
            {
                children.add(n);
            }
        }
        return children;
    }
    private void sortChildren(List<Node> children) {
        children.sort((a, b) -> {
            // Split IDs into [userId, timestamp]
            String[] aParts = a.getId().split(":");
            String[] bParts = b.getId().split(":");

            // First, compare timestamps (DESC)
            long aTime = Long.parseLong(aParts[1]);
            long bTime = Long.parseLong(bParts[1]);
            if (aTime != bTime) {
                return Long.compare(bTime, aTime);
            }

            return aParts[0].compareTo(bParts[0]);
        });
    }
    private void traverse(String nodeId, StringBuilder builder) {
        Node node = nodes.get(nodeId);
        if (node == null) return;

        // Only append to output if node is NOT deleted
        if (!node.isDeleted()) {
            builder.append(node.getValue());
        }

        // Always traverse children (even if parent is deleted)
        List<Node> children = getchildren(nodeId);
        sortChildren(children);
        for (Node child : children) {
            traverse(child.getId(), builder);
        }
    }

    // Add this method to CRDTDocument
    public Map<String, Node> getNodes() {
        return new HashMap<>(nodes); // Return a copy to preserve encapsulation
    }


    public String getRootId() {
        return rootId;
    }
    public void printNodeDetails() {
        System.out.println("=== Node Details ===");
        System.out.printf("%-20s | %-6s | %-20s | %-8s%n",
                "ID", "Value", "Parent ID", "Deleted");
        System.out.println("-----------------------------------------------------");

        nodes.values().forEach(node -> {
            System.out.printf("%-20s | %-6c | %-20s | %-8b%n",
                    node.getId(),
                    node.getValue(),
                    node.getParentId() != null ? node.getParentId() : "null",
                    node.isDeleted()
            );
        });
    }
    private Map<String, Deque<String>> undoStacks = new HashMap<>();
private Map<String, Deque<String>> redoStacks = new HashMap<>();

public void undo(String userId) {
    if (!undoStacks.containsKey(userId)) return;
    Deque<String> stack = undoStacks.get(userId);
    if (stack.isEmpty()) return;
    String lastId = stack.pop();
    Node node = nodes.get(lastId);
    if (node != null) {
        node.delete();
        redoStacks.computeIfAbsent(userId, k -> new LinkedList<>()).push(lastId);
    }
}

public void redo(String userId) {
    if (!redoStacks.containsKey(userId)) return;
    Deque<String> stack = redoStacks.get(userId);
    if (stack.isEmpty()) return;
    String id = stack.pop();
    Node node = nodes.get(id);
    if (node != null) {
        //node.undelete(); // implement this if needed
        undoStacks.get(userId).push(id);
    }
}

public void recordInsert(String id, String userId) {
    undoStacks.computeIfAbsent(userId, k -> new LinkedList<>()).push(id);
    // clear redo stack
    redoStacks.computeIfAbsent(userId, k -> new LinkedList<>()).clear();
}

}