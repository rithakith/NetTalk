package com.chatapp.server;

import com.chatapp.common.Message;
import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple HTTP Server Bridge - Basic web interface without WebSocket dependencies
 * This is a simplified version that works without external libraries
 */
public class SimpleWebServer {
    private static final int WEB_PORT = 8889;
    private static final String CHAT_SERVER_HOST = "localhost";
    private static final int CHAT_SERVER_PORT = 8888;
    
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running;
    
    public SimpleWebServer() {
        this.executor = Executors.newFixedThreadPool(10);
        this.running = false;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(WEB_PORT);
            running = true;
            
            System.out.println("[SimpleWebServer] Server started on port " + WEB_PORT);
            System.out.println("[SimpleWebServer] Access the chat at: http://localhost:" + WEB_PORT);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(new WebRequestHandler(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[SimpleWebServer] Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[SimpleWebServer] Failed to start server: " + e.getMessage());
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            executor.shutdown();
        } catch (IOException e) {
            System.err.println("[SimpleWebServer] Error stopping server: " + e.getMessage());
        }
    }
    
    private class WebRequestHandler implements Runnable {
        private final Socket clientSocket;
        
        public WebRequestHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {
                
                String inputLine = in.readLine();
                if (inputLine == null) return;
                
                // Parse HTTP request
                String[] requestParts = inputLine.split(" ");
                if (requestParts.length < 2) return;
                
                String method = requestParts[0];
                String path = requestParts[1];
                
                // Send HTTP response
                if (method.equals("GET")) {
                    handleGetRequest(path, out);
                }
                
            } catch (IOException e) {
                System.err.println("[SimpleWebServer] Error handling request: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        
        private void handleGetRequest(String path, PrintWriter out) {
            String response;
            String contentType = "text/html";
            
            if (path.equals("/") || path.equals("/index.html")) {
                response = getIndexHtml();
            } else if (path.equals("/app.js")) {
                response = getAppJs();
                contentType = "application/javascript";
            } else if (path.equals("/style.css")) {
                response = getStyleCss();
                contentType = "text/css";
            } else {
                response = "<html><body><h1>404 Not Found</h1></body></html>";
            }
            
            // Send HTTP response
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + contentType);
            out.println("Content-Length: " + response.getBytes().length);
            out.println("Connection: close");
            out.println();
            out.println(response);
            out.flush();
        }
        
        private String getIndexHtml() {
            return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>NetTalk Quiz System</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <div class="container">
                        <header>
                        <br>
                            <h1>NetTalk Quiz System</h1>
                            <p>Interactive Chat with Quiz Features</p>
                        </header>
                        
                        <div class="panel">
                            <h2>📋 How to Connect</h2>
                            <p>To use the full quiz system features, please connect using the Java client:</p>
                            <ol>
                                <li>Open PowerShell/Command Prompt</li>
                                <li>Navigate to: <code>c:\\PERSONAL\\Projects\\Network Project\\NetTalk\\ChatSystem</code></li>
                                <li>Run: <code>java com.chatapp.client.ChatClient</code></li>
                                <li>Enter your username and start chatting!</li>
                            </ol>
                        </div>
                        
                        <div class="panel">
                            <h2>🎮 Quiz Commands</h2>
                            <div class="quiz-commands">
                                <h4>Available Commands:</h4>
                                <code>/createquiz &lt;name&gt;</code> - Create new quiz<br>
                                <code>/addquestion quiz_id|question|opt1,opt2,opt3,opt4|correct|time</code><br>
                                <code>/invitequiz quiz_id user1,user2</code> - Invite users<br>
                                <code>/startquiz quiz_id</code> - Start quiz (admin only)<br>
                                <code>/joinquiz quiz_id</code> - Join existing quiz<br>
                                <code>/answer quiz_id option_num</code> - Submit answer<br>
                                <code>/quizzes</code> - List all active quizzes<br>
                                <code>/help</code> - Show help
                            </div>
                        </div>
                        
                        <div class="panel">
                            <h2>🔧 Server Status</h2>
                            <p><span style="color: green;">●</span> Simple Web Server: Running on port 8889</p>
                            <p><span style="color: green;">●</span> Chat Server: Expected on port 8888</p>
                            <p><strong>Note:</strong> For full WebSocket functionality, external libraries are needed.</p>
                        </div>
                    </div>
                    <script src="app.js"></script>
                </body>
                </html>
                """;
        }
        
        private String getAppJs() {
            return """
                console.log('NetTalk Quiz System - Simple Web Interface');
                console.log('For full functionality, use the Java client: java com.chatapp.client.ChatClient');
                
                // Show connection instructions
                document.addEventListener('DOMContentLoaded', function() {
                    console.log('Web interface loaded. Use Java client for full quiz functionality.');
                });
                """;
        }
        
        private String getStyleCss() {
            return """
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body { 
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh; padding: 20px;
                }
                .container { max-width: 800px; margin: 0 auto; }
                header { text-align: center; color: white; margin-bottom: 30px; }
                header h1 { font-size: 2.5em; margin-bottom: 10px; text-shadow: 2px 2px 4px rgba(0,0,0,0.3); }
                header p { font-size: 1.2em; opacity: 0.9; }
                .panel { 
                    background: white; border-radius: 15px; padding: 30px; 
                    box-shadow: 0 10px 30px rgba(0,0,0,0.3); margin-bottom: 20px;
                }
                .panel h2 { color: #667eea; margin-bottom: 20px; }
                .quiz-commands { 
                    background: rgba(102, 126, 234, 0.1); border: 1px solid rgba(102, 126, 234, 0.3);
                    border-radius: 5px; padding: 15px; font-family: 'Courier New', monospace;
                }
                .quiz-commands h4 { color: #667eea; margin-bottom: 10px; }
                code { 
                    background: rgba(102, 126, 234, 0.2); padding: 2px 4px; 
                    border-radius: 3px; font-family: 'Courier New', monospace;
                }
                ol { margin-left: 20px; }
                li { margin: 5px 0; }
                """;
        }
    }
    
    public static void main(String[] args) {
        SimpleWebServer server = new SimpleWebServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        // Start server
        server.start();
    }
}