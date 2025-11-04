package com.chatapp.server;

import com.chatapp.common.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.chatapp.server.ClientHandler;
import com.chatapp.util.PasswordUtil;

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

    // Credentials store: username -> hashedPassword
    private final ConcurrentHashMap<String, String> credentials;

    // Salt store: username -> salt
    private final ConcurrentHashMap<String, String> salts;

    // Roles: username -> role ("user", "admin")
    private final ConcurrentHashMap<String, String> roles;

    private static final int MAX_HISTORY_SIZE = 100;
    
    // Synchronization lock for complex operations
    private final Object userLock = new Object();
    private final Object historyLock = new Object();

    public UserManager() {
        this.activeUsers = new ConcurrentHashMap<>();
        this.chatHistory = new CopyOnWriteArrayList<>();
        this.credentials = new ConcurrentHashMap<>();
        this.salts = new ConcurrentHashMap<>();
        this.roles = new ConcurrentHashMap<>();
        System.out.println("[UserManager] Thread-safe user manager initialized");
        // Bootstrap: create an admin for demo
        String admin = "admin";
        String adminPass = "admin123"; // change before demo if needed
        registerCredentials(admin, adminPass, "admin");
    }

    /**
     * Register username => hashedPassword. Returns false if user exists.
     */
    public boolean registerCredentials(String username, String plaintextPassword) {
        return registerCredentials(username, plaintextPassword, "user");
    }

    public boolean registerCredentials(String username, String plaintextPassword, String role) {
        synchronized (userLock) {
            if (credentials.containsKey(username)) return false;
            String salt = PasswordUtil.generateSalt();
            String hashed = PasswordUtil.hashPassword(plaintextPassword, salt);
            credentials.put(username, hashed);
            salts.put(username, salt);
            roles.put(username, role);
            System.out.println("[UserManager] Credentials registered for: " + username + " role=" + role);
            return true;
        }
    }

    /**
     * Validate username & password
     */
    public boolean validateCredentials(String username, String plaintextPassword) {
        String storedHash = credentials.get(username);
        String salt = salts.get(username);
        if (storedHash == null || salt == null) return false;
        String attemptHash = PasswordUtil.hashPassword(plaintextPassword, salt);
        return storedHash.equals(attemptHash);
    }

    public String getRole(String username) {
        return roles.getOrDefault(username, "user");
    }

    /**
     * Register an active client handler after successful auth
     */
    public boolean registerUser(String username, ClientHandler handler) {
        synchronized (userLock) {
            if (activeUsers.containsKey(username)) {
                System.out.println("[UserManager] Registration failed: username '" + 
                                 username + "' already exists");
                return false;
            }
            
            activeUsers.put(username, handler);
            System.out.println("[UserManager] User '" + username + "' registered. Total users: " + activeUsers.size());
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
                System.out.println("[UserManager] User '" + username + "' unregistered. Total users: " + activeUsers.size());
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
                for (int i = 0; i < toRemove; i++) chatHistory.remove(0);
            }
            System.out.println("[UserManager] Message added to history. Total messages: " + chatHistory.size());
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
            int start = Math.max(0, size - count);
            return new ArrayList<>(chatHistory.subList(start, size));
        }
    }

    public void clearChatHistory() {
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
        return credentials.containsKey(username);
    }

    public Map<String, Object> getStatistics() {
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
