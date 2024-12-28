package com.ur91k.clichat.profile;

import org.joml.Vector4f;

public class UserProfile {
    private String username;
    private String password;  // Will be stored as a hash
    private Vector4f color;
    private long lastUsed;    // Timestamp for sorting profiles
    
    public UserProfile(String username, String password, Vector4f color) {
        this.username = username;
        this.password = password;
        this.color = new Vector4f(color);
        this.lastUsed = System.currentTimeMillis();
    }
    
    // Required for GSON deserialization
    public UserProfile() {
        this.color = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
        this.lastUsed = System.currentTimeMillis();
    }
    
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public Vector4f getColor() { return new Vector4f(color); }
    public long getLastUsed() { return lastUsed; }
    
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setColor(Vector4f color) { this.color = new Vector4f(color); }
    public void updateLastUsed() { this.lastUsed = System.currentTimeMillis(); }
    
    @Override
    public String toString() {
        return String.format("%s (Last used: %s)", username, 
            new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(lastUsed)));
    }
} 