package com.chatapp.server;

import com.chatapp.common.Message;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * Message Tracker - Manages message delivery and seen status
 * 
 * Tracks message states:
 * - SENT: Message sent to server (single checkmark)
 * - DELIVERED: Message delivered to all online users (double checkmark)
 * - SEEN: Message seen by all users (blue double checkmark)
 */
public class MessageTracker {
    private final ConcurrentHashMap<String, Message> messages = new ConcurrentHashMap<>();
    private final UserManager userManager;
    
    public MessageTracker(UserManager userManager) {
        this.userManager = userManager;
    }
    
    /**
     * Track a new message
     */
    public void trackMessage(Message message) {
        if (message.getMessageId() != null) {
            messages.put(message.getMessageId(), message);
            System.out.println("[MessageTracker] Tracking message: " + message.getMessageId());
        }
    }
    
    /**
     * Mark message as delivered to a specific user
     */
    public void markDelivered(String messageId, String username) {
        Message message = messages.get(messageId);
        if (message != null) {
            message.markDeliveredTo(username);
            
            // Check if delivered to all online users (excluding sender)
            Set<String> onlineUsers = new HashSet<>(userManager.getActiveUsernames());
            onlineUsers.remove(message.getSender()); // Remove sender from delivery check
            
            if (message.isDeliveredToAll(onlineUsers)) {
                message.setStatus(Message.MessageStatus.DELIVERED);
                notifyStatusUpdate(message);
                System.out.println("[MessageTracker] Message " + messageId + " delivered to all users");
            }
        }
    }
    
    /**
     * Mark message as seen by a specific user
     */
    public void markSeen(String messageId, String username) {
        Message message = messages.get(messageId);
        if (message != null) {
            message.markSeenBy(username);
            
            // Check if seen by all users (excluding sender)
            Set<String> allUsers = new HashSet<>(userManager.getActiveUsernames());
            allUsers.remove(message.getSender()); // Remove sender from seen check
            
            if (message.isSeenByAll(allUsers)) {
                message.setStatus(Message.MessageStatus.SEEN);
                notifyStatusUpdate(message);
                System.out.println("[MessageTracker] Message " + messageId + " seen by all users");
            }
        }
    }
    
    /**
     * Mark all messages from a specific sender as seen by a user
     */
    public void markAllMessagesSeen(String viewerUsername, String senderUsername) {
        for (Message message : messages.values()) {
            if (message.getSender().equals(senderUsername) && 
                !message.getSeenBy().contains(viewerUsername)) {
                markSeen(message.getMessageId(), viewerUsername);
            }
        }
    }
    
    /**
     * Notify sender about status update
     */
    private void notifyStatusUpdate(Message message) {
        // Create status update message
        Message statusUpdate = new Message(Message.MessageType.MESSAGE_SEEN, 
            "Server", 
            String.format("{\"messageId\":\"%s\",\"status\":\"%s\"}", 
                message.getMessageId(), message.getStatus()));
        
        // Send to original sender
        ClientHandler senderHandler = userManager.getUserHandler(message.getSender());
        if (senderHandler != null) {
            senderHandler.sendMessage(statusUpdate);
        }
    }
    
    /**
     * Get message status
     */
    public Message.MessageStatus getMessageStatus(String messageId) {
        Message message = messages.get(messageId);
        return message != null ? message.getStatus() : null;
    }
    
    /**
     * Clean up old messages (optional - to prevent memory leaks)
     */
    public void cleanupOldMessages() {
        // Remove messages older than 24 hours
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        messages.entrySet().removeIf(entry -> {
            Message message = entry.getValue();
            return message.getTimestamp().atZone(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli() < cutoffTime;
        });
    }
}