package com.ur91k.clichat.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ur91k.clichat.net.MessageAdapter;
import org.joml.Vector4f;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class UserProfileManager {
    private static final String PROFILES_FILE = "profiles.json";
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(Vector4f.class, new MessageAdapter.Vector4fAdapter())
        .create();
    
    private Map<String, UserProfile> profiles;
    private UserProfile currentProfile;
    
    public UserProfileManager() {
        this.profiles = new HashMap<>();
        load();
    }
    
    public void load() {
        try {
            if (Files.exists(Paths.get(PROFILES_FILE))) {
                String json = Files.readString(Paths.get(PROFILES_FILE));
                TypeToken<Map<String, UserProfile>> typeToken = new TypeToken<>() {};
                profiles = gson.fromJson(json, typeToken.getType());
            }
        } catch (IOException e) {
            System.err.println("Error loading profiles: " + e.getMessage());
        }
    }
    
    public void save() {
        try {
            String json = gson.toJson(profiles);
            Files.writeString(Paths.get(PROFILES_FILE), json);
        } catch (IOException e) {
            System.err.println("Error saving profiles: " + e.getMessage());
        }
    }
    
    public List<UserProfile> getProfiles() {
        return profiles.values().stream()
            .sorted((a, b) -> Long.compare(b.getLastUsed(), a.getLastUsed()))
            .toList();
    }
    
    public UserProfile getCurrentProfile() {
        return currentProfile;
    }
    
    public boolean createProfile(String username, String password, Vector4f color) {
        if (profiles.containsKey(username.toLowerCase())) {
            return false;
        }
        
        UserProfile profile = new UserProfile(username, hashPassword(password), color);
        profiles.put(username.toLowerCase(), profile);
        save();
        return true;
    }
    
    public boolean login(String username, String password) {
        UserProfile profile = profiles.get(username.toLowerCase());
        if (profile != null && profile.getPassword().equals(hashPassword(password))) {
            currentProfile = profile;
            profile.updateLastUsed();
            save();
            return true;
        }
        return false;
    }
    
    public void updateCurrentProfile() {
        if (currentProfile != null) {
            profiles.put(currentProfile.getUsername().toLowerCase(), currentProfile);
            save();
        }
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }
} 