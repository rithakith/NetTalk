package com.chatapp.client;

import com.chatapp.common.Message;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Simple Console-based Chat Client
 * Connects to ChatServer and allows sending/receiving messages
 */
public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;
    
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;
    private volatile boolean running;
    
    public ChatClient(String username) {
        this.username = username;
        this.running = false;
    }
    
    /**
     * Connect to the chat server
     */
    public void connect() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        
        // Send connect message
        Message connectMsg = new Message(Message.MessageType.CONNECT, username, "");
        out.writeObject(connectMsg);
        out.flush();
        
        running = true;
        System.out.println("Connected to server as '" + username + "'");
        System.out.println("\nCommands:");
        System.out.println("  Regular message: just type and press Enter");
        System.out.println("  Private message: @username message");
        System.out.println("  API commands: /weather <city>, /joke, /quote, /help");
        System.out.println("  Quit: /quit\n");
    }
    
    /**
     * Start receiving messages (in separate thread)
     */
    public void startReceiving() {
        Thread receiveThread = new Thread(() -> {
            try {
                while (running) {
                    Message message = (Message) in.readObject();
                    displayMessage(message);
                }
            } catch (EOFException e) {
                System.out.println("\nDisconnected from server");
            } catch (IOException | ClassNotFoundException e) {
                if (running) {
                    System.err.println("Error receiving message: " + e.getMessage());
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * Display received message
     */
    private void displayMessage(Message message) {
        switch (message.getType()) {
            case CHAT:
                System.out.println("[" + message.getSender() + "]: " + message.getContent());
                break;
            case SYSTEM:
                System.out.println("[SYSTEM]: " + message.getContent());
                break;
            case PRIVATE_MSG:
                System.out.println("[PRIVATE from " + message.getSender() + "]: " + message.getContent());
                break;
            case API_RESPONSE:
                System.out.println("\n" + "=".repeat(50));
                System.out.println(message.getContent());
                System.out.println("=".repeat(50) + "\n");
                break;
            case USER_LIST:
                System.out.println("[SYSTEM]: " + message.getContent());
                break;
            default:
                System.out.println(message.getContent());
        }
    }
    
    /**
     * Send message to server
     */
    public void sendMessage(String content) throws IOException {
        Message message;
        
        // Check for private message (@username message)
        if (content.startsWith("@")) {
            int spaceIndex = content.indexOf(' ');
            if (spaceIndex > 0) {
                String receiver = content.substring(1, spaceIndex);
                String privateContent = content.substring(spaceIndex + 1);
                message = new Message(Message.MessageType.PRIVATE_MSG, username, receiver, privateContent);
            } else {
                System.out.println("Invalid private message format. Use: @username message");
                return;
            }
        }
        // Check for API commands
        else if (content.startsWith("/")) {
            if (content.equalsIgnoreCase("/quit")) {
                disconnect();
                return;
            }
            message = new Message(Message.MessageType.API_REQUEST, username, content);
        }
        // Regular chat message
        else {
            message = new Message(Message.MessageType.CHAT, username, content);
        }
        
        out.writeObject(message);
        out.flush();
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() throws IOException {
        running = false;
        
        if (out != null) {
            Message disconnectMsg = new Message(Message.MessageType.DISCONNECT, username, "");
            out.writeObject(disconnectMsg);
            out.flush();
        }
        
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
        
        System.out.println("Disconnected from server");
    }
    
    /**
     * Main method to run the client
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        // Get username
        System.out.print("Enter your username: ");
        String username = scanner.nextLine().trim();
        
        if (username.isEmpty()) {
            System.out.println("Username cannot be empty!");
            return;
        }
        
        ChatClient client = new ChatClient(username);
        
        try {
            // Connect to server
            client.connect();
            
            // Start receiving messages
            client.startReceiving();
            
            // Main input loop
            while (client.running) {
                String input = scanner.nextLine();
                if (!input.trim().isEmpty()) {
                    client.sendMessage(input);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                client.disconnect();
            } catch (IOException e) {
                // Ignore
            }
            scanner.close();
        }
    }
}
