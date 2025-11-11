package com.chatapp.server;

import com.chatapp.api.ExternalApiClient;
import com.chatapp.common.Message;
import com.chatapp.quiz.Quiz;
import com.chatapp.quiz.QuizManager;
import com.chatapp.quiz.QuizQuestion;
import java.net.Socket;
import java.util.List;
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
 */
public class ChatServer {
    private static final int PORT = 8888;
    private static final int THREAD_POOL_SIZE = 10;
    
    // Member 1's component
    private ConnectionListener connectionListener;
    private Thread listenerThread;
    
    // Member 2's component
    private ExecutorService clientExecutor;
    
    // Member 3's component (disabled by default, can be enabled for demo)
    // private MessageBroadcaster nioBroadcaster;
    // private Thread broadcasterThread;
    
    // Member 4's component
    private UserManager userManager;
    
    // Message tracking component
    private MessageTracker messageTracker;
    
    // Quiz management component
    private QuizManager quizManager;
    
    // Server state
    private volatile boolean running;
    
    public ChatServer() {
        this.userManager = new UserManager();
        this.messageTracker = new MessageTracker(userManager);
        this.quizManager = new QuizManager();
        this.clientExecutor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = false;
        
        // Set up quiz event listener
        setupQuizEventListener();
        
        System.out.println("=".repeat(60));
        System.out.println("ChatSystem - Network Programming Assignment");
        System.out.println("Demonstrating 5 Network Programming Concepts");
        System.out.println("=".repeat(60));
    }
    
    /**
     * Set up quiz event listener to handle quiz events
     */
    private void setupQuizEventListener() {
        quizManager.setEventListener(new QuizManager.QuizEventListener() {
            @Override
            public void onQuizCreated(Quiz quiz) {
                System.out.println("[QuizEvents] Quiz created: " + quiz.getQuizId());
            }
            
            @Override
            public void onQuizStarted(Quiz quiz) {
                System.out.println("[QuizEvents] Quiz started: " + quiz.getQuizId());
                // Broadcast to all participants that quiz has started
                Message startMsg = new Message(Message.MessageType.QUIZ_START, "Server",
                    "Quiz '" + quiz.getQuizName() + "' has started! Get ready!");
                broadcastToParticipants(quiz, startMsg);
                
                // Small delay to ensure start message is processed first
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            @Override
            public void onQuizQuestionChanged(Quiz quiz, QuizQuestion question) {
                System.out.println("[QuizEvents] New question for quiz: " + quiz.getQuizId());
                // Send current question to all participants in format expected by frontend
                // Format: "Question 1: What is...? Options: A) Option1, B) Option2, C) Option3, D) Option4"
                String questionMsg = "Question " + (quiz.getCurrentQuestionIndex() + 1) + ": " +
                                   question.getQuestionText() + "? Options: ";
                
                String[] options = question.getOptions();
                char[] letters = {'A', 'B', 'C', 'D'};
                for (int i = 0; i < options.length && i < 4; i++) {
                    if (i > 0) questionMsg += ", ";
                    questionMsg += letters[i] + ") " + options[i];
                }
                
                Message qMsg = new Message(Message.MessageType.QUIZ_QUESTION, "Server", questionMsg);
                broadcastToParticipants(quiz, qMsg);
                
                System.out.println("[QuizEvents] Question sent: " + questionMsg);
            }
            
            @Override
            public void onQuizCompleted(Quiz quiz) {
                System.out.println("[QuizEvents] Quiz completed: " + quiz.getQuizId());
                // Send results to all participants
                String resultsMsg = "Quiz '" + quiz.getQuizName() + "' completed!\n" +
                                  "Final results will be shown shortly.";
                Message resultMsg = new Message(Message.MessageType.QUIZ_RESULTS, "Server", resultsMsg);
                broadcastToParticipants(quiz, resultMsg);
            }
            
            @Override
            public void onParticipantJoined(Quiz quiz, String username) {
                System.out.println("[QuizEvents] Participant joined: " + username + " to quiz " + quiz.getQuizId());
            }
            
            @Override
            public void onAnswerSubmitted(Quiz quiz, com.chatapp.quiz.QuizResult result) {
                System.out.println("[QuizEvents] Answer submitted by: " + result.getUsername());
            }
            
            @Override
            public void sendQuizMessage(String message, List<String> recipients) {
                Message msg = new Message(Message.MessageType.SYSTEM, "QuizSystem", message);
                for (String username : recipients) {
                    ClientHandler handler = userManager.getUserHandler(username);
                    if (handler != null) {
                        handler.sendMessage(msg);
                    }
                }
            }
        });
    }
    
    /**
     * Broadcast message to all quiz participants
     */
    private void broadcastToParticipants(Quiz quiz, Message message) {
        System.out.println("[ChatServer] Broadcasting quiz message to participants: " + message.getType());
        System.out.println("[ChatServer] Participants: " + quiz.getParticipants().keySet());
        
        // Broadcast to ALL users so WebSocket clients receive it
        // The quiz participants will filter and handle it on the client side
        broadcastMessage(message);
        
        System.out.println("[ChatServer] Quiz message broadcasted to all users");
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
        
        // Optional: Start Member 3's NIO Broadcaster
        // Uncomment to enable NIO broadcasting
        /*
        try {
            nioBroadcaster = new MessageBroadcaster();
            broadcasterThread = new Thread(nioBroadcaster, "NIOBroadcaster");
            broadcasterThread.start();
        } catch (IOException e) {
            System.err.println("Failed to start NIO broadcaster: " + e.getMessage());
        }
        */
        
        System.out.println("[ChatServer] Server started successfully!");
        System.out.println("[ChatServer] Ready to accept connections...\n");
    }
    
    /**
     * Handle new client connection (called by Member 1's ConnectionListener)
     * Uses Member 2's ExecutorService to handle concurrently
     */
    public void handleNewClient(Socket clientSocket) {
        // Create Member 2's ClientHandler
        ClientHandler handler = new ClientHandler(clientSocket, this);
        
        // Submit to thread pool for concurrent execution
        clientExecutor.submit(handler);
        
        System.out.println("[ChatServer] Client handler submitted to thread pool");
    }
    
    /**
     * Register client (uses Member 4's UserManager)
     */
    public void registerClient(String username, ClientHandler handler) {
        boolean success = userManager.registerUser(username, handler);
        
        if (success) {
            // Send user list to all clients
            userManager.broadcastUserList();
        } else {
            // Username already exists
            Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                "Username '" + username + "' is already taken. Please reconnect with a different name.");
            handler.sendMessage(errorMsg);
            handler.disconnect();
        }
    }
    
    /**
     * Unregister client (uses Member 4's UserManager)
     */
    public void unregisterClient(String username) {
        userManager.unregisterUser(username);
        userManager.broadcastUserList();
    }
    
    /**
     * Broadcast message to all connected clients
     */
    public void broadcastMessage(Message message) {
        System.out.println("[ChatServer] Broadcasting message - Type: " + message.getType() + 
                         ", Sender: " + message.getSender() + 
                         ", Content: " + message.getContent());
        
        // Add to chat history (Member 4's thread-safe operation)
        if (message.getType() == Message.MessageType.CHAT) {
            userManager.addToChatHistory(message);
            
            // Track message for delivery and seen status
            messageTracker.trackMessage(message);
        }
        
        // Send to all clients and mark as delivered
        int handlerCount = 0;
        for (ClientHandler handler : userManager.getAllHandlers()) {
            handlerCount++;
            System.out.println("[ChatServer] Sending to user: " + handler.getUsername() + " (handler #" + handlerCount + ")");
            handler.sendMessage(message);
            
            // Mark as delivered to each user (excluding sender)
            if (message.getType() == Message.MessageType.CHAT && 
                !handler.getUsername().equals(message.getSender())) {
                messageTracker.markDelivered(message.getMessageId(), handler.getUsername());
            }
        }
        
        System.out.println("[ChatServer] Broadcast complete - Sent to " + handlerCount + " handlers");
    }
    
    /**
     * Broadcast typing status to all other clients (excluding sender)
     */
    public void broadcastTypingStatus(Message message, ClientHandler sender) {
        // Send typing status to all clients except the sender
        for (ClientHandler handler : userManager.getAllHandlers()) {
            if (handler != sender) {
                handler.sendMessage(message);
            }
        }
        
        System.out.println("[ChatServer] Typing status: " + message.getSender() + " - " + message.getType());
    }

    /**
     * Send private message to specific user
     */
    public void sendPrivateMessage(Message message) {
        String receiver = message.getReceiver();
        ClientHandler receiverHandler = userManager.getUserHandler(receiver);
        
        if (receiverHandler != null) {
            receiverHandler.sendMessage(message);
            
            // Send confirmation to sender
            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message confirmMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "Private message sent to " + receiver);
                senderHandler.sendMessage(confirmMsg);
            }
        } else {
            // User not found
            ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
            if (senderHandler != null) {
                Message errorMsg = new Message(Message.MessageType.SYSTEM, "Server",
                    "User '" + receiver + "' not found");
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
     * Mark all messages from sender as seen by viewer
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
        // Process in separate thread to avoid blocking
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
        
        // Shutdown connection listener
        if (connectionListener != null) {
            connectionListener.shutdown();
        }
        
        // Shutdown NIO broadcaster if running
        /*
        if (nioBroadcaster != null) {
            nioBroadcaster.shutdown();
        }
        */
        
        // Shutdown client executor
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
        System.out.println("=".repeat(60));
        System.out.println("SERVER STATISTICS");
        System.out.println("=".repeat(60));
        System.out.println("Active Users: " + userManager.getUserCount());
        System.out.println("Total Messages: " + userManager.getChatHistory().size());
        System.out.println("Online Users: " + String.join(", ", userManager.getActiveUsernames()));
        System.out.println("Active Quizzes: " + quizManager.getActiveQuizzesCount());
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Get the QuizManager instance
     */
    public QuizManager getQuizManager() {
        return quizManager;
    }

    // ============== Quiz Handling Methods ==============

    /**
     * Handle generic quiz messages
     */
    public void handleQuizMessage(Message message, ClientHandler sender) {
        // This can be expanded to handle different quiz message types
        System.out.println("[ChatServer] Quiz message received: " + message.getType());
        // For now, just broadcast quiz messages to all users
        broadcastMessage(message);
    }

    /**
     * Handle create quiz command
     */
    public void handleCreateQuiz(String quizName, String adminUsername, ClientHandler sender) {
        try {
            Quiz quiz = quizManager.createQuiz(quizName, adminUsername);
            
            Message response = new Message(Message.MessageType.SYSTEM, "Server",
                "Quiz '" + quizName + "' created successfully! Quiz ID: " + quiz.getQuizId() + 
                "\nUse /addquestion to add questions to your quiz.");
            sender.sendMessage(response);
            
            System.out.println("[ChatServer] Quiz created: " + quiz.getQuizId() + " by " + adminUsername);
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to create quiz: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle add question to quiz command
     */
    public void handleAddQuestion(String quizId, String questionText, String[] options, 
                                 int correctIndex, int timeLimit, String adminUsername, ClientHandler sender) {
        try {
            boolean success = quizManager.addQuestion(quizId, questionText, options, correctIndex, timeLimit, adminUsername);
            
            Message response;
            if (success) {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Question added to quiz " + quizId + " successfully!");
            } else {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Failed to add question. Check quiz ID and ensure you're the admin.");
            }
            sender.sendMessage(response);
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to add question: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle invite users to quiz command
     */
    public void handleInviteToQuiz(String quizId, String[] usernames, String adminUsername, ClientHandler sender) {
        try {
            Quiz quiz = quizManager.getQuiz(quizId);
            if (quiz == null) {
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Quiz not found: " + quizId));
                return;
            }
            
            if (!quiz.getAdminUsername().equals(adminUsername)) {
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Only the quiz admin can invite users."));
                return;
            }
            
            int invitedCount = 0;
            
            // Check if inviting "all" users
            if (usernames.length == 1 && usernames[0].trim().equalsIgnoreCase("all")) {
                // Get all online users except the admin
                List<String> allUsers = userManager.getActiveUsernames();
                for (String user : allUsers) {
                    if (!user.equals(adminUsername)) {  // Don't invite the admin
                        ClientHandler userHandler = userManager.getUserHandler(user);
                        if (userHandler != null) {
                            Message invitation = new Message(Message.MessageType.QUIZ_INVITATION, adminUsername,
                                "You've been invited to join quiz '" + quiz.getQuizName() + "' (ID: " + quizId + ")\n" +
                                "Type /joinquiz " + quizId + " to participate!");
                            userHandler.sendMessage(invitation);
                            invitedCount++;
                        }
                    }
                }
            } else {
                // Invite specific users
                for (String username : usernames) {
                    username = username.trim();
                    if (!username.isEmpty()) {
                        ClientHandler userHandler = userManager.getUserHandler(username);
                        if (userHandler != null) {
                            Message invitation = new Message(Message.MessageType.QUIZ_INVITATION, adminUsername,
                                "You've been invited to join quiz '" + quiz.getQuizName() + "' (ID: " + quizId + ")\n" +
                                "Type /joinquiz " + quizId + " to participate!");
                            userHandler.sendMessage(invitation);
                            invitedCount++;
                        } else {
                            System.out.println("[ChatServer] User not found for quiz invitation: " + username);
                        }
                    }
                }
            }
            
            String invitationMessage = invitedCount > 0 
                ? "Sent invitations to " + invitedCount + " user(s) for quiz '" + quiz.getQuizName() + "'"
                : "No users found to invite. Make sure users are online.";
            sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server", invitationMessage));
                
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to send invitations: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle start quiz command
     */
    public void handleStartQuiz(String quizId, String adminUsername, ClientHandler sender) {
        try {
            boolean success = quizManager.startQuiz(quizId, adminUsername);
            
            Message response;
            if (success) {
                Quiz quiz = quizManager.getQuiz(quizId);
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Quiz '" + quiz.getQuizName() + "' started successfully!");
                
                // Notify all participants
                broadcastQuizStart(quiz);
            } else {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Failed to start quiz. Check quiz ID, ensure you're the admin, and that the quiz has questions.");
            }
            sender.sendMessage(response);
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to start quiz: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle delete quiz command
     */
    public void handleDeleteQuiz(String quizId, String username, ClientHandler sender) {
        try {
            Quiz quiz = quizManager.getQuiz(quizId);
            
            if (quiz == null) {
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Quiz not found: " + quizId));
                return;
            }
            
            // Check if user is the admin
            if (!quiz.getAdminUsername().equals(username)) {
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Only the quiz admin can delete this quiz."));
                return;
            }
            
            String quizName = quiz.getQuizName();
            
            // Delete the quiz
            boolean success = quizManager.deleteQuiz(quizId);
            
            if (success) {
                // Notify admin
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Quiz '" + quizName + "' (ID: " + quizId + ") has been deleted permanently."));
                
                // Broadcast deletion to all connected users
                Message deleteNotification = new Message(Message.MessageType.QUIZ_DELETED, "Server",
                    "Quiz '" + quizName + "' (ID: " + quizId + ") has been deleted by the admin.");
                broadcastMessage(deleteNotification);
                
                System.out.println("[ChatServer] Quiz deleted: " + quizId + " by " + username + " - broadcasted to all users");
            } else {
                sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server",
                    "Failed to delete quiz: " + quizId));
            }
            
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to delete quiz: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle join quiz command
     */
    public void handleJoinQuiz(String quizId, String username, ClientHandler sender) {
        try {
            boolean success = quizManager.joinQuiz(quizId, username);
            
            Message response;
            if (success) {
                Quiz quiz = quizManager.getQuiz(quizId);
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Successfully joined quiz '" + quiz.getQuizName() + "'!");
            } else {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Failed to join quiz. Quiz may not exist or may have already started.");
            }
            sender.sendMessage(response);
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to join quiz: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle quiz answer submission
     */
    public void handleQuizAnswer(String quizId, int answerIndex, String username, ClientHandler sender) {
        try {
            long responseTime = System.currentTimeMillis(); // For now, we'll use current time as response time
            boolean success = quizManager.submitAnswer(quizId, username, answerIndex, responseTime);
            
            Message response;
            if (success) {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Answer submitted for quiz " + quizId);
            } else {
                response = new Message(Message.MessageType.SYSTEM, "Server",
                    "Failed to submit answer. Check quiz ID and ensure the quiz is active.");
            }
            sender.sendMessage(response);
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to submit answer: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Handle list quizzes command
     */
    public void handleListQuizzes(String username, ClientHandler sender) {
        try {
            List<Quiz> activeQuizzes = quizManager.getActiveQuizzes();
            
            StringBuilder response = new StringBuilder("Active Quizzes:\n");
            if (activeQuizzes.isEmpty()) {
                response.append("No active quizzes available.");
            } else {
                for (Quiz quiz : activeQuizzes) {
                    response.append("- ").append(quiz.getQuizName())
                           .append(" (ID: ").append(quiz.getQuizId()).append(")")
                           .append(" - Admin: ").append(quiz.getAdminUsername())
                           .append(" - State: ").append(quiz.getState())
                           .append(" - Questions: ").append(quiz.getQuestions().size())
                           .append("\n");
                }
            }
            
            sender.sendMessage(new Message(Message.MessageType.SYSTEM, "Server", response.toString()));
        } catch (Exception e) {
            Message error = new Message(Message.MessageType.SYSTEM, "Server",
                "Failed to list quizzes: " + e.getMessage());
            sender.sendMessage(error);
        }
    }

    /**
     * Broadcast quiz start to all participants
     */
    private void broadcastQuizStart(Quiz quiz) {
        Message startMessage = new Message(Message.MessageType.QUIZ_START, "Server",
            "Quiz '" + quiz.getQuizName() + "' has started!");
        
        // Send to all participants
        for (String participantName : quiz.getParticipants().keySet()) {
            ClientHandler handler = userManager.getUserHandler(participantName);
            if (handler != null) {
                handler.sendMessage(startMessage);
            }
        }
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        
        // Start server
        server.start();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
        
        // Keep server running and print stats periodically
        try {
            while (server.running) {
                Thread.sleep(30000); // Print stats every 30 seconds
                server.printStatistics();
            }
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        }
    }
}
