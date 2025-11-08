package com.chatapp.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Common message protocol for client-server communication
 * Used across all components
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        CONNECT, // legacy
        CONNECT_ACK,
        CHAT,
        PRIVATE_MSG,
        SYSTEM,
        USER_LIST,
        API_REQUEST,
        API_RESPONSE,
        // Auth related
        LOGIN,
        REGISTER,
        AUTH_RESPONSE,
        // File related
        FILE_SEND,
        FILE_RESPONSE,
        DISCONNECT,
        TYPING_START,
        TYPING_STOP
    }
    
    private MessageType type;
    private String sender;
    private String receiver; // for private messages
    private String content;

    // File fields
    private byte[] fileData;
    private String fileName;
    private long fileSize;
    private LocalDateTime timestamp;
    
    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public Message(MessageType type, String sender, String content, String receiver) {
        this(type, sender, content);
        this.receiver = receiver;
    }

    // File constructor
    public Message(MessageType type, String sender, String content, byte[] fileData, String fileName, long fileSize, String receiver) {
        this(type, sender, content);
        this.fileData = fileData;
        this.fileName = fileName;
        this.fileSize = fileSize;
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

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp.toString(), sender, content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;
        return Objects.equals(type, message.type) &&
                Objects.equals(sender, message.sender) &&
                Objects.equals(receiver, message.receiver) &&
                Objects.equals(content, message.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, sender, receiver, content);
    }
}