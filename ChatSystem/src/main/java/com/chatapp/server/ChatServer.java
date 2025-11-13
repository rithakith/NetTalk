package com.chatapp.server;

import com.chatapp.common.Message;
import com.chatapp.api.ExternalApiClient;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main Chat Server - Integrates all components from all 5 members
 * 
 * This server combines:
 * - Member 1: TCP Connection Listener
 * - Member 2: Concurrent Client Handler with ThreadPool
 * - Member 3: NIO Message Broadcaster
 * - Member 4: Thread-Safe User Manager
 * - Member 5: External API Integration
 */
public class ChatServer {
    private static final int PORT = 8888;
    private static final int THREAD_POOL_SIZE = 10;
    
    // Member 1's component
    private ConnectionListener connectionListener;
    private Thread listenerThread;
    
    // Member 2's component
    private ExecutorService clientExecutor;
    
    // Member 3's component (disabled by default, can be enabled for demo)
    // private MessageBroadcaster nioBroadcaster;
    // private Thread broadcasterThread;
    
    // Member 4's component
    private UserManager userManager;
    
    // Message tracking component
    private MessageTracker messageTracker;
    
    // Server state
    private volatile boolean running;
    
    public ChatServer() {
        this.userManager = new UserManager();
        this.messageTracker = new MessageTracker(userManager);
        this.clientExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = false;
        
        System.out.println("=".repeat(60));
        System.out.println("ChatSystem - Network Programming Assignment");
        System.out.println("Demonstrating 5 Network Programming Concepts");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Start the chat server
     */
    public void start() {
        if (running) {
            System.out.println("Server is already running!");
            return;
        }
        
        running = true;
        System.out.println("\n[ChatServer] Starting server on port " + PORT + "...");
        
        // Start Member 1's Connection Listener
        connectionListener = new ConnectionListener(PORT, this);
        listenerThread = new Thread(connectionListener, "ConnectionListener");
        listenerThread.start();
        
        // Optional: Start Member 3's NIO Broadcaster
        // Uncomment to enable NIO broadcasting
        /*
        try {
            nioBroadcaster = new MessageBroadcaster();
            broadcasterThread = new Thread(nioBroadcaster, "NIOBroadcaster");
            broadcasterThread.start();
        } catch (IOException e) {
            System.err.println("Failed to start NIO broadcaster: " + e.getMessage());
        }
        */
        
        System.out.println("[ChatServer] Server started successfully!");
        System.out.println("[ChatServer] Ready to accept connections...\n");
    }
    
    /**
     * Handle new client connection (called by Member 1's ConnectionListener)
     * Uses Member 2's ExecutorService to handle concurrently
     */
    public void handleNewClient(Socket clientSocket) {
        // Create Member 2's ClientHandler
        ClientHandler handler = new ClientHandler(clientSocket, this);
        
        // Submit to thread pool for concurrent execution
        clientExecutor.submit(handler);
        
        System.out.println("[ChatServer] Client handler submitted to thread pool");
    }
    
    /**
     * Register client (uses Member 4's UserManager)
     */
    public void registerClient(String username, ClientHandler handler) {
        // Check if user is banned
        if (userManager.isBanned(username)) {
            Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                "You are banned from this server.");
            handler.sendMessage(errorMsg);
            handler.disconnect();
            return;
        }
        
        boolean success = userManager.registerUser(username, handler);
        
        if (success) {
            // Check if user is admin
            if (userManager.isAdmin(username)) {
                Message adminMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "⭐ You are logged in as ADMIN");
                handler.sendMessage(adminMsg);
            }
            
            // Send user list to all clients
            userManager.broadcastUserList();
        } else {
            // Username already exists
            Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                "Username '" + username + "' is already taken. Please reconnect with a different name.");
            handler.sendMessage(errorMsg);
            handler.disconnect();
        }
    }
    
    /**
     * Unregister client (uses Member 4's UserManager)
     */
    public void unregisterClient(String username) {
        userManager.unregisterUser(username);
        userManager.broadcastUserList();
    }
    
    /**
     * Broadcast message to all connected clients
     */
    public void broadcastMessage(Message message) {
        // Add to chat history (Member 4's thread-safe operation)
        if (message.getType() == Message.MessageType.CHAT) {
            userManager.addToChatHistory(message);
            
            // Track message for delivery and seen status
            messageTracker.trackMessage(message);
        }
        
        // Send to all clients and mark as delivered
        for (ClientHandler handler : userManager.getAllHandlers()) {
            handler.sendMessage(message);
            
            // Mark as delivered to each user (excluding sender)
            if (message.getType() == Message.MessageType.CHAT && 
                !handler.getUsername().equals(message.getSender())) {
                messageTracker.markDelivered(message.getMessageId(), handler.getUsername());
            }
        }
        
        System.out.println("[ChatServer] Broadcasted: " + message.getSender() + ": " + message.getContent());
    }
    
    /**
     * Broadcast typing status to all other clients (excluding sender)
     */
    public void broadcastTypingStatus(Message message, ClientHandler sender) {
        // Send typing status to all clients except the sender
        for (ClientHandler handler : userManager.getAllHandlers()) {
            if (handler != sender) {
                handler.sendMessage(message);
            }
        }
        
        System.out.println("[ChatServer] Typing status: " + message.getSender() + " - " + message.getType());
    }

    /**
     * Send private message to specific user
     */
    public void sendPrivateMessage(Message message) {
        String receiver = message.getReceiver();
        ClientHandler receiverHandler = userManager.getUserHandler(receiver);
        
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
            
            // Send confirmation to sender
            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message confirmMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "Private message sent to " + receiver);
                senderHandler.sendMessage(confirmMsg);
            }
        } else {
            // User not found
            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "User '" + receiver + "' not found");
                senderHandler.sendMessage(errorMsg);
            }
        }
    }
    
    /**
     * Handle message delivered confirmation
     */
    public void handleMessageDelivered(String messageId, String username) {
        messageTracker.markDelivered(messageId, username);
    }
    
    /**
     * Handle message seen confirmation
     */
    public void handleMessageSeen(String messageId, String username) {
        messageTracker.markSeen(messageId, username);
    }
    
    /**
     * Mark all messages from sender as seen by viewer
     */
    public void markAllMessagesSeen(String viewerUsername, String senderUsername) {
        messageTracker.markAllMessagesSeen(viewerUsername, senderUsername);
    }
    
    /**
     * Get MessageTracker instance
     */
    public MessageTracker getMessageTracker() {
        return messageTracker;
    }
    
    /**
     * Handle API request (Member 5's External API Integration)
     */
    public void handleApiRequest(Message request, ClientHandler requester) {
        // Process in separate thread to avoid blocking
        clientExecutor.submit(() -> {
            Message response = ExternalApiClient.processApiRequest(request);
            requester.sendMessage(response);
        });
    }
    
    // ========== ADMIN & MODERATION METHODS ==========
    
    /**
     * Check if user is muted
     */
    public boolean isUserMuted(String username) {
        return userManager.isMuted(username);
    }
    
    /**
     * Handle admin kick command
     */
    public void handleAdminKick(String adminUsername, String targetUsername, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to kick users."));
            return;
        }
        
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /kick <username>"));
            return;
        }
        
        ClientHandler targetHandler = userManager.getUserHandler(targetUsername);
        if (targetHandler == null) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "User '" + targetUsername + "' not found."));
            return;
        }
        
        // Send kick message to target
        targetHandler.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "You have been kicked by admin " + adminUsername));
        
        // Disconnect target
        targetHandler.disconnect();
        
        // Broadcast to all
        broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "🚫 " + targetUsername + " was kicked by admin " + adminUsername));
        
        System.out.println("[Admin] " + adminUsername + " kicked " + targetUsername);
    }
    
    /**
     * Handle admin ban command
     */
    public void handleAdminBan(String adminUsername, String targetUsername, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to ban users."));
            return;
        }
        
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /ban <username>"));
            return;
        }
        
        // Ban the user
        userManager.banUser(targetUsername);
        
        // Kick if currently online
        ClientHandler targetHandler = userManager.getUserHandler(targetUsername);
        if (targetHandler != null) {
            targetHandler.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "You have been banned by admin " + adminUsername));
            targetHandler.disconnect();
        }
        
        // Broadcast to all
        broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "🔨 " + targetUsername + " was banned by admin " + adminUsername));
        
        System.out.println("[Admin] " + adminUsername + " banned " + targetUsername);
    }
    
    /**
     * Handle admin mute command
     */
    public void handleAdminMute(String adminUsername, String targetUsername, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to mute users."));
            return;
        }
        
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /mute <username>"));
            return;
        }
        
        ClientHandler targetHandler = userManager.getUserHandler(targetUsername);
        if (targetHandler == null) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "User '" + targetUsername + "' not found."));
            return;
        }
        
        // Mute the user
        userManager.muteUser(targetUsername);
        
        // Notify target
        targetHandler.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "🔇 You have been muted by admin " + adminUsername));
        
        // Notify admin
        requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "✅ " + targetUsername + " has been muted."));
        
        System.out.println("[Admin] " + adminUsername + " muted " + targetUsername);
    }
    
    /**
     * Handle admin unmute command
     */
    public void handleAdminUnmute(String adminUsername, String targetUsername, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to unmute users."));
            return;
        }
        
        if (targetUsername == null || targetUsername.trim().isEmpty()) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /unmute <username>"));
            return;
        }
        
        if (!userManager.isMuted(targetUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                targetUsername + " is not muted."));
            return;
        }
        
        // Unmute the user
        userManager.unmuteUser(targetUsername);
        
        // Notify target if online
        ClientHandler targetHandler = userManager.getUserHandler(targetUsername);
        if (targetHandler != null) {
            targetHandler.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "🔊 You have been unmuted by admin " + adminUsername));
        }
        
        // Notify admin
        requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
            "✅ " + targetUsername + " has been unmuted."));
        
        System.out.println("[Admin] " + adminUsername + " unmuted " + targetUsername);
    }
    
    /**
     * Handle admin broadcast command
     */
    public void handleAdminBroadcast(String adminUsername, String announcement, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to broadcast messages."));
            return;
        }
        
        if (announcement == null || announcement.trim().isEmpty()) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /broadcast <message>"));
            return;
        }
        
        // Broadcast announcement
        Message broadcastMsg = new Message(Message.MessageType.SYSTEM, "Server",
            "📢 ADMIN ANNOUNCEMENT from " + adminUsername + ": " + announcement);
        
        for (ClientHandler handler : userManager.getAllHandlers()) {
            handler.sendMessage(broadcastMsg);
        }
        
        System.out.println("[Admin] " + adminUsername + " broadcast: " + announcement);
    }
    
    /**
     * Handle admin stats command
     */
    public void handleAdminStats(String adminUsername, ClientHandler requester) {
        if (!userManager.isAdmin(adminUsername)) {
            requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "⛔ You don't have admin permissions to view stats."));
            return;
        }
        
        // Get statistics
        int activeUsers = userManager.getUserCount();
        int totalMessages = userManager.getChatHistory().size();
        List<String> onlineUsers = userManager.getActiveUsernames();
        List<String> bannedUsers = userManager.getBannedList();
        List<String> mutedUsers = userManager.getMutedList();
        List<String> admins = userManager.getAdminList();
        
        StringBuilder stats = new StringBuilder();
        stats.append("\n📊 SERVER STATISTICS\n");
        stats.append("═══════════════════════\n");
        stats.append("Active Users: ").append(activeUsers).append("\n");
        stats.append("Total Messages: ").append(totalMessages).append("\n");
        stats.append("Online Users: ").append(String.join(", ", onlineUsers)).append("\n");
        stats.append("Admins: ").append(admins.isEmpty() ? "None" : String.join(", ", admins)).append("\n");
        stats.append("Banned Users: ").append(bannedUsers.isEmpty() ? "None" : String.join(", ", bannedUsers)).append("\n");
        stats.append("Muted Users: ").append(mutedUsers.isEmpty() ? "None" : String.join(", ", mutedUsers)).append("\n");
        stats.append("═══════════════════════");
        
        requester.sendMessage(new Message(Message.MessageType.SYSTEM, "Server", stats.toString()));
        
        System.out.println("[Admin] " + adminUsername + " requested server statistics");
    }
    
    /**
     * Shutdown server gracefully
     */
    public void shutdown() {
        System.out.println("\n[ChatServer] Shutting down server...");
        running = false;
        
        // Shutdown connection listener
        if (connectionListener != null) {
            connectionListener.shutdown();
        }
        
        // Shutdown NIO broadcaster if running
        /*
        if (nioBroadcaster != null) {
            nioBroadcaster.shutdown();
        }
        */
        
        // Shutdown client executor
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                clientExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientExecutor.shutdownNow();
        }
        
        System.out.println("[ChatServer] Server shutdown complete");
    }
    
    /**
     * Get server statistics
     */
    public void printStatistics() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Server Statistics");
        System.out.println("=".repeat(60));
        System.out.println("Active Users: " + userManager.getUserCount());
        System.out.println("Total Messages: " + userManager.getChatHistory().size());
        System.out.println("Online Users: " + String.join(", ", userManager.getActiveUsernames()));
        System.out.println("=".repeat(60) + "\n");
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Start server
        server.start();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
        
        // Keep server running and print stats periodically
        try {
            while (server.running) {
                Thread.sleep(30000); // Print stats every 30 seconds
                server.printStatistics();
            }
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        }
    }
}
