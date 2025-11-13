package com.chatapp.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Common message protocol for client-server communication
 * Used across all components
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // Message Types
    public enum MessageType {
        CONNECT,
        CONNECT_ACK,
        CHAT,
        PRIVATE_MSG,
        SYSTEM,
        USER_LIST,
        API_REQUEST,
        API_RESPONSE,
        LOGIN,
        REGISTER,
        AUTH_RESPONSE,
        FILE_SEND,
        FILE_RESPONSE,
        DISCONNECT,
        TYPING_START,
        TYPING_STOP,
        MESSAGE_DELIVERED,
        MESSAGE_SEEN
    }

    // Message Status Types
    public enum MessageStatus {
        SENT,       // Message sent to server
        DELIVERED,  // Delivered to receiver(s)
        SEEN        // Seen by receiver(s)
    }

    private MessageType type;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;

    // File-related fields
    private byte[] fileData;
    private String fileName;
    private long fileSize;

    // Message tracking fields
    private String messageId;                 // Unique ID for message
    private MessageStatus status;             // Sent / Delivered / Seen
    private Set<String> deliveredTo;          // Users who received message
    private Set<String> seenBy;               // Users who have seen message

    // Constructors
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.messageId = UUID.randomUUID().toString();
        this.status = MessageStatus.SENT;
        this.deliveredTo = new HashSet<>();
        this.seenBy = new HashSet<>();
    }

    public Message(MessageType type, String sender, String content, String receiver) {
        this(type, sender, content);
        this.receiver = receiver;
    }

    public Message(MessageType type, String sender, String content,
                   byte[] fileData, String fileName, long fileSize, String receiver) {
        this(type, sender, content, receiver);
        this.fileData = fileData;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    // Getters and Setters
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

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Set<String> getDeliveredTo() { return deliveredTo; }
    public void setDeliveredTo(Set<String> deliveredTo) { this.deliveredTo = deliveredTo; }

    public Set<String> getSeenBy() { return seenBy; }
    public void setSeenBy(Set<String> seenBy) { this.seenBy = seenBy; }

    // Helper methods for delivery tracking
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

    public boolean isDeliveredToAll(Set<String> allUsers) {
        return deliveredTo != null && allUsers != null && deliveredTo.containsAll(allUsers);
    }

    public boolean isSeenByAll(Set<String> allUsers) {
        return seenBy != null && allUsers != null && seenBy.containsAll(allUsers);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s [%s]", timestamp, sender, content, status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return Objects.equals(messageId, message.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId);
    }
}
