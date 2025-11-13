package com.chatapp.server;

import com.chatapp.common.Message;
import com.chatapp.api.ExternalApiClient;

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
 * - Extended: Message Delivery and Seen Tracking
 */
public class ChatServer {
    private static final int PORT = 8888;
    private static final int THREAD_POOL_SIZE = 10;

    // Member 1's component
    private ConnectionListener connectionListener;
    private Thread listenerThread;

    // Member 2's component
    private ExecutorService clientExecutor;

    // Member 4's component
    private UserManager userManager;

    // Extended feature: Message delivery and seen tracking
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
        System.out.println("Demonstrating 5 Network Programming Concepts + Message Tracking");
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

        System.out.println("[ChatServer] Server started successfully!");
        System.out.println("[ChatServer] Ready to accept connections...\n");
    }

    /**
     * Handle new client connection (called by ConnectionListener)
     */
    public void handleNewClient(Socket clientSocket) {
        ClientHandler handler = new ClientHandler(clientSocket, this);
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
            userManager.broadcastUserList();
        } else {
            Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "Username '" + username + "' is already active. Please reconnect with a different name.");
            handler.sendMessage(errorMsg);
            handler.disconnect();
        }
    }

    /**
     * Unregister client and update user list
     */
    public void unregisterClient(String username) {
        userManager.unregisterUser(username);
        userManager.broadcastUserList();
    }

    /**
     * Broadcast message to all connected clients and track delivery
     */
    public void broadcastMessage(Message message) {
        if (message.getType() == Message.MessageType.CHAT) {
            userManager.addToChatHistory(message);
            messageTracker.trackMessage(message);
        }

        for (ClientHandler handler : userManager.getAllHandlers()) {
            handler.sendMessage(message);
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
        for (ClientHandler handler : userManager.getAllHandlers()) {
            if (handler != sender) {
                handler.sendMessage(message);
            }
        }
        System.out.println("[ChatServer] Typing status: " + message.getSender() + " - " + message.getType());
    }

    /**
     * Send private message to a specific user
     */
    public void sendPrivateMessage(Message message) {
        String receiver = message.getReceiver();
        ClientHandler receiverHandler = userManager.getUserHandler(receiver);

        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
            messageTracker.trackMessage(message);
            messageTracker.markDelivered(message.getMessageId(), receiver);

            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message confirmMsg = new Message(Message.MessageType.SYSTEM, "Server",
                        "Private message sent to " + receiver);
                senderHandler.sendMessage(confirmMsg);
            }
        } else {
            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                        "User '" + receiver + "' not found or offline");
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
     * Mark all messages from one sender as seen by another user
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

        if (connectionListener != null) {
            connectionListener.shutdown();
        }

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
     * Print server statistics
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
     * Main entry point
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();

        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        try {
            while (server.running) {
                Thread.sleep(30000);
                server.printStatistics();
            }
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        }
    }
}
