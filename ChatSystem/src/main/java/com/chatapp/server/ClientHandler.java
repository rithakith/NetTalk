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
 * This class efficiently manages concurrent client connections using thread pools.
 * It handles:
 * - Authentication (Register/Login)
 * - Chat (Public/Private)
 * - File transfer
 * - API requests
 * - Typing notifications
 * - Message delivery and seen confirmations
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
            out.flush(); // flush the stream header
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("[ClientHandler-" + Thread.currentThread().getName() +
                    "] New client connection from: " + socket.getInetAddress());

            // Main message loop
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
                server.broadcastMessage(message);
                break;

            case PRIVATE_MSG:
                server.sendPrivateMessage(message);
                break;

            case FILE_SEND:
                handleIncomingFile(message);
                break;

            case API_REQUEST:
                server.handleApiRequest(message, this);
                break;

            case DISCONNECT:
                running = false;
                break;

            case TYPING_START:
                server.broadcastTypingStatus(message, this);
                break;

            case TYPING_STOP:
                server.broadcastTypingStatus(message, this);
                break;

            case MESSAGE_DELIVERED:
                server.handleMessageDelivered(message.getContent(), username);
                break;

            case MESSAGE_SEEN:
                server.handleMessageSeen(message.getContent(), username);
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
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                    "REGISTER_FAIL: Invalid username/password"));
            return;
        }

        boolean success = UserAuthService.register(user, password);
        if (success) {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                    "REGISTER_SUCCESS: User created successfully"));
            System.out.println("[ClientHandler] Registration successful for: " + user);
        } else {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                    "REGISTER_FAIL: Username already exists"));
            System.out.println("[ClientHandler] Registration failed - user exists: " + user);
        }
    }

    /**
     * Handle user login
     */
    private void handleLogin(Message msg) {
        String user = msg.getSender();
        String password = msg.getContent();

        if (user == null || user.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                    "LOGIN_FAIL: Invalid username/password"));
            return;
        }

        boolean valid = UserAuthService.authenticate(user, password);
        if (valid) {
            boolean activated = server.getUserManager().registerUser(user, this);
            if (activated) {
                this.username = user;
                String role = UserAuthService.getRole(user);
                sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                        "LOGIN_SUCCESS: Welcome back, " + user + " (Role: " + role + ")"));
                server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
                        user + " has joined the chat"));
                server.getUserManager().broadcastUserList();
                System.out.println("[ClientHandler] Login successful for: " + user + " (Role: " + role + ")");
            } else {
                sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                        "LOGIN_FAIL: User already logged in"));
            }
        } else {
            sendMessage(new Message(Message.MessageType.AUTH_RESPONSE, "Server",
                    "LOGIN_FAIL: Invalid credentials"));
            System.out.println("[ClientHandler] Login failed for: " + user);
        }
    }

    /**
     * Handle legacy "CONNECT" flow (no authentication)
     */
    private void handleLegacyConnect(Message msg) {
        String user = msg.getSender();
        if (user == null || user.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server", "Invalid username"));
            return;
        }

        if (!UserAuthService.userExists(user)) {
            UserAuthService.register(user, "guest123", "user");
        }

        this.username = user;
        boolean activated = server.getUserManager().registerUser(user, this);
        if (activated) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server", "Welcome, " + user));
            server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    user + " has joined the chat"));
            server.getUserManager().broadcastUserList();
        } else {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Username '" + user + "' is already active"));
            running = false;
        }
    }

    /**
     * Handle file upload
     */
    private void handleIncomingFile(Message message) {
        try {
            String fileName = message.getFileName();
            String base64Data = message.getContent();

            if (fileName == null || base64Data == null) {
                sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server", "Invalid file data"));
                return;
            }

            byte[] fileData = Base64.getDecoder().decode(base64Data);
            Files.createDirectories(Paths.get("uploads"));
            String serverPath = "uploads/" + System.currentTimeMillis() + "_" + fileName;
            Files.write(Paths.get(serverPath), fileData);

            System.out.println("[ClientHandler] File saved: " + serverPath);

            // Notify all users
            Message fileNotification = new Message(Message.MessageType.SYSTEM, "Server",
                    username + " shared a file: " + fileName + " (" + formatFileSize(fileData.length) + ")");
            server.broadcastMessage(fileNotification);

            sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server",
                    "File uploaded successfully: " + fileName));

        } catch (Exception e) {
            System.err.println("[ClientHandler] File handling error: " + e.getMessage());
            sendMessage(new Message(Message.MessageType.FILE_RESPONSE, "Server",
                    "File upload failed: " + e.getMessage()));
        }
    }

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

        if (username != null) {
            server.unregisterClient(username);
            server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    username + " has left the chat"));
        }

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
