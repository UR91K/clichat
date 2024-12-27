package com.ur91k.clichat.net;

import org.joml.Vector4f;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a message in the chat system.
 * This can be a chat message, system notification, or command result.
 */
public class Message {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    public enum Type {
        CHAT,           // Normal chat message
        SYSTEM,         // System notification (joins, parts, etc)
        COMMAND,        // Command response
        JOIN,           // User joined
        LEAVE,          // User left
        KICK,           // User was kicked
        BAN,           // User was banned
        NICK_CHANGE,    // User changed nickname
        COLOR_CHANGE,   // User changed color
        ROOM_UPDATE     // Room name or color changed
    }
    
    private final Type type;
    private final String senderId;      // Unique ID of sender
    private final String senderName;    // Display name of sender
    private final Vector4f senderColor; // Color of sender's name
    private final String content;       // Message content
    private LocalDateTime timestamp;    // Changed from final to allow setting in deserialization
    
    // Additional metadata for specific message types
    private String targetId;   // For commands targeting users
    private String oldValue;   // For changes (old nickname, old color)
    private String newValue;   // For changes (new nickname, new color)
    
    public Message(Type type, String senderId, String senderName, Vector4f senderColor, String content) {
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderColor = new Vector4f(senderColor);
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Creates a system message
     */
    public static Message system(String content) {
        return new Message(Type.SYSTEM, "system", "System", 
            new Vector4f(0.8f, 0.8f, 0.8f, 1.0f), content);
    }
    
    /**
     * Creates a chat message
     */
    public static Message chat(String senderId, String senderName, Vector4f senderColor, String content) {
        return new Message(Type.CHAT, senderId, senderName, senderColor, content);
    }
    
    /**
     * Creates a join notification
     */
    public static Message join(String senderId, String senderName, Vector4f senderColor) {
        return new Message(Type.JOIN, senderId, senderName, senderColor, 
            senderName + " joined the chat");
    }
    
    /**
     * Creates a leave notification
     */
    public static Message leave(String senderId, String senderName, Vector4f senderColor) {
        return new Message(Type.LEAVE, senderId, senderName, senderColor,
            senderName + " left the chat");
    }
    
    /**
     * Creates a nickname change notification
     */
    public static Message nickChange(String senderId, String oldName, String newName, Vector4f senderColor) {
        Message msg = new Message(Type.NICK_CHANGE, senderId, newName, senderColor,
            oldName + " is now known as " + newName);
        msg.oldValue = oldName;
        msg.newValue = newName;
        return msg;
    }
    
    /**
     * Creates a color change notification
     */
    public static Message colorChange(String senderId, String senderName, Vector4f oldColor, Vector4f newColor) {
        Message msg = new Message(Type.COLOR_CHANGE, senderId, senderName, newColor,
            senderName + " changed their color");
        msg.oldValue = oldColor.toString();
        msg.newValue = newColor.toString();
        return msg;
    }
    
    /**
     * Creates a room update notification
     */
    public static Message roomUpdate(String roomName, Vector4f roomColor) {
        Message msg = new Message(Type.ROOM_UPDATE, "system", "System",
            new Vector4f(0.8f, 0.8f, 0.8f, 1.0f),
            "Room updated: " + roomName);
        msg.oldValue = "";  // TODO: Store old room info
        msg.newValue = roomName + ";" + roomColor.toString();
        return msg;
    }
    
    /**
     * Formats the message for display in the terminal
     */
    public String format() {
        String time = timestamp.format(TIME_FORMAT);
        
        switch (type) {
            case CHAT:
                return String.format("[%s] %s: %s", time, senderName, content);
            case SYSTEM:
            case JOIN:
            case LEAVE:
            case NICK_CHANGE:
            case COLOR_CHANGE:
            case ROOM_UPDATE:
                return String.format("[%s] * %s", time, content);
            case COMMAND:
                return String.format("[%s] > %s", time, content);
            case KICK:
            case BAN:
                return String.format("[%s] ! %s", time, content);
            default:
                return String.format("[%s] %s", time, content);
        }
    }
    
    // Getters
    public Type getType() { return type; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public Vector4f getSenderColor() { return new Vector4f(senderColor); }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getTargetId() { return targetId; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    
    // Setters for additional metadata
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
} 