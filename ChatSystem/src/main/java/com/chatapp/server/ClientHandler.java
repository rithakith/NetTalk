package com.chatapp.server;

import com.chatapp.common.Message;
import com.chatapp.auth.UserAuthService;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * MEMBER 2 CONTRIBUTION: Concurrent Client Handler
 *
 * Network Programming Concept: Multithreading with ExecutorService (ThreadPool)
 *
 * This class demonstrates how to efficiently manage multiple concurrent client
 * connections using Java's thread pool pattern. Instead of creating a new thread
 * for each client (which doesn't scale), we use ExecutorService to manage a
 * pool of reusable threads.
 *
 * Key Concepts Demonstrated:
 * - ExecutorService and ThreadPool for scalability
 * - Runnable pattern for concurrent task execution
 * - Efficient resource management with thread reuse
 * - Preventing server overload with bounded thread pools
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean running;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // Set up I/O streams
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // Important: flush the stream header
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[ClientHandler-" + Thread.currentThread().getName() +
                    "] New client connection from: " + socket.getInetAddress());

            // Main message loop - handle incoming messages
            while (running) {
                Message msg = (Message) in.readObject();
                if (msg == null) continue;

                System.out.println("[ClientHandler] Received: " + msg.getType() + " from: " + msg.getSender());
                processMessage(msg);
            }
        } catch (EOFException e) {
            System.out.println("[ClientHandler] Client disconnected: " + username);
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                System.err.println("[ClientHandler] Error handling client: " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Process received message based on type
     */
    private void processMessage(Message message) {
        switch (message.getType()) {
            case REGISTER:
                handleRegister(message);
                break;
            case LOGIN:
                handleLogin(message);
                break;
            case CONNECT:
                handleLegacyConnect(message);
                break;
            case CHAT:
                // Broadcast regular chat message
                server.broadcastMessage(message);
                break;

            case PRIVATE_MSG:
                // Send private message to specific user
                server.sendPrivateMessage(message);
                break;

            case API_REQUEST:
                // Handle API request (Member 5's feature)
                server.handleApiRequest(message, this);
                break;
            case FILE_SEND:
                handleIncomingFile(message);
                break;
            case DISCONNECT:
                running = false;
                break;

            case TYPING_START:
                // Broadcast typing start to other users
                server.broadcastTypingStatus(message, this);
                break;

            case TYPING_STOP:
                // Broadcast typing stop to other users
                server.broadcastTypingStatus(message, this);
                break;

            default:
                System.out.println("[ClientHandler] Unknown message type: " + message.getType());
        }
    }

    /**
     * Handle user registration using UserAuthService
     */
    private void handleRegister(Message msg) {
        String user = msg.getSender();
        String password = msg.getContent();

        if (user == null || user.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "REGISTER_FAIL: Invalid username/password"));
            return;
        }

        // Use UserAuthService for registration
        boolean success = UserAuthService.register(user, password);
        if (success) {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "REGISTER_SUCCESS: User created successfully"));
            System.out.println("[ClientHandler] Registration successful for: " + user);
        } else {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "REGISTER_FAIL: Username already exists"));
            System.out.println("[ClientHandler] Registration failed - user exists: " + user);
        }
    }

    /**
     * Handle user login using UserAuthService
     */
    private void handleLogin(Message msg) {
        String user = msg.getSender();
        String password = msg.getContent();

        if (user == null || user.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "LOGIN_FAIL: Invalid username/password"));
            return;
        }

        // Use UserAuthService for authentication
        boolean valid = UserAuthService.authenticate(user, password);
        if (valid) {
            // Register as active user
            boolean activated = server.getUserManager().registerUser(user, this);
            if (activated) {
                this.username = user;
                String role = UserAuthService.getRole(user);
                sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                        "LOGIN_SUCCESS: Welcome back, " + user + " (Role: " + role + ")"));
                server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server", user + " has joined the chat"));
                server.getUserManager().broadcastUserList();
                System.out.println("[ClientHandler] Login successful for: " + user + " (Role: " + role + ")");
            } else {
                sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "LOGIN_FAIL: User already logged in"));
            }
        } else {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server", "LOGIN_FAIL: Invalid credentials"));
            System.out.println("[ClientHandler] Login failed for: " + user);
        }
    }

    private void handleLegacyConnect(Message msg) {
        // Legacy flow without authentication - now requires login
        String user = msg.getSender();
        if (user == null || user.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server", "Invalid username"));
            return;
        }

        // For legacy connect, auto-register as guest if doesn't exist
        if (!UserAuthService.userExists(user)) {
            UserAuthService.register(user, "guest123", "user");
        }

        this.username = user;
        boolean activated = server.getUserManager().registerUser(user, this);
        if (activated) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server", "Welcome, " + user));
            server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server", user + " has joined the chat"));
            server.getUserManager().broadcastUserList();
        } else {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server", "Username '" + user + "' is already active"));
            running = false;
        }
    }

    /**
     * Handle incoming file upload
     */
    private void handleIncomingFile(Message message) {
        try {
            String fileName = message.getFileName();
            String base64Data = message.getContent();
            long fileSize = message.getFileSize();

            if (fileName == null || base64Data == null) {
                sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server", "Invalid file data"));
                return;
            }

            // Decode base64 back to bytes
            byte[] fileData = Base64.getDecoder().decode(base64Data);

            // Ensure uploads directory exists
            Files.createDirectories(Paths.get("uploads"));

            // Save file on server
            String serverPath = "uploads/" + System.currentTimeMillis() + "_" + fileName;
            Files.write(Paths.get(serverPath), fileData);

            System.out.println("[ClientHandler] File saved: " + serverPath + " (" + fileData.length + " bytes)");

            // Broadcast file notification to all users
            Message fileNotification = new Message(
                    Message.MessageType.SYSTEM,
                    "Server",
                    "📁 " + username + " shared a file: " + fileName + " (" + formatFileSize(fileData.length) + ")"
            );
            server.broadcastMessage(fileNotification);

            // Send success response to sender
            sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server",
                    "✅ File uploaded successfully: " + fileName));

        } catch (Exception e) {
            System.err.println("[ClientHandler] File handling error: " + e.getMessage());
            sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server",
                    "❌ File upload failed: " + e.getMessage()));
        }
    }

    /**
     * Format file size for human readable output
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }

    /**
     * Send message to this client
     */
    public void sendMessage(Message message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error sending message to " + username + ": " + e.getMessage());
            running = false;
        }
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        running = false;

        // Unregister from server
        if (username != null) {
            server.unregisterClient(username);
            server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    username + " has left the chat"));
        }

        // Close resources
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[ClientHandler] Error closing resources: " + e.getMessage());
        }

        System.out.println("[ClientHandler] Cleaned up resources for: " + username);
    }

    public String getUsername() {
        return username;
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}