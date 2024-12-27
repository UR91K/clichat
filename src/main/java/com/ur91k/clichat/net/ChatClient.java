package com.ur91k.clichat.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ur91k.clichat.util.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.joml.Vector4f;

import java.net.URI;
import java.util.function.Consumer;

public class ChatClient extends WebSocketClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class);
    private final Gson gson;
    private final Consumer<Message> onMessageReceived;
    private final Consumer<String> onConnectionStatusChanged;
    
    private String userId;
    private String username;
    private Vector4f userColor;
    private String roomName;
    private Vector4f roomColor;
    private boolean isConnected = false;
    
    public ChatClient(URI serverUri, String username, Vector4f userColor,
            Consumer<Message> onMessageReceived,
            Consumer<String> onConnectionStatusChanged) {
        super(serverUri);
        this.username = username;
        this.userColor = userColor;
        this.onMessageReceived = onMessageReceived;
        this.onConnectionStatusChanged = onConnectionStatusChanged;
        
        // Configure Gson with our custom adapters
        this.gson = new GsonBuilder()
            .registerTypeAdapter(Vector4f.class, new Vector4fAdapter())
            .registerTypeAdapter(Message.class, new MessageAdapter())
            .create();
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        isConnected = true;
        userId = this.getLocalSocketAddress().toString();
        
        // Send join message
        send(gson.toJson(Message.join(userId, username, userColor)));
        
        onConnectionStatusChanged.accept("CONNECTED");
        logger.info("Connected to server");
    }
    
    @Override
    public void onMessage(String messageJson) {
        try {
            Message message = gson.fromJson(messageJson, Message.class);
            
            // Handle room updates
            if (message.getType() == Message.Type.ROOM_UPDATE) {
                String[] parts = message.getNewValue().split(";");
                roomName = parts[0];
                // Parse the Vector4f from string
                String vectorStr = parts[1].substring(1, parts[1].length() - 1); // Remove []
                String[] components = vectorStr.split(",");
                roomColor = new Vector4f(
                    Float.parseFloat(components[0]),
                    Float.parseFloat(components[1]),
                    Float.parseFloat(components[2]),
                    Float.parseFloat(components[3])
                );
            }
            
            onMessageReceived.accept(message);
            logger.debug("Received message: {}", message.getContent());
        } catch (Exception e) {
            logger.error("Error handling message: {}", e.getMessage());
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        isConnected = false;
        onConnectionStatusChanged.accept("DISCONNECTED");
        logger.info("Disconnected from server: {}", reason);
    }
    
    @Override
    public void onError(Exception ex) {
        logger.error("WebSocket error: {}", ex.getMessage());
    }
    
    public void sendMessage(String content) {
        if (!isConnected) {
            logger.warn("Cannot send message: not connected");
            return;
        }
        Message message = Message.chat(userId, username, userColor, content);
        send(gson.toJson(message));
    }
    
    public void changeNickname(String newNickname) {
        if (!isConnected) return;
        username = newNickname;
        Message message = Message.nickChange(userId, username, newNickname, userColor);
        send(gson.toJson(message));
    }
    
    public void changeColor(Vector4f newColor) {
        if (!isConnected) return;
        Vector4f oldColor = new Vector4f(userColor);
        userColor = newColor;
        Message message = Message.colorChange(userId, username, oldColor, newColor);
        send(gson.toJson(message));
    }
    
    public void sendCommand(String command) {
        if (!isConnected) return;
        Message message = new Message(Message.Type.COMMAND, userId, username, userColor, command);
        send(gson.toJson(message));
    }
    
    // Getters
    public boolean isConnected() { return isConnected; }
    public String getUsername() { return username; }
    public Vector4f getUserColor() { return new Vector4f(userColor); }
    public String getRoomName() { return roomName; }
    public Vector4f getRoomColor() { return roomColor != null ? new Vector4f(roomColor) : null; }
    public String getServerAddress() { 
        return getRemoteSocketAddress().toString().replaceFirst("^ws://", "");
    }
} 