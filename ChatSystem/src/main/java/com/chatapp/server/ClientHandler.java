package com.chatapp.server;

import com.chatapp.common.Message;
import java.io.*;
import java.net.Socket;

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
                             "] Handling client from: " + socket.getInetAddress());
            
            // Read first message (should be CONNECT with username)
            Message connectMsg = (Message) in.readObject();
            if (connectMsg.getType() == Message.MessageType.CONNECT) {
                username = connectMsg.getSender();
                System.out.println("[ClientHandler] User '" + username + "' connected");
                
                // Register this handler with the server
                server.registerClient(username, this);
                
                // Send welcome message
                sendMessage(new Message(Message.MessageType.SYSTEM, "Server", 
                    "Welcome to the chat, " + username + "!"));
                
                // Broadcast user joined
                server.broadcastMessage(new Message(Message.MessageType.SYSTEM, "Server", 
                    username + " has joined the chat"));
            }
            
            // Main message loop - handle incoming messages
            while (running) {
                Message message = (Message) in.readObject();
                processMessage(message);
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
            case CHAT:
                // Check if user is muted
                if (server.isUserMuted(username)) {
                    sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                        "You are muted and cannot send messages."));
                    return;
                }
                // Broadcast regular chat message
                server.broadcastMessage(message);
                break;
                
            case PRIVATE_MSG:
                // Check if user is muted
                if (server.isUserMuted(username)) {
                    sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                        "You are muted and cannot send messages."));
                    return;
                }
                // Send private message to specific user
                server.sendPrivateMessage(message);
                break;
                
            case API_REQUEST:
                // Handle API request (Member 5's feature)
                server.handleApiRequest(message, this);
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
                
            case MESSAGE_DELIVERED:
                // Handle message delivery confirmation
                server.handleMessageDelivered(message.getContent(), username);
                break;
                
            case MESSAGE_SEEN:
                // Handle message seen confirmation
                server.handleMessageSeen(message.getContent(), username);
                break;
            
            // Admin commands
            case ADMIN_KICK:
                server.handleAdminKick(username, message.getContent(), this);
                break;
                
            case ADMIN_BAN:
                server.handleAdminBan(username, message.getContent(), this);
                break;
                
            case ADMIN_MUTE:
                server.handleAdminMute(username, message.getContent(), this);
                break;
                
            case ADMIN_UNMUTE:
                server.handleAdminUnmute(username, message.getContent(), this);
                break;
                
            case ADMIN_BROADCAST:
                server.handleAdminBroadcast(username, message.getContent(), this);
                break;
                
            case ADMIN_STATS:
                server.handleAdminStats(username, this);
                break;
                
            default:
                System.out.println("[ClientHandler] Unknown message type: " + message.getType());
        }
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
    }
}
