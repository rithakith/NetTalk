package com.chatapp.common;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Common message protocol for client-server communication
 * Used across all components
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CONNECT, DISCONNECT, CHAT, PRIVATE_MSG, USER_LIST, API_REQUEST, API_RESPONSE, SYSTEM, TYPING_START, TYPING_STOP, MESSAGE_DELIVERED, MESSAGE_SEEN,
        // Quiz message types
        QUIZ_CREATED, QUIZ_INVITATION, QUIZ_JOIN, QUIZ_START, QUIZ_QUESTION, QUIZ_ANSWER, QUIZ_RESULTS, QUIZ_ENDED, QUIZ_DELETED
    }
    
    public enum MessageStatus {
        SENT,           // Single checkmark: sent to server
        DELIVERED,      // Double checkmark: delivered to all online users
        SEEN            // Blue double checkmark: seen by all users
    }
    
    private MessageType type;
    private String sender;
    private String receiver; // for private messages
    private String content;
    private LocalDateTime timestamp;
    
    // Message tracking fields
    private String messageId;           // Unique identifier for each message
    private MessageStatus status;       // Current status of the message
    private java.util.Set<String> deliveredTo;  // Users who received the message
    private java.util.Set<String> seenBy;       // Users who have seen the message
    
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.messageId = java.util.UUID.randomUUID().toString();
        this.status = MessageStatus.SENT;
        this.deliveredTo = new java.util.HashSet<>();
        this.seenBy = new java.util.HashSet<>();
    }
    
    public Message(MessageType type, String sender, String receiver, String content) {
        this(type, sender, content);
        this.receiver = receiver;
    }
    
    // Getters and setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    // Message tracking getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    
    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }
    
    public java.util.Set<String> getDeliveredTo() { return deliveredTo; }
    public void setDeliveredTo(java.util.Set<String> deliveredTo) { this.deliveredTo = deliveredTo; }
    
    public java.util.Set<String> getSeenBy() { return seenBy; }
    public void setSeenBy(java.util.Set<String> seenBy) { this.seenBy = seenBy; }
    
    // Helper methods
    public void markDeliveredTo(String username) {
        if (deliveredTo != null) {
            deliveredTo.add(username);
        }
    }
    
    public void markSeenBy(String username) {
        if (seenBy != null) {
            seenBy.add(username);
        }
    }
    
    public boolean isDeliveredToAll(java.util.Set<String> allUsers) {
        if (deliveredTo == null || allUsers == null) return false;
        return deliveredTo.containsAll(allUsers);
    }
    
    public boolean isSeenByAll(java.util.Set<String> allUsers) {
        if (seenBy == null || allUsers == null) return false;
        return seenBy.containsAll(allUsers);
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s [%s]", timestamp.toString(), sender, content, status);
    }
}
