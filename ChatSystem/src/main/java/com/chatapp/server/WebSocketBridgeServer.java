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
 * WebSocket Server Bridge - Connects Web Frontend to Java Backend
 * 
 * This server acts as a bridge between the web frontend (using WebSockets)
 * and the existing Java TCP socket server.
 */
public class WebSocketBridgeServer extends org.java_websocket.server.WebSocketServer {
    private static final int WS_PORT = 8889;
    private static final String CHAT_SERVER_HOST = "localhost";
    private static final int CHAT_SERVER_PORT = 8888;
    
    private final ConcurrentHashMap<WebSocket, ClientConnection> connections = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    private class ClientConnection {
        Socket tcpSocket;
        ObjectOutputStream tcpOut;
        ObjectInputStream tcpIn;
        Thread readerThread;
        String username;
        
        void cleanup() {
            if (readerThread != null) {
                readerThread.interrupt();
            }
            try {
                if (tcpIn != null) tcpIn.close();
                if (tcpOut != null) tcpOut.close();
                if (tcpSocket != null && !tcpSocket.isClosed()) tcpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public WebSocketBridgeServer() {
        super(new InetSocketAddress(WS_PORT));
        setConnectionLostTimeout(30);
        // Allow connections from any origin (needed for web browsers)
        setReuseAddr(true);
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[WebSocket] New connection from: " + conn.getRemoteSocketAddress());
        
        try {
            Socket tcpSocket = new Socket(CHAT_SERVER_HOST, CHAT_SERVER_PORT);
            ClientConnection clientConn = new ClientConnection();
            clientConn.tcpSocket = tcpSocket;
            clientConn.tcpOut = new ObjectOutputStream(tcpSocket.getOutputStream());
            clientConn.tcpOut.flush();
            clientConn.tcpIn = new ObjectInputStream(tcpSocket.getInputStream());
            
            connections.put(conn, clientConn);
            
            clientConn.readerThread = new Thread(() -> {
                try {
                    while (!Thread.interrupted() && conn.isOpen()) {
                        Message msg = (Message) clientConn.tcpIn.readObject();
                        
                        JsonObject jsonMsg = new JsonObject();
                        jsonMsg.addProperty("type", msg.getType().toString());
                        jsonMsg.addProperty("sender", msg.getSender());
                        jsonMsg.addProperty("content", msg.getContent());
                        if (msg.getReceiver() != null) {
                            jsonMsg.addProperty("receiver", msg.getReceiver());
                        }
                        jsonMsg.addProperty("timestamp", msg.getTimestamp().toString());
                        
                        conn.send(jsonMsg.toString());
                    }
                } catch (Exception e) {
                    if (!Thread.interrupted()) {
                        System.err.println("[WebSocket] Error reading from TCP: " + e.getMessage());
                    }
                }
            });
            clientConn.readerThread.start();
            
        } catch (IOException e) {
            System.err.println("[WebSocket] Failed to connect to chat server: " + e.getMessage());
            conn.close(1011, "Failed to connect to chat server");
        }
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        ClientConnection clientConn = connections.get(conn);
        if (clientConn == null) return;
        
        try {
            JsonObject jsonMsg = JsonParser.parseString(message).getAsJsonObject();
            String type = jsonMsg.get("type").getAsString();
            
            Message msg = null;
            
            switch (type) {
                case "CONNECT":
                    String username = jsonMsg.get("sender").getAsString();
                    clientConn.username = username;
                    msg = new Message(Message.MessageType.CONNECT, username, "");
                    break;
                    
                case "CHAT":
                    msg = new Message(Message.MessageType.CHAT, 
                        jsonMsg.get("sender").getAsString(),
                        jsonMsg.get("content").getAsString());
                    break;
                    
                case "PRIVATE_MSG":
                    msg = new Message(Message.MessageType.PRIVATE_MSG,
                        jsonMsg.get("sender").getAsString(),
                        jsonMsg.get("content").getAsString());
                    msg.setReceiver(jsonMsg.get("receiver").getAsString());
                    break;
                    
                case "API_REQUEST":
                    msg = new Message(Message.MessageType.API_REQUEST,
                        jsonMsg.get("sender").getAsString(),
                        jsonMsg.get("content").getAsString());
                    break;
                    
                case "DISCONNECT":
                    msg = new Message(Message.MessageType.DISCONNECT,
                        jsonMsg.get("sender").getAsString(), "");
                    break;
                    
                case "TYPING_START":
                    msg = new Message(Message.MessageType.TYPING_START,
                        jsonMsg.get("sender").getAsString(), "");
                    break;
                    
                case "TYPING_STOP":
                    msg = new Message(Message.MessageType.TYPING_STOP,
                        jsonMsg.get("sender").getAsString(), "");
                    break;
            }
            
            if (msg != null && clientConn.tcpOut != null) {
                clientConn.tcpOut.writeObject(msg);
                clientConn.tcpOut.flush();
            }
            
        } catch (Exception e) {
            System.err.println("[WebSocket] Error processing message: " + e.getMessage());
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[WebSocket] Connection closed: " + reason);
        
        ClientConnection clientConn = connections.remove(conn);
        if (clientConn != null) {
            if (clientConn.username != null && clientConn.tcpOut != null) {
                try {
                    Message disconnectMsg = new Message(Message.MessageType.DISCONNECT, 
                        clientConn.username, "");
                    clientConn.tcpOut.writeObject(disconnectMsg);
                    clientConn.tcpOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            clientConn.cleanup();
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] Error: " + ex.getMessage());
        if (conn != null) {
            conn.close();
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("[WebSocket] Server started on port " + WS_PORT);
        setConnectionLostTimeout(30);
    }
    
    public static void main(String[] args) {
        WebSocketBridgeServer server = new WebSocketBridgeServer();
        server.start();
        
        System.out.println("WebSocket Bridge Server running on ws://localhost:" + WS_PORT);
        System.out.println("Bridging to Chat Server at " + CHAT_SERVER_HOST + ":" + CHAT_SERVER_PORT);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.println("Shutting down WebSocket server...");
                server.stop(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }
}