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
                // Check if message is a command
                if (message.getContent().startsWith("/")) {
                    handleCommand(message);
                } else {
                    // Broadcast regular chat message
                    server.broadcastMessage(message);
                }
                break;
                
            case PRIVATE_MSG:
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
                
            // Quiz message handling
            case QUIZ_CREATED:
            case QUIZ_INVITATION:
            case QUIZ_JOIN:
            case QUIZ_START:
            case QUIZ_QUESTION:
            case QUIZ_ANSWER:
            case QUIZ_RESULTS:
            case QUIZ_ENDED:
                server.handleQuizMessage(message, this);
                break;
                
            default:
                System.out.println("[ClientHandler] Unknown message type: " + message.getType());
        }
    }
    
    /**
     * Handle quiz and other commands
     */
    private void handleCommand(Message message) {
        String content = message.getContent();
        String[] parts = content.split("\\s+", 2); // Split into command and arguments
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        switch (command) {
            case "/createquiz":
                handleCreateQuizCommand(args);
                break;
                
            case "/addquestion":
                handleAddQuestionCommand(args);
                break;
                
            case "/invitequiz":
                handleInviteQuizCommand(args);
                break;
                
            case "/startquiz":
                handleStartQuizCommand(args);
                break;
                
            case "/joinquiz":
                handleJoinQuizCommand(args);
                break;
                
            case "/answer":
                handleAnswerCommand(args);
                break;
                
            case "/deletequiz":
                handleDeleteQuizCommand(args);
                break;
                
            case "/help":
                handleHelpCommand();
                break;
                
            case "/quizzes":
                handleListQuizzesCommand();
                break;
                
            default:
                // Send error message back to user
                sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Unknown command: " + command + ". Type /help for available commands."));
                break;
        }
    }
    
    private void handleCreateQuizCommand(String args) {
        if (args.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /createquiz <quiz_name>"));
            return;
        }
        
        server.handleCreateQuiz(args.trim(), username, this);
    }
    
    private void handleAddQuestionCommand(String args) {
        // Format: /addquestion quiz_id|question_text|option1,option2,option3,option4|correct_index|time_limit
        String[] parts = args.split("\\|");
        if (parts.length != 5) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /addquestion quiz_id|question_text|option1,option2,option3,option4|correct_index|time_limit"));
            return;
        }
        
        try {
            String quizId = parts[0].trim();
            String questionText = parts[1].trim();
            String[] options = parts[2].split(",");
            int correctIndex = Integer.parseInt(parts[3].trim());
            int timeLimit = Integer.parseInt(parts[4].trim());
            
            server.handleAddQuestion(quizId, questionText, options, correctIndex, timeLimit, username, this);
        } catch (NumberFormatException e) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Invalid number format in command. Check correct_index and time_limit."));
        }
    }
    
    private void handleInviteQuizCommand(String args) {
        // Format: /invitequiz quiz_id username1,username2,username3
        String[] parts = args.split("\\s+", 2);
        if (parts.length != 2) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /invitequiz quiz_id username1,username2,username3"));
            return;
        }
        
        String quizId = parts[0];
        String[] usernames = parts[1].split(",");
        server.handleInviteToQuiz(quizId, usernames, username, this);
    }
    
    private void handleStartQuizCommand(String args) {
        if (args.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /startquiz <quiz_id>"));
            return;
        }
        
        server.handleStartQuiz(args.trim(), username, this);
    }
    
    private void handleJoinQuizCommand(String args) {
        if (args.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /joinquiz <quiz_id>"));
            return;
        }
        
        server.handleJoinQuiz(args.trim(), username, this);
    }
    
    private void handleDeleteQuizCommand(String args) {
        if (args.trim().isEmpty()) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /deletequiz <quiz_id>"));
            return;
        }
        
        server.handleDeleteQuiz(args.trim(), username, this);
    }
    
    private void handleAnswerCommand(String args) {
        // Format: /answer quiz_id answer_index
        String[] parts = args.split("\\s+");
        if (parts.length != 2) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Usage: /answer quiz_id answer_index"));
            return;
        }
        
        try {
            String quizId = parts[0];
            int answerIndex = Integer.parseInt(parts[1]);
            server.handleQuizAnswer(quizId, answerIndex, username, this);
        } catch (NumberFormatException e) {
            sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                "Invalid answer index. Must be a number."));
        }
    }
    
    private void handleHelpCommand() {
        StringBuilder help = new StringBuilder();
        help.append("Available commands:\n");
        help.append("/createquiz <name> - Create a new quiz\n");
        help.append("/addquestion quiz_id|question|option1,option2,option3,option4|correct_index|time_limit\n");
        help.append("/invitequiz quiz_id username1,username2 - Invite users to quiz\n");
        help.append("/startquiz quiz_id - Start a quiz (admin only)\n");
        help.append("/joinquiz quiz_id - Join an existing quiz\n");
        help.append("/answer quiz_id answer_index - Submit answer during quiz\n");
        help.append("/deletequiz quiz_id - Delete a quiz (admin only)\n");
        help.append("/quizzes - List active quizzes\n");
        help.append("/help - Show this help message");
        
        sendMessage(new Message(Message.MessageType.SYSTEM, "Server", help.toString()));
    }
    
    private void handleListQuizzesCommand() {
        server.handleListQuizzes(username, this);
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
