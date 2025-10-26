package com.chatapp.server;

import com.chatapp.common.Message;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MEMBER 4 CONTRIBUTION: Thread-Safe Resource Management
 * 
 * Network Programming Concept: Synchronization and Concurrency Control
 * 
 * This class ensures that shared data structures (user lists, chat logs)
 * are safely accessed and modified by multiple threads simultaneously.
 * Demonstrates various synchronization techniques to prevent race conditions
 * and ensure data consistency in a multi-threaded environment.
 * 
 * Key Concepts Demonstrated:
 * - synchronized keyword for mutual exclusion
 * - ConcurrentHashMap for thread-safe map operations
 * - CopyOnWriteArrayList for thread-safe list operations
 * - Lock-free concurrent collections
 * - Preventing race conditions and data inconsistency
 */
public class UserManager {
    // Thread-safe map: username -> ClientHandler
    private final ConcurrentHashMap<String, ClientHandler> activeUsers;
    
    // Thread-safe list for chat history
    private final CopyOnWriteArrayList<Message> chatHistory;
    
    // Maximum chat history size
    private static final int MAX_HISTORY_SIZE = 100;
    
    // Synchronization lock for complex operations
    private final Object userLock = new Object();
    private final Object historyLock = new Object();
    
    public UserManager() {
        this.activeUsers = new ConcurrentHashMap<>();
        this.chatHistory = new CopyOnWriteArrayList<>();
        System.out.println("[UserManager] Thread-safe user manager initialized");
    }
    
    /**
     * Register a new user - thread-safe operation
     * Returns true if successful, false if username already exists
     */
    public boolean registerUser(String username, ClientHandler handler) {
        synchronized (userLock) {
            if (activeUsers.containsKey(username)) {
                System.out.println("[UserManager] Registration failed: username '" + 
                                 username + "' already exists");
                return false;
            }
            
            activeUsers.put(username, handler);
            System.out.println("[UserManager] User '" + username + "' registered. Total users: " + 
                             activeUsers.size());
            return true;
        }
    }
    
    /**
     * Unregister a user - thread-safe operation
     */
    public void unregisterUser(String username) {
        synchronized (userLock) {
            ClientHandler removed = activeUsers.remove(username);
            if (removed != null) {
                System.out.println("[UserManager] User '" + username + "' unregistered. Total users: " + 
                                 activeUsers.size());
            }
        }
    }
    
    /**
     * Get all active usernames - returns thread-safe snapshot
     */
    public List<String> getActiveUsernames() {
        synchronized (userLock) {
            // Return a copy to prevent external modification
            return new ArrayList<>(activeUsers.keySet());
        }
    }
    
    /**
     * Get handler for specific user - thread-safe
     */
    public ClientHandler getUserHandler(String username) {
        return activeUsers.get(username); // ConcurrentHashMap.get() is thread-safe
    }
    
    /**
     * Get all active handlers - returns thread-safe collection
     */
    public Collection<ClientHandler> getAllHandlers() {
        // ConcurrentHashMap.values() returns thread-safe collection view
        return activeUsers.values();
    }
    
    /**
     * Add message to chat history - thread-safe with size limit
     */
    public void addToChatHistory(Message message) {
        synchronized (historyLock) {
            chatHistory.add(message);
            
            // Maintain history size limit
            if (chatHistory.size() > MAX_HISTORY_SIZE) {
                // Remove oldest messages
                int toRemove = chatHistory.size() - MAX_HISTORY_SIZE;
                for (int i = 0; i < toRemove; i++) {
                    chatHistory.remove(0);
                }
            }
            
            System.out.println("[UserManager] Message added to history. Total messages: " + 
                             chatHistory.size());
        }
    }
    
    /**
     * Get chat history - returns thread-safe copy
     */
    public List<Message> getChatHistory() {
        synchronized (historyLock) {
            // Return a copy to prevent external modification
            return new ArrayList<>(chatHistory);
        }
    }
    
    /**
     * Get recent messages (last N)
     */
    public List<Message> getRecentMessages(int count) {
        synchronized (historyLock) {
            int size = chatHistory.size();
            int startIndex = Math.max(0, size - count);
            return new ArrayList<>(chatHistory.subList(startIndex, size));
        }
    }
    
    /**
     * Clear chat history - thread-safe
     */
    public synchronized void clearChatHistory() {
        synchronized (historyLock) {
            chatHistory.clear();
            System.out.println("[UserManager] Chat history cleared");
        }
    }
    
    /**
     * Get user count - thread-safe
     */
    public int getUserCount() {
        return activeUsers.size(); // ConcurrentHashMap.size() is thread-safe
    }
    
    /**
     * Check if user exists - thread-safe
     */
    public boolean userExists(String username) {
        return activeUsers.containsKey(username); // Thread-safe
    }
    
    /**
     * Get statistics - demonstrates complex synchronized operation
     */
    public synchronized Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        synchronized (userLock) {
            stats.put("activeUsers", activeUsers.size());
            stats.put("usernames", new ArrayList<>(activeUsers.keySet()));
        }
        
        synchronized (historyLock) {
            stats.put("totalMessages", chatHistory.size());
        }
        
        return stats;
    }
    
    /**
     * Broadcast user list update to all clients
     */
    public void broadcastUserList() {
        List<String> usernames = getActiveUsernames();
        String userListStr = String.join(", ", usernames);
        
        Message userListMsg = new Message(
            Message.MessageType.USER_LIST, 
            "Server", 
            "Online users: " + userListStr
        );
        
        // Send to all active users
        for (ClientHandler handler : getAllHandlers()) {
            handler.sendMessage(userListMsg);
        }
    }
}
