package com.chatapp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * MEMBER 1 CONTRIBUTION: TCP Connection Listener
 * 
 * Network Programming Concept: Blocking TCP Sockets (ServerSocket)
 * 
 * This class implements the core server logic using ServerSocket to handle
 * initial incoming client connections. It demonstrates reliable, ordered
 * TCP communication - the foundation of client-server architecture.
 * 
 * Key Concepts Demonstrated:
 * - ServerSocket.accept() for blocking connection acceptance
 * - TCP 3-way handshake for reliable connection establishment
 * - Connection-oriented communication
 */
public class ConnectionListener implements Runnable {
    private final int port;
    private final ChatServer server;
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    public ConnectionListener(int port, ChatServer server) {
        this.port = port;
        this.server = server;
        this.running = false;
    }
    
    @Override
    public void run() {
        try {
            // Create TCP server socket - binds to port and listens
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("[ConnectionListener] Server started on port " + port);
            System.out.println("[ConnectionListener] Waiting for client connections...");
            
            while (running) {
                try {
                    // BLOCKING CALL: Waits for incoming client connection
                    // This demonstrates TCP connection-oriented nature
                    Socket clientSocket = serverSocket.accept();
                    
                    // Connection established - get client info
                    String clientInfo = clientSocket.getInetAddress().getHostAddress() + 
                                       ":" + clientSocket.getPort();
                    System.out.println("[ConnectionListener] New connection from: " + clientInfo);
                    
                    // Hand off the connected socket to the concurrent handler
                    server.handleNewClient(clientSocket);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[ConnectionListener] Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[ConnectionListener] Failed to start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    /**
     * Gracefully shutdown the connection listener
     */
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("[ConnectionListener] Server socket closed");
            }
        } catch (IOException e) {
            System.err.println("[ConnectionListener] Error closing server socket: " + e.getMessage());
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
