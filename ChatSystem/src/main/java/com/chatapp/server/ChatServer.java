package com.chatapp.server;

import com.chatapp.common.Message;
import com.chatapp.api.ExternalApiClient;
import com.chatapp.util.PasswordUtil;

import java.net.Socket;
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

    // Server state
    private volatile boolean running;

    public ChatServer() {
        this.userManager = new UserManager();
        this.clientExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = false;

        System.out.println("=".repeat(60));
        System.out.println("ChatSystem - Network Programming Assignment");
        System.out.println("Demonstrating 5 Network Programming Concepts");
        System.out.println("=".repeat(60));
    }

    public UserManager getUserManager() {
        return userManager;
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
     * Get user role from UserManager
     */
    public String getRole(String username) {
        return userManager.getRole(username);
    }

    /**
     * Register an active user (after successful login)
     */
    public void registerClient(String username, ClientHandler handler) {
        boolean success = userManager.registerUser(username, handler);

        if (success) {
            // Send user list to all clients
            userManager.broadcastUserList();
        } else {
            // Username already exists
            Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "Username '" + username + "' is already active. Please reconnect with a different name.");
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
        }

        // Send to all clients
        for (ClientHandler handler : userManager.getAllHandlers()) {
            handler.sendMessage(message);
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
                        "User '" + receiver + "' not found or offline");
                senderHandler.sendMessage(errorMsg);
            }
        }
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