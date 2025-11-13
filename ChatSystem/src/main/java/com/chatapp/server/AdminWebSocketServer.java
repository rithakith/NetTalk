package com.chatapp.server;

import com.chatapp.common.Message;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Admin WebSocket Server - Dedicated admin interface on port 8890
 * Provides enhanced admin functionality with separate connection
 */
public class AdminWebSocketServer extends org.java_websocket.server.WebSocketServer {
    
    private static final int ADMIN_PORT = 8890;
    private static final String CHAT_SERVER_HOST = "localhost";
    private static final int CHAT_SERVER_PORT = 8888;
    
    private final ConcurrentHashMap<WebSocket, AdminConnection> adminConnections = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    private static class AdminConnection {
        String adminUsername;
        Socket tcpSocket;
        ObjectOutputStream tcpOut;
        ObjectInputStream tcpIn;
        Thread readerThread;
        
        AdminConnection(String username) {
            this.adminUsername = username;
        }
    }
    
    public AdminWebSocketServer() {
        super(new InetSocketAddress(ADMIN_PORT));
        setConnectionLostTimeout(30);
        // Allow connections from any origin (needed for web browsers)
        setReuseAddr(true);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[AdminWS] New admin connection from: " + conn.getRemoteSocketAddress());
        
        try {
            // Connect to ChatServer via TCP
            Socket tcpSocket = new Socket(CHAT_SERVER_HOST, CHAT_SERVER_PORT);
            AdminConnection adminConn = new AdminConnection("temp");
            adminConn.tcpSocket = tcpSocket;
            adminConn.tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
            adminConn.tcpOut.flush();
            adminConn.tcpIn = new ObjectInputStream(tcpSocket.getInputStream());
            
            adminConnections.put(conn, adminConn);
            
            // Start reader thread to forward messages from ChatServer to admin client
            adminConn.readerThread = new Thread(() -> {
                try {
                    while (!Thread.interrupted()) {
                        Message msg = (Message) adminConn.tcpIn.readObject();
                        
                        // Forward message to admin WebSocket client
                        JsonObject jsonMsg = new JsonObject();
                        jsonMsg.addProperty("type", msg.getType().toString());
                        jsonMsg.addProperty("sender", msg.getSender());
                        jsonMsg.addProperty("content", msg.getContent());
                        if (msg.getReceiver() != null) {
                            jsonMsg.addProperty("receiver", msg.getReceiver());
                        }
                        jsonMsg.addProperty("timestamp", msg.getTimestamp().toString());
                        
                        conn.send(jsonMsg.toString());
                        System.out.println("[AdminWS] Forwarded to admin: " + jsonMsg.toString());
                    }
                } catch (Exception e) {
                    if (!Thread.interrupted()) {
                        System.err.println("[AdminWS] Error reading from TCP: " + e.getMessage());
                    }
                }
            });
            adminConn.readerThread.start();
            
        } catch (IOException e) {
            System.err.println("[AdminWS] Failed to connect to chat server: " + e.getMessage());
            conn.close(1011, "Failed to connect to chat server");
        }
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        AdminConnection adminConn = adminConnections.get(conn);
        if (adminConn == null) return;
        
        try {
            JsonObject jsonMsg = JsonParser.parseString(message).getAsJsonObject();
            String type = jsonMsg.get("type").getAsString();
            
            Message msg = null;
            
            switch (type) {
                case "CONNECT":
                    String adminUsername = jsonMsg.get("sender").getAsString();
                    adminConn.adminUsername = adminUsername;
                    msg = new Message(Message.MessageType.CONNECT, adminUsername, "");
                    System.out.println("[AdminWS] Admin connected: " + adminUsername);
                    break;
                    
                case "CHAT":
                    // Admin commands (quiz management)
                    msg = new Message(Message.MessageType.CHAT, 
                        jsonMsg.get("sender").getAsString(),
                        jsonMsg.get("content").getAsString());
                    System.out.println("[AdminWS] Admin command: " + jsonMsg.get("content").getAsString());
                    break;
                    
                case "DISCONNECT":
                    msg = new Message(Message.MessageType.DISCONNECT,
                        jsonMsg.get("sender").getAsString(), "");
                    break;
                    
                default:
                    System.out.println("[AdminWS] Unknown admin message type: " + type);
                    return;
            }
            
            // Forward to ChatServer
            if (msg != null && adminConn.tcpOut != null) {
                adminConn.tcpOut.writeObject(msg);
                adminConn.tcpOut.flush();
            }
            
        } catch (Exception e) {
            System.err.println("[AdminWS] Error processing admin message: " + e.getMessage());
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[AdminWS] Admin connection closed: " + reason);
        
        AdminConnection adminConn = adminConnections.remove(conn);
        if (adminConn != null) {
            try {
                if (adminConn.readerThread != null) {
                    adminConn.readerThread.interrupt();
                }
                if (adminConn.tcpOut != null) {
                    adminConn.tcpOut.writeObject(new Message(Message.MessageType.DISCONNECT, 
                        adminConn.adminUsername, ""));
                    adminConn.tcpOut.close();
                }
                if (adminConn.tcpIn != null) {
                    adminConn.tcpIn.close();
                }
                if (adminConn.tcpSocket != null) {
                    adminConn.tcpSocket.close();
                }
            } catch (Exception e) {
                System.err.println("[AdminWS] Error closing admin connection: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[AdminWS] Admin WebSocket error: " + ex.getMessage());
    }
    
    @Override
    public void onStart() {
        System.out.println("[AdminWS] Admin WebSocket server started on port " + ADMIN_PORT);
        setConnectionLostTimeout(30);
    }
    
    public static void main(String[] args) {
        AdminWebSocketServer server = new AdminWebSocketServer();
        
        server.start();
        System.out.println("Admin WebSocket Server running on ws://localhost:" + ADMIN_PORT);
        System.out.println("Connecting to Chat Server at " + CHAT_SERVER_HOST + ":" + CHAT_SERVER_PORT);
        
        // Keep server running
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Admin server interrupted");
            try {
                server.stop(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}