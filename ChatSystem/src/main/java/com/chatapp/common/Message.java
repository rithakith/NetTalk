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
        CONNECT, DISCONNECT, CHAT, PRIVATE_MSG, USER_LIST, API_REQUEST, API_RESPONSE, SYSTEM
    }
    
    private MessageType type;
    private String sender;
    private String receiver; // for private messages
    private String content;
    private LocalDateTime timestamp;
    
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
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
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp.toString(), sender, content);
    }
}
